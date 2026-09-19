package com.codingarena.common.ratelimit;

import com.codingarena.auth.AuthApplication;
import com.codingarena.auth.dto.LoginRequest;
import com.codingarena.auth.dto.RegisterRequest;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.config.TestRedisConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AuthApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.codingarena.challenge.repository.ChallengeRepository challengeRepository;

    @Autowired
    private com.codingarena.match.repository.MatchRepository matchRepository;

    @Autowired
    private com.codingarena.match.repository.ProblemRepository problemRepository;

    @Autowired
    private com.codingarena.friendship.repository.FriendshipRepository friendshipRepository;

    @Autowired
    private com.codingarena.auth.repository.UserPreferencesRepository userPreferencesRepository;

    @Autowired
    private com.codingarena.match.repository.TestCaseRepository testCaseRepository;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        rateLimitService.clear();
        friendshipRepository.deleteAll();
        userPreferencesRepository.deleteAll();
        challengeRepository.deleteAll();
        matchRepository.deleteAll();
        testCaseRepository.deleteAll();
        problemRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_ExceedingLimit_Returns429() throws Exception {
        String clientIp = "192.168.1.100";

        // Limit is 3 requests per minute for register
        for (int i = 1; i <= 3; i++) {
            RegisterRequest req = new RegisterRequest("user" + i, "user" + i + "@example.com", "pass12345");
            mockMvc.perform(post("/api/auth/register")
                            .header("X-Forwarded-For", clientIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        // 4th request must be rejected with 429
        RegisterRequest req4 = new RegisterRequest("user4", "user4@example.com", "pass12345");
        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req4)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Rate limit exceeded, try again later"));
    }

    @Test
    void login_ExceedingLimit_Returns429() throws Exception {
        String clientIp = "192.168.1.101";

        // Register a user first
        RegisterRequest reg = new RegisterRequest("loginuser", "loginuser@example.com", "pass12345");
        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "192.168.1.200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest("loginuser@example.com", "pass12345");

        // Limit is 5 requests per minute for login
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", clientIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isOk());
        }

        // 6th request must be rejected with 429
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Rate limit exceeded, try again later"));
    }

    @Test
    void matchmakingQueue_UserKeyed_ExceedingLimitReturns429_OtherUserUnaffected() throws Exception {
        String sharedIp = "10.0.0.1";

        // Register User A
        RegisterRequest regA = new RegisterRequest("usera", "usera@example.com", "pass12345");
        MvcResult resA = mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "10.0.0.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regA)))
                .andExpect(status().isCreated())
                .andReturn();
        String tokenA = objectMapper.readTree(resA.getResponse().getContentAsString()).get("token").asText();

        // Register User B
        RegisterRequest regB = new RegisterRequest("userb", "userb@example.com", "pass12345");
        MvcResult resB = mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "10.0.0.11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regB)))
                .andExpect(status().isCreated())
                .andReturn();
        String tokenB = objectMapper.readTree(resB.getResponse().getContentAsString()).get("token").asText();

        // Limit is 10 requests per minute for matchmaking queue
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post("/api/matchmaking/queue")
                            .header("Authorization", "Bearer " + tokenA)
                            .header("X-Forwarded-For", sharedIp))
                    .andExpect(status().isOk());
        }

        // 11th request for User A returns 429
        mockMvc.perform(post("/api/matchmaking/queue")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Forwarded-For", sharedIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Rate limit exceeded, try again later"));

        // User B sharing the same IP is completely unaffected
        mockMvc.perform(post("/api/matchmaking/queue")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Forwarded-For", sharedIp))
                .andExpect(status().isOk());
    }

    @Test
    void getEndpoints_AreNotRateLimited() throws Exception {
        RegisterRequest reg = new RegisterRequest("getuser", "getuser@example.com", "pass12345");
        MvcResult res = mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "10.0.0.50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();
        String token = objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();

        // Perform 15 GET /api/auth/me requests - should all succeed without 429
        for (int i = 0; i < 15; i++) {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }
}
