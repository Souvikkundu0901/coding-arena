package com.codingarena.auth.service;

public interface CaptchaService {
    void verifyToken(String captchaToken);
    boolean isConfigured();
}
