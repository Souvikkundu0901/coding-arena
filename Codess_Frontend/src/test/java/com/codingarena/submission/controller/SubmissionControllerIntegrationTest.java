package com.codingarena.submission.controller;

import com.codingarena.auth.AuthApplication;
import com.codingarena.auth.dto.RegisterRequest;
import com.codingarena.auth.model.User;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.config.TestRedisConfig;
import com.codingarena.match.model.Match;
import com.codingarena.match.model.Problem;
import com.codingarena.match.model.TestCase;
import com.codingarena.match.repository.MatchRepository;
import com.codingarena.match.repository.ProblemRepository;
import com.codingarena.match.repository.TestCaseRepository;
import com.codingarena.submission.dto.SubmissionRequest;
import com.codingarena.submission.repository.SubmissionRepository;
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
class SubmissionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private com.codingarena.friendship.repository.FriendshipRepository friendshipRepository;

    @Autowired
    private com.codingarena.auth.repository.UserPreferencesRepository userPreferencesRepository;

    @Autowired
    private com.codingarena.challenge.repository.ChallengeRepository challengeRepository;

    @Autowired
    private com.codingarena.common.ratelimit.RateLimitService rateLimitService;

    @Autowired
    private ObjectMapper objectMapper;

    private String player1Token;
    private User player1;

    private String player2Token;
    private User player2;

    private String outsiderToken;
    private User outsider;

    private Match activeMatch;

    @BeforeEach
    void setUp() throws Exception {
        rateLimitService.clear();
        friendshipRepository.deleteAll();
        userPreferencesRepository.deleteAll();
        challengeRepository.deleteAll();
        submissionRepository.deleteAll();
        matchRepository.deleteAll();
        testCaseRepository.deleteAll();
        problemRepository.deleteAll();
        userRepository.deleteAll();

        // Register Player 1
        RegisterRequest req1 = new RegisterRequest("coder1", "coder1@example.com", "password123");
        MvcResult res1 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();
        player1Token = objectMapper.readTree(res1.getResponse().getContentAsString()).get("token").asText();
        player1 = userRepository.findByEmail("coder1@example.com").orElseThrow();

        // Register Player 2
        RegisterRequest req2 = new RegisterRequest("coder2", "coder2@example.com", "password123");
        MvcResult res2 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn();
        player2Token = objectMapper.readTree(res2.getResponse().getContentAsString()).get("token").asText();
        player2 = userRepository.findByEmail("coder2@example.com").orElseThrow();

        // Register Outsider
        RegisterRequest req3 = new RegisterRequest("outsider", "outsider@example.com", "password123");
        MvcResult res3 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isCreated())
                .andReturn();
        outsiderToken = objectMapper.readTree(res3.getResponse().getContentAsString()).get("token").asText();
        outsider = userRepository.findByEmail("outsider@example.com").orElseThrow();

        // Create Problem and Test Case
        Problem problem = problemRepository.save(new Problem("Two Sum", "EASY", "Find indices that add up to target"));
        testCaseRepository.save(new TestCase(problem, "[2,7,11,15], 9", "[0,1]", true));

        // Create Match
        activeMatch = matchRepository.save(new Match(player1, player2, problem, "IN_PROGRESS", LocalDateTime.now()));
    }

    @Test
    void createSubmission_PlayerInMatch_Returns201AndAccepted() throws Exception {
        SubmissionRequest request = new SubmissionRequest("print('[0,1]')", "python");

        mockMvc.perform(post("/api/matches/" + activeMatch.getId() + "/submissions")
                        .header("Authorization", "Bearer " + player1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.verdict").value("ACCEPTED"))
                .andExpect(jsonPath("$.username").value("coder1"));
    }

    @Test
    void createSubmission_Outsider_Returns403() throws Exception {
        SubmissionRequest request = new SubmissionRequest("print('[0,1]')", "python");

        mockMvc.perform(post("/api/matches/" + activeMatch.getId() + "/submissions")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied to this match"));
    }

    @Test
    void createSubmission_CompletedMatch_Returns409() throws Exception {
        activeMatch.setStatus("COMPLETED");
        activeMatch.setWinnerId(player1.getId());
        matchRepository.save(activeMatch);

        SubmissionRequest request = new SubmissionRequest("print('[0,1]')", "python");

        mockMvc.perform(post("/api/matches/" + activeMatch.getId() + "/submissions")
                        .header("Authorization", "Bearer " + player2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Match is already completed"));
    }

    @Test
    void createSubmission_ExpiredMatch_Returns409() throws Exception {
        activeMatch.setStatus("EXPIRED");
        matchRepository.save(activeMatch);

        SubmissionRequest request = new SubmissionRequest("print('[0,1]')", "python");

        mockMvc.perform(post("/api/matches/" + activeMatch.getId() + "/submissions")
                        .header("Authorization", "Bearer " + player2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Match is already expired"));
    }

    @Test
    void getSubmissions_PlayerInMatch_Returns200WithList() throws Exception {
        SubmissionRequest request = new SubmissionRequest("print('[0,1]')", "python");

        mockMvc.perform(post("/api/matches/" + activeMatch.getId() + "/submissions")
                        .header("Authorization", "Bearer " + player1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/matches/" + activeMatch.getId() + "/submissions")
                        .header("Authorization", "Bearer " + player1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].username").value("coder1"))
                .andExpect(jsonPath("$[0].verdict").value("ACCEPTED"));
    }
}
