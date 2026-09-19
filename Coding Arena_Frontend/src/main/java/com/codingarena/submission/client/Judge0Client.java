package com.codingarena.submission.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Judge0Client {

    private static final Logger log = LoggerFactory.getLogger(Judge0Client.class);

    private final RestTemplate restTemplate;
    private final String judge0Url;
    private final boolean mockMode;
    private final String rapidApiKey;

@org.springframework.beans.factory.annotation.Autowired
public Judge0Client(
        RestTemplateBuilder restTemplateBuilder,
        @Value("${judge0.url:https://judge0-ce.p.rapidapi.com}") String judge0Url,
        @Value("${judge0.mock-mode:false}") boolean mockMode,
        @Value("${rapidapi.key:}") String rapidApiKey) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.judge0Url = normalizeUrl(judge0Url);
        this.mockMode = mockMode;
        this.rapidApiKey = rapidApiKey != null ? rapidApiKey.trim() : "";
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "https://judge0-ce.p.rapidapi.com";
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    public Judge0Client(
            RestTemplateBuilder restTemplateBuilder,
            String judge0Url,
            boolean mockMode) {
        this(restTemplateBuilder, judge0Url, mockMode, "");
    }

    public Judge0Response execute(String code, String language, String stdin, String expectedOutput) {
        if (mockMode) {
            log.warn("JUDGE0_MOCK_MODE is enabled — returning simulated verdict, NOT real code execution");
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            Judge0Response response = new Judge0Response();
            response.setTime("0.300");
            response.setMemory(12000);

            boolean matches = code != null
                    && expectedOutput != null
                    && code.toLowerCase(Locale.ROOT).trim().contains(expectedOutput.toLowerCase(Locale.ROOT).trim());

            if (matches) {
                response.setStatus(new Judge0Response.Judge0Status(3, "Accepted"));
                response.setStdout(expectedOutput);
            } else {
                response.setStatus(new Judge0Response.Judge0Status(4, "Wrong Answer"));
                response.setStdout("");
            }
            return response;
        }

        int languageId = resolveLanguageId(language);
        if (languageId == 62 && code != null) {
            code = normalizeJavaCode(code);
        }

        Judge0Request request = new Judge0Request(code, languageId, stdin, expectedOutput);
        request.setCpuTimeLimit(5.0);
        request.setMemoryLimit(256000);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-RapidAPI-Host", "judge0-ce.p.rapidapi.com");
        if (!rapidApiKey.isEmpty()) {
            headers.set("X-RapidAPI-Key", rapidApiKey);
        }

        String url = judge0Url + "/submissions?wait=true";

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonPayload = mapper.writeValueAsString(request);
            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
            ResponseEntity<Judge0Response> response = restTemplate.postForEntity(url, entity, Judge0Response.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to execute code on Judge0 at {}: {}", url, e.getMessage());
            Judge0Response fallback = new Judge0Response();
            Judge0Response.Judge0Status status = new Judge0Response.Judge0Status(13, "Internal Error");
            fallback.setStatus(status);
            fallback.setStderr("Judge0 execution failed: " + e.getMessage());
            return fallback;
        }
    }

    public int resolveLanguageId(String language) {
        if (language == null) return 71; // Default Python 3
        String lang = language.trim().toLowerCase(Locale.ROOT);

        return switch (lang) {
            case "java", "java17" -> 62;      // Java (OpenJDK 13/17)
            case "python", "python3", "py" -> 71; // Python (3.8.1)
            case "cpp", "c++", "g++" -> 54;    // C++ (GCC 9.2.0)
            case "c", "gcc" -> 50;             // C (GCC 9.2.0)
            case "javascript", "js", "node" -> 63; // JavaScript (Node.js 12.14.0)
            case "typescript", "ts" -> 74;     // TypeScript (3.7.4)
            default -> 71;
        };
    }

    public String normalizeJavaCode(String code) {
        if (code == null) return null;
        if (Pattern.compile("\\bclass\\s+Main\\b").matcher(code).find()) {
            return code;
        }
        Matcher publicMatcher = Pattern.compile("(?m)^(\\s*public\\s+class\\s+)([A-Za-z0-9_]+)").matcher(code);
        if (publicMatcher.find()) {
            String originalClassName = publicMatcher.group(2);
            return code.replaceAll("\\b" + Pattern.quote(originalClassName) + "\\b", "Main");
        }
        Matcher classMatcher = Pattern.compile("(?m)^(\\s*class\\s+)([A-Za-z0-9_]+)").matcher(code);
        if (classMatcher.find()) {
            String originalClassName = classMatcher.group(2);
            return code.replaceAll("\\b" + Pattern.quote(originalClassName) + "\\b", "Main");
        }
        return code;
    }

    public boolean isMockMode() {
        return mockMode;
    }
}
