package org.example.authservice.domain;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class VerificationCodeGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int CODE_MIN = 100000;
    private static final int CODE_MAX = 999999;

    public String generateVerificationCode() {
        int code = SECURE_RANDOM.nextInt(CODE_MIN, CODE_MAX + 1);
        return String.valueOf(code);
    }
}