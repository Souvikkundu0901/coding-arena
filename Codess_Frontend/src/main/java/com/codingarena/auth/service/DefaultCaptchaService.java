package com.codingarena.auth.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;

import java.time.Duration;
import java.util.List;

@Service
public class DefaultCaptchaService implements CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(DefaultCaptchaService.class);
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final String secretKey;
    private final RestOperations restTemplate;

    @Autowired
    public DefaultCaptchaService(@Value("${captcha.secret-key:}") String secretKey,
                                 RestTemplateBuilder restTemplateBuilder) {
        this.secretKey = secretKey != null ? secretKey.trim() : "";
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    public DefaultCaptchaService(String secretKey, RestOperations restTemplate) {
        this.secretKey = secretKey != null ? secretKey.trim() : "";
        this.restTemplate = restTemplate;
    }

    @Override
    public void verifyToken(String captchaToken) {
        if (secretKey.isEmpty()) {
            log.warn("RECAPTCHA_SECRET_KEY is not configured; skipping CAPTCHA verification (local dev mode)");
            return;
        }

        if (captchaToken == null || captchaToken.trim().isEmpty()) {
            throw new IllegalArgumentException("CAPTCHA verification failed");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("secret", secretKey);
            requestBody.add("response", captchaToken.trim());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(requestBody, headers);
            RecaptchaResponse response = restTemplate.postForObject(VERIFY_URL, entity, RecaptchaResponse.class);

            if (response == null || !response.isSuccess()) {
                log.warn("reCAPTCHA verification returned failure: {}", response != null ? response.getErrorCodes() : "null response");
                throw new IllegalArgumentException("CAPTCHA verification failed");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify CAPTCHA with Google: {}", e.getMessage());
            throw new IllegalArgumentException("CAPTCHA verification failed");
        }
    }

    @Override
    public boolean isConfigured() {
        return !secretKey.isEmpty();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecaptchaResponse {
        private boolean success;

        @JsonProperty("error-codes")
        private List<String> errorCodes;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public List<String> getErrorCodes() {
            return errorCodes;
        }

        public void setErrorCodes(List<String> errorCodes) {
            this.errorCodes = errorCodes;
        }
    }
}
