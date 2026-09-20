package com.codingarena.submission.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import static org.junit.jupiter.api.Assertions.*;

class Judge0ClientTest {

    @Test
    void execute_MockModeTrue_CodeContainsExpectedOutput_ReturnsAccepted() {
        Judge0Client client = new Judge0Client(new RestTemplateBuilder(), "http://localhost:2358", true);

        Judge0Response response = client.execute("def twoSum(): return [0, 1]", "python", "2 7 11 15\n9", "[0, 1]");

        assertNotNull(response);
        assertNotNull(response.getStatus());
        assertEquals(3, response.getStatus().getId());
        assertEquals("Accepted", response.getStatus().getDescription());
        assertEquals("[0, 1]", response.getStdout());
    }

    @Test
    void execute_MockModeTrue_CodeDoesNotContainExpectedOutput_ReturnsWrongAnswer() {
        Judge0Client client = new Judge0Client(new RestTemplateBuilder(), "http://localhost:2358", true);

        Judge0Response response = client.execute("def twoSum(): return [0, 2]", "python", "2 7 11 15\n9", "[0, 1]");

        assertNotNull(response);
        assertNotNull(response.getStatus());
        assertEquals(4, response.getStatus().getId());
        assertEquals("Wrong Answer", response.getStatus().getDescription());
        assertEquals("", response.getStdout());
    }

    @Test
    void execute_MockModeTrue_CaseInsensitiveMatching() {
        Judge0Client client = new Judge0Client(new RestTemplateBuilder(), "http://localhost:2358", true);

        Judge0Response response = client.execute("print('TRUE')", "python", "", "true");

        assertNotNull(response);
        assertEquals(3, response.getStatus().getId());
        assertEquals("true", response.getStdout());
    }

    @Test
    void execute_MockModeFalse_DefaultsToFalse() {
        Judge0Client client = new Judge0Client(new RestTemplateBuilder(), "http://localhost:2358", false);
        assertFalse(client.isMockMode());
    }

    @Test
    void execute_IncludesRapidApiHeaders_WhenRapidApiKeyProvided() {
        java.util.concurrent.atomic.AtomicReference<org.springframework.http.HttpHeaders> capturedHeaders = new java.util.concurrent.atomic.AtomicReference<>();

        RestTemplateBuilder builder = new RestTemplateBuilder().additionalInterceptors((request, body, execution) -> {
            capturedHeaders.set(request.getHeaders());
            return new org.springframework.http.client.ClientHttpResponse() {
                @Override
                public org.springframework.http.HttpStatusCode getStatusCode() {
                    return org.springframework.http.HttpStatus.OK;
                }

                @Override
                public int getRawStatusCode() {
                    return 200;
                }

                @Override
                public String getStatusText() {
                    return "OK";
                }

                @Override
                public void close() {}

                @Override
                public java.io.InputStream getBody() {
                    return new java.io.ByteArrayInputStream(
                            "{\"status\":{\"id\":3,\"description\":\"Accepted\"},\"stdout\":\"output\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8)
                    );
                }

                @Override
                public org.springframework.http.HttpHeaders getHeaders() {
                    org.springframework.http.HttpHeaders responseHeaders = new org.springframework.http.HttpHeaders();
                    responseHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                    return responseHeaders;
                }
            };
        });

        Judge0Client client = new Judge0Client(builder, "https://judge0-ce.p.rapidapi.com", false, "test-rapidapi-key-123");
        Judge0Response response = client.execute("print(1)", "python", "", "");

        assertNotNull(response);
        assertEquals(3, response.getStatus().getId());
        assertNotNull(capturedHeaders.get());
        assertEquals("test-rapidapi-key-123", capturedHeaders.get().getFirst("X-RapidAPI-Key"));
        assertEquals("judge0-ce.p.rapidapi.com", capturedHeaders.get().getFirst("X-RapidAPI-Host"));
        assertEquals(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, capturedHeaders.get().getFirst(org.springframework.http.HttpHeaders.CONTENT_TYPE));
    }

    @Test
    void execute_OmitsRapidApiKeyHeader_WhenKeyNotConfigured() {
        java.util.concurrent.atomic.AtomicReference<org.springframework.http.HttpHeaders> capturedHeaders = new java.util.concurrent.atomic.AtomicReference<>();

        RestTemplateBuilder builder = new RestTemplateBuilder().additionalInterceptors((request, body, execution) -> {
            capturedHeaders.set(request.getHeaders());
            return new org.springframework.http.client.ClientHttpResponse() {
                @Override
                public org.springframework.http.HttpStatusCode getStatusCode() {
                    return org.springframework.http.HttpStatus.OK;
                }

                @Override
                public int getRawStatusCode() {
                    return 200;
                }

                @Override
                public String getStatusText() {
                    return "OK";
                }

                @Override
                public void close() {}

                @Override
                public java.io.InputStream getBody() {
                    return new java.io.ByteArrayInputStream(
                            "{\"status\":{\"id\":3,\"description\":\"Accepted\"},\"stdout\":\"output\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8)
                    );
                }

                @Override
                public org.springframework.http.HttpHeaders getHeaders() {
                    org.springframework.http.HttpHeaders responseHeaders = new org.springframework.http.HttpHeaders();
                    responseHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                    return responseHeaders;
                }
            };
        });

        Judge0Client client = new Judge0Client(builder, "https://judge0-ce.p.rapidapi.com", false, "");
        Judge0Response response = client.execute("print(1)", "python", "", "");

        assertNotNull(response);
        assertNotNull(capturedHeaders.get());
        assertNull(capturedHeaders.get().getFirst("X-RapidAPI-Key"));
        assertEquals("judge0-ce.p.rapidapi.com", capturedHeaders.get().getFirst("X-RapidAPI-Host"));
    }

    @Test
    void normalizeJavaCode_RenamesPublicClassSolutionToMain() {
        Judge0Client client = new Judge0Client(new RestTemplateBuilder(), "http://localhost:2358", true);
        String input = "public class Solution {\n    public static void main(String[] args) {\n        Solution s = new Solution();\n    }\n}";
        String normalized = client.normalizeJavaCode(input);

        assertTrue(normalized.contains("public class Main"));
        assertTrue(normalized.contains("Main s = new Main()"));
        assertFalse(normalized.contains("Solution"));
    }

    @Test
    void normalizeJavaCode_LeavesMainClassUntouched() {
        Judge0Client client = new Judge0Client(new RestTemplateBuilder(), "http://localhost:2358", true);
        String input = "public class Main {\n    public static void main(String[] args) {}\n}";
        String normalized = client.normalizeJavaCode(input);

        assertEquals(input, normalized);
    }

    @Test
    void normalizeJavaCode_RenamesNonPublicClassSolutionToMain() {
        Judge0Client client = new Judge0Client(new RestTemplateBuilder(), "http://localhost:2358", true);
        String input = "class Solution {\n    public static void main(String[] args) {}\n}";
        String normalized = client.normalizeJavaCode(input);

        assertTrue(normalized.contains("class Main"));
        assertFalse(normalized.contains("Solution"));
    }
}
