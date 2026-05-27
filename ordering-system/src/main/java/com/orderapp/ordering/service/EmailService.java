package com.orderapp.ordering.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.annotation.PostConstruct;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@orderapp.local}")
    private String from;

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
        log.info("Mail configuration loaded: host={}, port={}, username={}, from={}, passwordConfigured={}",
                (smtpHost == null || smtpHost.isBlank()) ? "<not-set>" : smtpHost,
                smtpPort,
                (smtpUsername == null || smtpUsername.isBlank()) ? "<not-set>" : smtpUsername,
                (from == null || from.isBlank()) ? "<not-set>" : from,
                smtpPassword != null && !smtpPassword.isBlank());
    }

    public boolean sendTemporaryPasswordEmail(String to, String tenantName, String temporaryPassword, String logoDataUrl) {
        String subject = "OrderApp - Password temporanea";
        String htmlBody = buildEmailHtml(tenantName, temporaryPassword, logoDataUrl);

        // If SMTP is not configured (username/password/host missing), skip sending and log in dev.
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
            // Non blocchiamo la registrazione se SMTP non è configurato correttamente.
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

    private String buildEmailHtml(String tenantName, String temporaryPassword, String logoDataUrl) {
        String safeTenantName = tenantName == null ? "" : escapeHtml(tenantName);
        String safePassword = escapeHtml(temporaryPassword);
        String logoBlock = (logoDataUrl != null && !logoDataUrl.isBlank())
                ? "<div style='margin-bottom:20px;'><img src='cid:companyLogo' alt='Logo aziendale' style='max-width:140px;max-height:80px;object-fit:contain;display:block;'/></div>"
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

    private record ParsedDataUrl(String contentType, byte[] bytes) {}
}
