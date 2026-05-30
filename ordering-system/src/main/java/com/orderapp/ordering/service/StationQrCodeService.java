package com.orderapp.ordering.service;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.SecureRandom;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.orderapp.ordering.dto.ResolveQrResponse;
import com.orderapp.ordering.dto.StationQrResponse;
import com.orderapp.ordering.entity.StationEntity;
import com.orderapp.ordering.entity.StationQrCodeEntity;
import com.orderapp.ordering.entity.StationType;
import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.exception.BusinessException;
import com.orderapp.ordering.exception.ResourceNotFoundException;
import com.orderapp.ordering.exception.UnauthorizedTenantAccessException;
import com.orderapp.ordering.repository.StationQrCodeRepository;
import com.orderapp.ordering.repository.StationRepository;
import com.orderapp.ordering.repository.TenantRepository;

import lombok.RequiredArgsConstructor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class StationQrCodeService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StationRepository stationRepository;
    private final StationQrCodeRepository stationQrCodeRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.customer-base-url:http://localhost:4200/customer/menu}")
    private String customerBaseUrl;

    public StationQrResponse generateQrForStation(Long tenantId, Long stationId, boolean regenerate) {
        StationEntity station = loadTenantStation(tenantId, stationId);

        List<StationQrCodeEntity> existing = stationQrCodeRepository.findByStationIdOrderByGeneratedAtDesc(stationId);
        StationQrCodeEntity active = existing.stream().filter(StationQrCodeEntity::isActive).findFirst().orElse(null);

        if (active != null && !regenerate) {
            return toResponse(active);
        }

        if (active != null) {
            active.setActive(false);
            active.setRegeneratedAt(OffsetDateTime.now());
            active.setUpdatedAt(OffsetDateTime.now());
            stationQrCodeRepository.save(active);
        }

        OffsetDateTime now = OffsetDateTime.now();
        String code = generateSecureCode();
        String qrValue = buildQrValue(station, code);

        StationQrCodeEntity created = StationQrCodeEntity.builder()
                .tenantId(tenantId)
                .station(station)
                .code(code)
                .qrValue(qrValue)
                .imagePath(null)
                .status("ACTIVE")
                .primaryCode(true)
                .rotatable(true)
                .generatedAt(now)
                .regeneratedAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toResponse(stationQrCodeRepository.save(created));
    }

    public StationQrResponse regenerateQrForStation(Long tenantId, Long stationId) {
        return generateQrForStation(tenantId, stationId, true);
    }

    public StationQrResponse getActiveQrForStation(Long tenantId, Long stationId) {
        loadTenantStation(tenantId, stationId);
        StationQrCodeEntity qr = stationQrCodeRepository.findActiveByStationId(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("QR non trovato"));
        return toResponse(qr);
    }

    public ResolveQrResponse resolveStationByQrCode(String code) {
        StationQrCodeEntity qr = stationQrCodeRepository.findByCodeIgnoreCase(code)
                .filter(StationQrCodeEntity::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("QR non valido o non attivo"));

        StationEntity station = qr.getStation();
        if (station == null) {
            throw new ResourceNotFoundException("Postazione non trovata");
        }

        Tenant tenant = tenantRepository.findById(qr.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant non trovato"));

        boolean tenantActive = "ACTIVE".equalsIgnoreCase(tenant.getStatus());
        boolean stationActive = station.isActive();
        boolean orderingEnabled = tenantActive
                && stationActive
                && qr.isActive()
                && isOrderingEnabledByTenantSettings(tenant);

        return new ResolveQrResponse(
                tenant.getId(),
                tenant.getSlug(),
                tenant.getName(),
                station.getId(),
                qr.getCode(),
                station.getName(),
                StationType.fromDatabaseValue(station.getType()).name(),
                station.getArea() != null ? station.getArea().getId() : null,
                station.getArea() != null ? station.getArea().getName() : null,
                tenantActive,
                stationActive,
                qr.isActive(),
                orderingEnabled,
                buildQrMenuUrl(tenant.getSlug(), qr.getCode())
        );
    }

    public byte[] downloadQrPng(Long tenantId, Long stationId) {
        StationQrCodeEntity qr = stationQrCodeRepository.findActiveByStationId(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("QR non trovato"));
        if (!tenantId.equals(qr.getTenantId())) {
            throw new UnauthorizedTenantAccessException("QR non appartiene al tenant autenticato");
        }
        
        StationEntity station = loadTenantStation(tenantId, stationId);
        String stationName = station.getName();
        String areaName = station.getArea() != null ? station.getArea().getName() : "N/A";
        
        return renderQrPngWithInfo(resolveDownloadPayload(qr), stationName, areaName);
    }

    public byte[] downloadAllQrs(Long tenantId) {
        // Fetch all stations for tenant
        List<StationEntity> stations = stationRepository.searchStations(tenantId, "", null, null, null, null);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (StationEntity station : stations) {
                Long stationId = station.getId();
                try {
                    StationQrCodeEntity qr = stationQrCodeRepository.findActiveByStationId(stationId)
                            .orElseGet(() -> {
                                // generate if missing
                                generateQrForStation(tenantId, stationId, false);
                                return stationQrCodeRepository.findActiveByStationId(stationId)
                                        .orElseThrow(() -> new ResourceNotFoundException("QR non trovato dopo la generazione"));
                            });

                    String entryName = String.format("station-%d-%s.png", stationId, station.getName().replaceAll("[^a-zA-Z0-9_-]", "_"));
                    byte[] image = renderQrPngWithInfo(resolveDownloadPayload(qr), station.getName(), station.getArea() != null ? station.getArea().getName() : "");

                    ZipEntry ze = new ZipEntry(entryName);
                    zos.putNextEntry(ze);
                    zos.write(image);
                    zos.closeEntry();
                } catch (Exception ex) {
                    // skip individual failures but continue zipping others
                }
            }

            zos.finish();
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("Impossibile creare l'archivio ZIP dei QR");
        }
    }

    public byte[] downloadQrPngByCode(String code) {
        StationQrCodeEntity qr = stationQrCodeRepository.findByCodeIgnoreCase(code)
                .filter(StationQrCodeEntity::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("QR non trovato"));
        return renderQrPng(resolveDownloadPayload(qr));
    }

    public String downloadQrSvg(Long tenantId, Long stationId) {
        StationQrCodeEntity qr = stationQrCodeRepository.findActiveByStationId(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("QR non trovato"));
        if (!tenantId.equals(qr.getTenantId())) {
            throw new UnauthorizedTenantAccessException("QR non appartiene al tenant autenticato");
        }

        return renderQrSvg(resolveDownloadPayload(qr));
    }

    private String renderQrSvg(String payload) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                    payload,
                    BarcodeFormat.QR_CODE,
                    320,
                    320,
                    java.util.Map.of(EncodeHintType.MARGIN, 1)
            );

            int width = matrix.getWidth();
            int height = matrix.getHeight();
            StringBuilder sb = new StringBuilder();
            sb.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ").append(width).append(" ").append(height).append("' shape-rendering='crispEdges'>");

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (matrix.get(x, y)) {
                        sb.append("<rect x='").append(x).append("' y='").append(y).append("' width='1' height='1' fill='#000'/>");
                    }
                }
            }

            sb.append("</svg>");
            return sb.toString();
        } catch (WriterException ex) {
            throw new BusinessException("Impossibile generare il QR SVG");
        }
    }

    public StationQrCodeEntity loadActiveQrEntity(Long tenantId, Long stationId) {
        StationEntity station = loadTenantStation(tenantId, stationId);
        return stationQrCodeRepository.findActiveByStationId(station.getId())
                .orElseThrow(() -> new ResourceNotFoundException("QR non trovato"));
    }

    public String buildQrMenuUrl(String tenantSlug, String stationCode) {
        String baseUrl = resolveQrBaseUrl();
        return baseUrl + "?tenant=" + tenantSlug + "&token=" + stationCode;
    }

    public String buildQrMenuUrlForStation(StationEntity station, String stationCode) {
        String baseUrl = resolveQrBaseUrl();
        return baseUrl + "?tenant=" + station.getTenantId() + "&token=" + stationCode;
    }

    private String resolveQrBaseUrl() {
        // Always use production Vercel frontend URL
        // Ignore localhost environments - QR codes must point to production
        String baseUrl = normalizeLocalhostBaseUrl(customerBaseUrl);

        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://order-app-qr.vercel.app/customer/menu";
        }

        if (baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1")) {
            return "https://order-app-qr.vercel.app/customer/menu";
        }

        return baseUrl;
    }

    private StationEntity loadTenantStation(Long tenantId, Long stationId) {
        StationEntity station = stationRepository.findByTenantIdAndId(tenantId, stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Postazione non trovata"));

        if (!tenantId.equals(station.getTenantId())) {
            throw new UnauthorizedTenantAccessException("Postazione non appartenente al tenant");
        }

        return station;
    }

    private String generateSecureCode() {
        byte[] random = new byte[16];
        String code;
        do {
            SECURE_RANDOM.nextBytes(random);
            code = Base64.getUrlEncoder().withoutPadding().encodeToString(random).replace("-", "").replace("_", "");
            if (code.length() > 22) {
                code = code.substring(0, 22);
            }
        } while (stationQrCodeRepository.findByCodeIgnoreCase(code).isPresent());
        return code;
    }

    private String buildQrValue(StationEntity station, String code) {
        Tenant tenant = tenantRepository.findById(station.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant non trovato"));
        return buildQrMenuUrl(tenant.getSlug(), code);
    }

    private StationQrResponse toResponse(StationQrCodeEntity qr) {
        String normalizedQrValue = normalizeLocalhostBaseUrl(qr.getQrValue());
        return new StationQrResponse(
                qr.getId(),
                qr.getStation() != null ? qr.getStation().getId() : null,
                qr.getCode(),
            normalizedQrValue,
                qr.getImagePath(),
                qr.isActive(),
                qr.getGeneratedAt(),
                qr.getRegeneratedAt(),
                qr.getStation() != null ? "/api/staff/stations/" + qr.getStation().getId() + "/qr/download" : null
        );
    }

    private byte[] renderQrPng(String payload) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                    payload,
                    BarcodeFormat.QR_CODE,
                    320,
                    320,
                    java.util.Map.of(EncodeHintType.MARGIN, 1)
            );

            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix, new MatrixToImageConfig(0xFF000000, Color.WHITE.getRGB()));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException | IllegalArgumentException ex) {
            throw new BusinessException("Impossibile generare il QR code");
        }
    }

    private byte[] renderQrPngWithInfo(String payload, String stationName, String areaName) {
        try {
            // Generate QR code with HIGH error correction to allow logo overlay
            BitMatrix matrix = new MultiFormatWriter().encode(
                    payload,
                    BarcodeFormat.QR_CODE,
                    300,
                    300,
                    java.util.Map.of(
                        EncodeHintType.MARGIN, 1,
                        EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
                    )
            );

            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(matrix, new MatrixToImageConfig(0xFF000000, Color.WHITE.getRGB()));
            BufferedImage logoImage = null;
            try {
                logoImage = loadStationLogo();
            } catch (IOException ex) {
                // If logo not available, proceed without logo (fallback to plain QR)
                logoImage = null;
            }

            // Add logo PNG to QR code (addLogoToQr will handle null and return original QR)
            BufferedImage qrWithLogo = addLogoToQr(qrImage, logoImage);

            // Create composite image with branded layout
            int width = 600;
            int height = 850;
            BufferedImage compositeImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2d = compositeImage.createGraphics();
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // White background
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            // Title: "SCANSIONA IL QRCODE"
            g2d.setColor(Color.BLACK);
            java.awt.Font titleFont = new java.awt.Font("Arial", java.awt.Font.BOLD, 42);

            g2d.setFont(titleFont);
            String title = "SCANSIONA IL QRCODE";
            java.awt.FontMetrics titleMetrics = g2d.getFontMetrics(titleFont);
            int titleX = (width - titleMetrics.stringWidth(title)) / 2;
            int titleY = 70;
            g2d.drawString(title, titleX, titleY);

            // Subtitle: "SFOGLIA IL MENU, ED ORDINA DAL TUO SMARTPHONE"
            java.awt.Font subtitleFont = new java.awt.Font("Arial", java.awt.Font.PLAIN, 16);
            g2d.setFont(subtitleFont);
            String subtitle = "SFOGLIA IL MENU, ED ORDINA DAL TUO SMARTPHONE";
            java.awt.FontMetrics subtitleMetrics = g2d.getFontMetrics(subtitleFont);
            int subtitleX = (width - subtitleMetrics.stringWidth(subtitle)) / 2;
            int subtitleY = titleY + 40;
            g2d.drawString(subtitle, subtitleX, subtitleY);

            // Draw QR code centered
            int qrX = (width - qrWithLogo.getWidth()) / 2;
            int qrY = 180;
            g2d.drawImage(qrWithLogo, qrX, qrY, null);

            // Footer: nome della postazione
            java.awt.Font footerFont = new java.awt.Font("Arial", java.awt.Font.BOLD, 28);
            g2d.setFont(footerFont);
            String footer = stationName == null || stationName.isBlank() ? "Postazione" : stationName;
            java.awt.FontMetrics footerMetrics = g2d.getFontMetrics(footerFont);
            int footerX = (width - footerMetrics.stringWidth(footer)) / 2;
            int footerY = height - 60;
            g2d.drawString(footer, footerX, footerY);
            
            g2d.dispose();
            
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(compositeImage, "PNG", output);
            return output.toByteArray();
            
        } catch (WriterException | IOException | IllegalArgumentException ex) {
            log.warn("Failed to generate QR with info, falling back to plain QR: {}", ex.getMessage());
            return renderQrPng(payload);
        }
    }

    private BufferedImage addLogoToQr(BufferedImage qrImage, BufferedImage logoImage) {
        try {
            if (logoImage == null) {
                return qrImage;
            }
            int qrSize = qrImage.getWidth();
            int logoSize = Math.max(64, qrSize / 4);
            int padding = Math.max(8, logoSize / 8);

            BufferedImage result = new BufferedImage(qrSize, qrSize, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2d = result.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(qrImage, 0, 0, null);

            int logoX = (qrSize - logoSize) / 2;
            int logoY = (qrSize - logoSize) / 2;
            int boxX = logoX - padding;
            int boxY = logoY - padding;
            int boxSize = logoSize + (2 * padding);

            g2d.setColor(Color.WHITE);
            g2d.fillRoundRect(boxX, boxY, boxSize, boxSize, 18, 18);

            BufferedImage scaledLogo = new BufferedImage(logoSize, logoSize, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D logoGraphics = scaledLogo.createGraphics();
            logoGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            logoGraphics.drawImage(logoImage, 0, 0, logoSize, logoSize, null);
            logoGraphics.dispose();

            g2d.drawImage(scaledLogo, logoX, logoY, null);
            g2d.setColor(new Color(200, 200, 200));
            g2d.setStroke(new java.awt.BasicStroke(1.5f));
            g2d.drawRoundRect(boxX, boxY, boxSize, boxSize, 18, 18);

            g2d.dispose();
            return result;

        } catch (Exception ex) {
            // If logo creation fails, return original QR
            return qrImage;
        }
    }

    private BufferedImage loadStationLogo() throws IOException {
        for (Path candidate : List.of(
                Paths.get("ordering-frontend/src/app/features/public/icons/logo.png"),
                Paths.get("../ordering-frontend/src/app/features/public/icons/logo.png"),
                Paths.get(System.getProperty("user.dir"), "ordering-frontend/src/app/features/public/icons/logo.png"),
                Paths.get(System.getProperty("user.dir"), "../ordering-frontend/src/app/features/public/icons/logo.png")
        )) {
            if (Files.exists(candidate)) {
                BufferedImage logo = ImageIO.read(candidate.toFile());
                if (logo != null) {
                    return logo;
                }
            }
        }

        try (InputStream inputStream = getClass().getResourceAsStream("/logo.png")) {
            if (inputStream != null) {
                BufferedImage logo = ImageIO.read(inputStream);
                if (logo != null) {
                    return logo;
                }
            }
        }

        throw new IOException("Logo PNG non trovato");
    }

    private String resolveDownloadPayload(StationQrCodeEntity qr) {
        if (qr.getQrValue() != null && !qr.getQrValue().isBlank()) {
            return normalizeLocalhostBaseUrl(qr.getQrValue());
        }

        if (qr.getStation() != null) {
            return buildQrMenuUrlForStation(qr.getStation(), qr.getCode());
        }

        return qr.getCode();
    }

    private boolean isOrderingEnabledByTenantSettings(Tenant tenant) {
        JsonNode root = readOpeningConfigOrEmpty(tenant.getOpeningConfigJson());

        boolean orderingPaused = root.path("orderingPaused").asBoolean(false);
        if (orderingPaused) {
            return false;
        }

        JsonNode opening = root.path("openingHours");
        String open = opening.path("open").asText("00:00");
        String close = opening.path("close").asText("23:59");

        Integer openMinutes = parseMinutes(open);
        Integer closeMinutes = parseMinutes(close);
        if (openMinutes == null || closeMinutes == null) {
            return true;
        }

        ZoneId zone;
        try {
            zone = tenant.getTimezone() != null ? ZoneId.of(tenant.getTimezone()) : ZoneId.systemDefault();
        } catch (Exception ex) {
            zone = ZoneId.systemDefault();
        }

        LocalTime now = ZonedDateTime.now(zone).toLocalTime();
        int nowMinutes = now.getHour() * 60 + now.getMinute();

        boolean wraps = closeMinutes < openMinutes;
        if (!wraps) {
            return nowMinutes >= openMinutes && nowMinutes <= closeMinutes;
        }

        return nowMinutes >= openMinutes || nowMinutes <= closeMinutes;
    }

    private JsonNode readOpeningConfigOrEmpty(String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode parsed = objectMapper.readTree(json);
            return parsed != null ? parsed : objectMapper.createObjectNode();
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private Integer parseMinutes(String time) {
        if (time == null) {
            return null;
        }
        String trimmed = time.trim();
        if (trimmed.length() != 5 || trimmed.charAt(2) != ':') {
            return null;
        }
        try {
            int hh = Integer.parseInt(trimmed.substring(0, 2));
            int mm = Integer.parseInt(trimmed.substring(3, 5));
            if (hh < 0 || hh > 23 || mm < 0 || mm > 59) {
                return null;
            }
            return hh * 60 + mm;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeLocalhostBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        try {
            String trimmed = value.trim();
            URI uri = URI.create(trimmed);
            String host = uri.getHost();
            if (host == null) {
                return trimmed;
            }

            boolean isLocalHost = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
            if (isLocalHost) {
                String productionBase = "https://order-app-qr.vercel.app/customer/menu";
                String path = uri.getPath();
                String query = uri.getQuery();
                String fragment = uri.getFragment();

                String normalizedPath = (path == null || path.isBlank() || "/".equals(path))
                        ? "/customer/menu"
                        : path;

                URI normalized = new URI(
                        "https",
                        null,
                        "order-app-qr.vercel.app",
                        -1,
                        normalizedPath,
                        query,
                        fragment
                );

                if (normalized.toString().startsWith("https://order-app-qr.vercel.app/customer/menu")) {
                    return normalized.toString();
                }

                return productionBase + (query != null && !query.isBlank() ? "?" + query : "");
            }

            return trimmed;
        } catch (Exception ex) {
            return "https://order-app-qr.vercel.app/customer/menu";
        }
    }
}