package com.orderapp.ordering.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class TemporaryPasswordGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RNG = new SecureRandom();

    public String generate(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("Temporary password length must be >= 8");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = RNG.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(idx));
        }
        return sb.toString();
    }

    public String generateDefault() {
        return generate(12);
    }
}
