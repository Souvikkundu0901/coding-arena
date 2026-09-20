package com.codingarena.challenge.controller;

import com.codingarena.auth.AuthApplication;
import com.codingarena.auth.dto.RegisterRequest;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.challenge.dto.CreateChallengeRequest;
import com.codingarena.challenge.model.Challenge;
import com.codingarena.challenge.repository.ChallengeRepository;
import com.codingarena.common.ratelimit.RateLimitService;
import com.codingarena.config.TestRedisConfig;
import com.codingarena.match.repository.MatchRepository;
import com.codingarena.match.repository.ProblemRepository;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AuthApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class ChallengeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private com.codingarena.match.repository.TestCaseRepository testCaseRepository;

    @Autowired
    private com.codingarena.friendship.repository.FriendshipRepository friendshipRepository;

    @Autowired
    private com.codingarena.auth.repository.UserPreferencesRepository userPreferencesRepository;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private ObjectMapper objectMapper;

    private String userAToken;
    private String userBToken;
    private String userCToken;

    @BeforeEach
    void setUp() throws Exception {
        rateLimitService.clear();
        friendshipRepository.deleteAll();
        userPreferencesRepository.deleteAll();
        challengeRepository.deleteAll();
        matchRepository.deleteAll();
        testCaseRepository.deleteAll();
        problemRepository.deleteAll();
        userRepository.deleteAll();

        // Register User A
        RegisterRequest reqA = new RegisterRequest("usera", "usera@example.com", "pass12345");
        MvcResult resA = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(status().isCreated())
                .andReturn();
        userAToken = objectMapper.readTree(resA.getResponse().getContentAsString()).get("token").asText();

        // Register User B
        RegisterRequest reqB = new RegisterRequest("userb", "userb@example.com", "pass12345");
        MvcResult resB = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isCreated())
                .andReturn();
        userBToken = objectMapper.readTree(resB.getResponse().getContentAsString()).get("token").asText();

        // Register User C
        RegisterRequest reqC = new RegisterRequest("userc", "userc@example.com", "pass12345");
        MvcResult resC = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqC)))
                .andExpect(status().isCreated())
                .andReturn();
        userCToken = objectMapper.readTree(resC.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void createChallenge_Success_Returns201AndPendingChallenge() throws Exception {
        CreateChallengeRequest request = new CreateChallengeRequest("userb");

        MvcResult result = mockMvc.perform(post("/api/challenges")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.challengerUsername").value("usera"))
                .andExpect(jsonPath("$.challengedUsername").value("userb"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID challengeId = UUID.fromString(jsonNode.get("id").asText());

        assertTrue(challengeRepository.findById(challengeId).isPresent());
    }

    @Test
    void createChallenge_UnknownUser_Returns404() throws Exception {
        CreateChallengeRequest request = new CreateChallengeRequest("nonexistent");

        mockMvc.perform(post("/api/challenges")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found: nonexistent"));
    }

    @Test
    void createChallenge_SelfChallenge_Returns400() throws Exception {
        CreateChallengeRequest request = new CreateChallengeRequest("usera");

        mockMvc.perform(post("/api/challenges")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot challenge yourself"));
    }

    @Test
    void createChallenge_DuplicatePendingChallenge_Returns409() throws Exception {
        CreateChallengeRequest request = new CreateChallengeRequest("userb");

        // First challenge succeeds
        mockMvc.perform(post("/api/challenges")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate challenge from User A to User B fails with 409
        mockMvc.perform(post("/api/challenges")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("A pending challenge already exists between these users"));

        // Reverse challenge from User B to User A also fails with 409
        CreateChallengeRequest reverseRequest = new CreateChallengeRequest("usera");
        mockMvc.perform(post("/api/challenges")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reverseRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("A pending challenge already exists between these users"));
    }

    @Test
    void getPendingChallenges_ReturnsPendingOnlyForCaller() throws Exception {
        CreateChallengeRequest request = new CreateChallengeRequest("userb");
        mockMvc.perform(post("/api/challenges")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // User B has 1 pending challenge
        mockMvc.perform(get("/api/challenges/pending")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].challengerUsername").value("usera"));

        // User A has 0 incoming pending challenges
        mockMvc.perform(get("/api/challenges/pending")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void acceptChallenge_Success_CreatesMatchAndSetsAccepted() throws Exception {
        CreateChallengeRequest request = new CreateChallengeRequest("userb");
        MvcResult createResult = mockMvc.perform(post("/api/challenges")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String challengeId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // User C cannot accept (403 Forbidden)
        mockMvc.perform(post("/api/challenges/" + challengeId + "/accept")
                        .header("Authorization", "Bearer " + userCToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only the challenged user can accept this challenge"));

        // User A (challenger) cannot accept (403 Forbidden)
        mockMvc.perform(post("/api/challenges/" + challengeId + "/accept")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only the challenged user can accept this challenge"));

        // User B (challenged) accepts (200 OK)
        MvcResult acceptResult = mockMvc.perform(post("/api/challenges/" + challengeId + "/accept")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.matchId").isNotEmpty())
                .andReturn();

        String matchId = objectMapper.readTree(acceptResult.getResponse().getContentAsString()).get("matchId").asText();
        assertTrue(matchRepository.findById(UUID.fromString(matchId)).isPresent());

        // Accepting again returns 409 Conflict
        mockMvc.perform(post("/api/challenges/" + challengeId + "/accept")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isConflict());
    }

    @Test
    void declineChallenge_Success_SetsDeclined() throws Exception {
        CreateChallengeRequest request = new CreateChallengeRequest("userb");
        MvcResult createResult = mockMvc.perform(post("/api/challenges")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String challengeId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // User C cannot decline (403 Forbidden)
        mockMvc.perform(post("/api/challenges/" + challengeId + "/decline")
                        .header("Authorization", "Bearer " + userCToken))
                .andExpect(status().isForbidden());

        // User B declines (200 OK)
        mockMvc.perform(post("/api/challenges/" + challengeId + "/decline")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));

        // Declining again returns 409 Conflict
        mockMvc.perform(post("/api/challenges/" + challengeId + "/decline")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isConflict());
    }
}
