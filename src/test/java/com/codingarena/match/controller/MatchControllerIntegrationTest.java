package com.codingarena.match.controller;

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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AuthApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class MatchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private com.codingarena.submission.repository.SubmissionRepository submissionRepository;

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

    private User playerA;
    private User playerB;
    private User spectator;
    private String tokenA;
    private String tokenB;
    private String tokenSpectator;
    private Problem problem;

    @BeforeEach
    void setUp() throws Exception {
        submissionRepository.deleteAll();
        matchRepository.deleteAll();
        challengeRepository.deleteAll();
        friendshipRepository.deleteAll();
        userPreferencesRepository.deleteAll();
        userRepository.deleteAll();
        problemRepository.deleteAll();

        if (rateLimitService != null) {
            rateLimitService.clear();
        }

        tokenA = registerAndGetToken("playerA", "playera@example.com", "Password123!");
        tokenB = registerAndGetToken("playerB", "playerb@example.com", "Password123!");
        tokenSpectator = registerAndGetToken("spectator", "spectator@example.com", "Password123!");

        playerA = userRepository.findByUsername("playerA").orElseThrow();
        playerB = userRepository.findByUsername("playerB").orElseThrow();
        spectator = userRepository.findByUsername("spectator").orElseThrow();

        problem = new Problem("Test Problem", "EASY", "Problem description");
        problem = problemRepository.save(problem);
    }

    private String registerAndGetToken(String username, String email, String password) throws Exception {
        RegisterRequest request = new RegisterRequest(username, email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).get("token").asText();
    }

    @Test
    void getMatchDetails_PlayerA_Returns200() throws Exception {
        Match match = matchRepository.save(new Match(playerA, playerB, problem, "IN_PROGRESS", null));

        mockMvc.perform(get("/api/matches/" + match.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(match.getId().toString()))
                .andExpect(jsonPath("$.playerAUsername").value("playerA"))
                .andExpect(jsonPath("$.playerBUsername").value("playerB"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void forfeitMatch_PlayerAForfeits_OpponentPlayerBWins_Returns200() throws Exception {
        Match match = matchRepository.save(new Match(playerA, playerB, problem, "IN_PROGRESS", null));

        mockMvc.perform(post("/api/matches/" + match.getId() + "/forfeit")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(match.getId().toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.winnerId").value(playerB.getId().toString()));

        Match updated = matchRepository.findById(match.getId()).orElseThrow();
        assertEquals("COMPLETED", updated.getStatus());
        assertEquals(playerB.getId(), updated.getWinnerId());

        User refreshedA = userRepository.findById(playerA.getId()).orElseThrow();
        User refreshedB = userRepository.findById(playerB.getId()).orElseThrow();
        assertEquals(1, refreshedB.getWins());
        assertEquals(1, refreshedA.getLosses());
    }

    @Test
    void forfeitMatch_PlayerBForfeits_OpponentPlayerAWins_Returns200() throws Exception {
        Match match = matchRepository.save(new Match(playerA, playerB, problem, "IN_PROGRESS", null));

        mockMvc.perform(post("/api/matches/" + match.getId() + "/forfeit")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(match.getId().toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.winnerId").value(playerA.getId().toString()));

        Match updated = matchRepository.findById(match.getId()).orElseThrow();
        assertEquals("COMPLETED", updated.getStatus());
        assertEquals(playerA.getId(), updated.getWinnerId());
    }

    @Test
    void forfeitMatch_Spectator_Returns403() throws Exception {
        Match match = matchRepository.save(new Match(playerA, playerB, problem, "IN_PROGRESS", null));

        mockMvc.perform(post("/api/matches/" + match.getId() + "/forfeit")
                        .header("Authorization", "Bearer " + tokenSpectator))
                .andExpect(status().isForbidden());
    }

    @Test
    void forfeitMatch_NonExistentMatch_Returns404() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(post("/api/matches/" + randomId + "/forfeit")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    void forfeitMatch_AlreadyCompletedMatch_Returns409() throws Exception {
        Match match = new Match(playerA, playerB, problem, "COMPLETED", LocalDateTime.now());
        match.setWinnerId(playerB.getId());
        match.setEndedAt(LocalDateTime.now());
        match = matchRepository.save(match);

        mockMvc.perform(post("/api/matches/" + match.getId() + "/forfeit")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isConflict());
    }

    @Test
    void forfeitMatch_AlreadyExpiredMatch_Returns409() throws Exception {
        Match match = new Match(playerA, playerB, problem, "EXPIRED", null);
        match.setEndedAt(LocalDateTime.now());
        match = matchRepository.save(match);

        mockMvc.perform(post("/api/matches/" + match.getId() + "/forfeit")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isConflict());
    }

    @Test
    void forfeitMatch_Unauthenticated_ReturnsUnauthorized() throws Exception {
        Match match = matchRepository.save(new Match(playerA, playerB, problem, "IN_PROGRESS", null));

        mockMvc.perform(post("/api/matches/" + match.getId() + "/forfeit"))
                .andExpect(status().isUnauthorized());
    }
}
