package com.orderapp.ordering.config;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.orderapp.ordering.multitenant.TenantNotResolvedException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {
	@ExceptionHandler(TenantNotResolvedException.class)
	public ResponseEntity<?> handleTenantNotResolved(TenantNotResolvedException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
			"timestamp", OffsetDateTime.now().toString(),
			"status", 400,
			"error", "TENANT_NOT_RESOLVED",
			"message", ex.getMessage(),
			"path", request.getRequestURI()
		));
	}
}

