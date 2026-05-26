package com.orderapp.ordering.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderapp.ordering.dto.ResolveQrResponse;
import com.orderapp.ordering.service.StationQrCodeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/qr")
public class PublicQrController {
    private final StationQrCodeService stationQrCodeService;

    @GetMapping("/{code}")
    public ResolveQrResponse resolveQr(@PathVariable String code) {
        return stationQrCodeService.resolveStationByQrCode(code);
    }

    @GetMapping("/{code}/image")
    public ResponseEntity<byte[]> downloadQr(@PathVariable String code) {
        byte[] image = stationQrCodeService.downloadQrPngByCode(code);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=qr-" + code + ".png")
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }
}
