package com.codingarena.auth.controller;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.codingarena.friendship.repository.FriendshipRepository friendshipRepository;

    @Autowired
    private com.codingarena.auth.repository.UserPreferencesRepository userPreferencesRepository;

    @Autowired
    private com.codingarena.challenge.repository.ChallengeRepository challengeRepository;

    @Autowired
    private com.codingarena.match.repository.MatchRepository matchRepository;

    @Autowired
    private com.codingarena.match.repository.ProblemRepository problemRepository;

    @Autowired
    private com.codingarena.match.repository.TestCaseRepository testCaseRepository;

    @Autowired
    private com.codingarena.common.ratelimit.RateLimitService rateLimitService;

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
    void register_Success_Returns201AndToken() throws Exception {
        RegisterRequest request = new RegisterRequest("john_doe", "john@example.com", "secret123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.username").value("john_doe"))
                .andExpect(jsonPath("$.user.email").value("john@example.com"))
                .andExpect(jsonPath("$.user.rating").value(0))
                .andExpect(jsonPath("$.user.wins").value(0))
                .andExpect(jsonPath("$.user.losses").value(0));
    }

    @Test
    void register_DuplicateEmail_Returns409WithErrorShape() throws Exception {
        RegisterRequest request1 = new RegisterRequest("user1", "same@example.com", "password123");
        RegisterRequest request2 = new RegisterRequest("user2", "same@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already exists"));
    }

    @Test
    void register_DuplicateUsername_Returns409WithErrorShape() throws Exception {
        RegisterRequest request1 = new RegisterRequest("same_user", "email1@example.com", "password123");
        RegisterRequest request2 = new RegisterRequest("same_user", "email2@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    @Test
    void login_Success_Returns200AndToken() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("jane_doe", "jane@example.com", "password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("jane@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.username").value("jane_doe"))
                .andExpect(jsonPath("$.user.email").value("jane@example.com"))
                .andExpect(jsonPath("$.user.rating").value(0))
                .andExpect(jsonPath("$.user.wins").value(0))
                .andExpect(jsonPath("$.user.losses").value(0));
    }

    @Test
    void login_BadCredentials_Returns401WithErrorShape() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("jane_doe2", "jane2@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("jane2@example.com", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void me_ValidToken_Returns200AndUserProfile() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("alice", "alice@example.com", "password123");
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = registerResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.rating").value(0))
                .andExpect(jsonPath("$.wins").value(0))
                .andExpect(jsonPath("$.losses").value(0));
    }

    @Test
    void me_MissingToken_Returns401WithErrorShape() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void me_InvalidToken_Returns401WithErrorShape() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }
}
