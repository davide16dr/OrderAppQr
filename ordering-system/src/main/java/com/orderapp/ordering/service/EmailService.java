package com.orderapp.ordering.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    @Value("${app.mail.from:no-reply@orderapp.local}")
    private String from;

    @Value("${app.mail.provider:smtp}")
    private String provider;

    @Value("${app.mail.resend.api-key:}")
    private String resendApiKey;

    @Value("${app.mail.resend.endpoint:https://api.resend.com/emails}")
    private String resendEndpoint;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.port:0}")
    private int smtpPort;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    @Value("${app.mail.log-temporary-password-on-failure:false}")
    private boolean logTemporaryPasswordOnFailure;

    @PostConstruct
    void logMailConfiguration() {
        log.info("Mail configuration loaded: provider={}, smtpHost={}, smtpPort={}, username={}, from={}, resendConfigured={}, passwordConfigured={}",
                normalizeProvider(),
                (smtpHost == null || smtpHost.isBlank()) ? "<not-set>" : smtpHost,
                smtpPort,
                (smtpUsername == null || smtpUsername.isBlank()) ? "<not-set>" : smtpUsername,
                (from == null || from.isBlank()) ? "<not-set>" : from,
                resendApiKey != null && !resendApiKey.isBlank(),
                smtpPassword != null && !smtpPassword.isBlank());
    }

    public boolean sendTemporaryPasswordEmail(String to, String tenantName, String temporaryPassword, String logoDataUrl) {
        String subject = "OrderApp - Password temporanea";
        String normalizedProvider = normalizeProvider();

        if ("resend".equals(normalizedProvider)) {
            if (resendApiKey == null || resendApiKey.isBlank()) {
                log.warn("Transactional provider Resend requested but RESEND_API_KEY is missing; email not sent to {}", to);
                return false;
            }

            try {
                String htmlBody = buildEmailHtml(tenantName, temporaryPassword, logoDataUrl, false);
                return sendViaResend(to, subject, htmlBody, temporaryPassword);
            } catch (Exception ex) {
                log.error("Impossibile inviare email password temporanea via Resend a {} (from={})", to, from, ex);
                if (logTemporaryPasswordOnFailure) {
                    log.warn("[DEV] Password temporanea per {}: {}", to, temporaryPassword);
                }
                return false;
            }
        }

        String htmlBody = buildEmailHtml(tenantName, temporaryPassword, logoDataUrl, true);

        // SMTP fallback for local/dev or legacy deployments.
        if ((smtpHost == null || smtpHost.isBlank()) || (smtpUsername == null || smtpUsername.isBlank()) || (smtpPassword == null || smtpPassword.isBlank())) {
            if (logTemporaryPasswordOnFailure) {
                log.warn("[DEV] Password temporanea per {}: {}", to, temporaryPassword);
                log.warn("[DEV] SMTP non configurato correttamente. Configura le variabili d'ambiente: SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD");
            } else {
                log.info("SMTP non configurato; saltata invio email temporanea a {}", to);
            }
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            if (logoDataUrl != null && !logoDataUrl.isBlank()) {
                ParsedDataUrl parsed = parseDataUrl(logoDataUrl);
                if (parsed != null) {
                    helper.addInline("companyLogo", new ByteArrayResource(parsed.bytes), parsed.contentType);
                }
            }

            mailSender.send(message);
            log.info("Email password temporanea inviata a {} (SMTP {}:{}, from={})",
                    to,
                    (smtpHost == null || smtpHost.isBlank()) ? "<non-configurato>" : smtpHost,
                    smtpPort,
                    from);
            return true;
        } catch (Exception ex) {
            log.error(
                    "Impossibile inviare email password temporanea a {} (SMTP {}:{}, from={})",
                    to,
                    (smtpHost == null || smtpHost.isBlank()) ? "<non-configurato>" : smtpHost,
                    smtpPort,
                    from,
                    ex);

            if (logTemporaryPasswordOnFailure) {
                log.warn("[DEV] Password temporanea per {}: {}", to, temporaryPassword);
                log.warn("[DEV] Configura le variabili d'ambiente: SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD");
            }
            return false;
        }
    }

    private boolean sendViaResend(String to, String subject, String htmlBody, String temporaryPassword) throws IOException, InterruptedException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("from", from);
        payload.put("subject", subject);
        payload.put("html", htmlBody);
        payload.putArray("to").add(to);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(resendEndpoint))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            log.info("Email password temporanea inviata a {} via Resend (from={})", to, from);
            return true;
        }

        log.error("Resend API returned status {} for {}: {}", response.statusCode(), to, abbreviate(response.body()));
        if (logTemporaryPasswordOnFailure) {
            log.warn("[DEV] Password temporanea per {}: {}", to, temporaryPassword);
        }
        return false;
    }

    private String buildEmailHtml(String tenantName, String temporaryPassword, String logoDataUrl, boolean useCidLogo) {
        String safeTenantName = tenantName == null ? "" : escapeHtml(tenantName);
        String safePassword = escapeHtml(temporaryPassword);
        String logoBlock = (logoDataUrl != null && !logoDataUrl.isBlank())
                ? useCidLogo
                    ? "<div style='margin-bottom:20px;'><img src='cid:companyLogo' alt='Logo aziendale' style='max-width:140px;max-height:80px;object-fit:contain;display:block;'/></div>"
                    : "<div style='margin-bottom:20px;'><img src='" + escapeHtml(logoDataUrl) + "' alt='Logo aziendale' style='max-width:140px;max-height:80px;object-fit:contain;display:block;'/></div>"
                : "";

        return "<!doctype html>"
                + "<html><body style='font-family:Arial,sans-serif;color:#111827;line-height:1.5;background:#f9fafb;padding:24px;'>"
                + "<div style='max-width:600px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:16px;padding:24px;'>"
                + logoBlock
                + "<h2 style='margin:0 0 12px;font-size:22px;color:#111827;'>Password temporanea OrderApp</h2>"
                + "<p style='margin:0 0 12px;'>La registrazione per <strong>" + safeTenantName + "</strong> è stata ricevuta.</p>"
                + "<p style='margin:0 0 12px;'>Ecco la tua password temporanea per accedere:</p>"
                + "<div style='margin:20px 0;padding:14px 16px;background:#f3f4f6;border-radius:12px;font-size:18px;font-weight:700;letter-spacing:1px;'>"
                + safePassword
                + "</div>"
                + "<p style='margin:0;'>Ti consigliamo di cambiarla subito nelle Impostazioni dopo il primo accesso.</p>"
                + "<p style='margin:20px 0 0;color:#6b7280;font-size:13px;'>Grazie,<br>OrderApp</p>"
                + "</div></body></html>";
    }

    private ParsedDataUrl parseDataUrl(String dataUrl) {
        try {
            String trimmed = dataUrl.trim();
            if (!trimmed.startsWith("data:")) {
                return null;
            }

            int commaIndex = trimmed.indexOf(',');
            if (commaIndex < 0) {
                return null;
            }

            String meta = trimmed.substring(5, commaIndex);
            String payload = trimmed.substring(commaIndex + 1);
            boolean base64 = meta.contains(";base64");
            String contentType = meta.split(";")[0];
            if (contentType == null || contentType.isBlank()) {
                contentType = "image/png";
            }

            byte[] bytes = base64 ? Base64.getDecoder().decode(payload) : payload.getBytes(StandardCharsets.UTF_8);
            return new ParsedDataUrl(contentType, bytes);
        } catch (Exception ex) {
            return null;
        }
    }

    private String escapeHtml(String input) {
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String normalizeProvider() {
        String configured = provider == null ? "" : provider.trim().toLowerCase();
        if (!configured.isBlank()) {
            return configured;
        }

        return (resendApiKey != null && !resendApiKey.isBlank()) ? "resend" : "smtp";
    }

    private String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 500 ? text : text.substring(0, 500) + "...";
    }

    private record ParsedDataUrl(String contentType, byte[] bytes) {}
}
