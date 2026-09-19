package com.codingarena.matchmaking.controller;

import com.codingarena.auth.AuthApplication;
import com.codingarena.auth.dto.RegisterRequest;
import com.codingarena.auth.model.User;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.config.TestRedisConfig;
import com.codingarena.match.model.Match;
import com.codingarena.match.model.Problem;
import com.codingarena.match.repository.MatchRepository;
import com.codingarena.match.repository.ProblemRepository;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AuthApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class MatchmakingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private com.codingarena.friendship.repository.FriendshipRepository friendshipRepository;

    @Autowired
    private com.codingarena.auth.repository.UserPreferencesRepository userPreferencesRepository;

    @Autowired
    private com.codingarena.match.repository.TestCaseRepository testCaseRepository;

    @Autowired
    private com.codingarena.challenge.repository.ChallengeRepository challengeRepository;

    @Autowired
    private com.codingarena.common.ratelimit.RateLimitService rateLimitService;

    @Autowired
    private ObjectMapper objectMapper;

    private String user1Token;
    private User user1;

    private String user2Token;
    private User user2;

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

        // Register User 1
        RegisterRequest req1 = new RegisterRequest("player1", "player1@example.com", "password123");
        MvcResult res1 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();

        user1Token = objectMapper.readTree(res1.getResponse().getContentAsString()).get("token").asText();
        user1 = userRepository.findByEmail("player1@example.com").orElseThrow();

        // Register User 2
        RegisterRequest req2 = new RegisterRequest("player2", "player2@example.com", "password123");
        MvcResult res2 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn();

        user2Token = objectMapper.readTree(res2.getResponse().getContentAsString()).get("token").asText();
        user2 = userRepository.findByEmail("player2@example.com").orElseThrow();
    }

    @Test
    void joinQueue_AuthenticatedUser_Returns200() throws Exception {
        mockMvc.perform(post("/api/matchmaking/queue")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Joined matchmaking queue successfully"));
    }

    @Test
    void leaveQueue_AuthenticatedUser_Returns200() throws Exception {
        mockMvc.perform(delete("/api/matchmaking/queue")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Left matchmaking queue successfully"));
    }

    @Test
    void getMatchDetails_PlayerInMatch_Returns200() throws Exception {
        Problem problem = problemRepository.save(new Problem("Two Sum", "EASY", "Find target sum indices"));
        LocalDateTime now = LocalDateTime.now();
        Match match = matchRepository.save(new Match(user1, user2, problem, "IN_PROGRESS", now, now.plusMinutes(15)));

        mockMvc.perform(get("/api/matches/" + match.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(match.getId().toString()))
                .andExpect(jsonPath("$.playerAUsername").value("player1"))
                .andExpect(jsonPath("$.playerBUsername").value("player2"))
                .andExpect(jsonPath("$.players.length()").value(2))
                .andExpect(jsonPath("$.players[0].username").value("player1"))
                .andExpect(jsonPath("$.players[1].username").value("player2"))
                .andExpect(jsonPath("$.problem.title").value("Two Sum"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void getMatchDetails_UnauthorizedUser_Returns403() throws Exception {
        RegisterRequest req3 = new RegisterRequest("outsider", "outsider@example.com", "password123");
        MvcResult res3 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isCreated())
                .andReturn();
        String outsiderToken = objectMapper.readTree(res3.getResponse().getContentAsString()).get("token").asText();

        Problem problem = problemRepository.save(new Problem("Two Sum", "EASY", "Find target sum indices"));
        Match match = matchRepository.save(new Match(user1, user2, problem, "IN_PROGRESS", LocalDateTime.now()));

        mockMvc.perform(get("/api/matches/" + match.getId())
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }
}
