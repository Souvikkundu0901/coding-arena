package com.codingarena.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaptchaServiceTest {

    @Mock
    private RestOperations restTemplate;

    private DefaultCaptchaService configuredCaptchaService;
    private DefaultCaptchaService emptyCaptchaService;

    @BeforeEach
    void setUp() {
        configuredCaptchaService = new DefaultCaptchaService("test-secret-key", restTemplate);
        emptyCaptchaService = new DefaultCaptchaService("", restTemplate);
    }

    @Test
    void verifyToken_WhenSecretKeyEmpty_SkipsVerification() {
        assertDoesNotThrow(() -> emptyCaptchaService.verifyToken("any-token"));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void verifyToken_WhenTokenNullOrBlank_ThrowsException() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> configuredCaptchaService.verifyToken(null));
        assertEquals("CAPTCHA verification failed", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> configuredCaptchaService.verifyToken("   "));
        assertEquals("CAPTCHA verification failed", ex2.getMessage());

        verifyNoInteractions(restTemplate);
    }

    @Test
    void verifyToken_WhenGoogleVerificationFails_ThrowsException() {
        DefaultCaptchaService.RecaptchaResponse failResponse = new DefaultCaptchaService.RecaptchaResponse();
        failResponse.setSuccess(false);
        failResponse.setErrorCodes(List.of("invalid-input-response"));

        when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(DefaultCaptchaService.RecaptchaResponse.class)))
                .thenReturn(failResponse);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> configuredCaptchaService.verifyToken("invalid-token"));

        assertEquals("CAPTCHA verification failed", ex.getMessage());
    }

    @Test
    void verifyToken_WhenGoogleVerificationSucceeds_CompletesNormally() {
        DefaultCaptchaService.RecaptchaResponse successResponse = new DefaultCaptchaService.RecaptchaResponse();
        successResponse.setSuccess(true);

        when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(DefaultCaptchaService.RecaptchaResponse.class)))
                .thenReturn(successResponse);

        assertDoesNotThrow(() -> configuredCaptchaService.verifyToken("valid-token"));
    }
}
