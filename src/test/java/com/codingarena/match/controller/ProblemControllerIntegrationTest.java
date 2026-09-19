package com.codingarena.match.controller;

import com.codingarena.auth.AuthApplication;
import com.codingarena.auth.dto.RegisterRequest;
import com.codingarena.auth.model.User;
import com.codingarena.auth.repository.UserPreferencesRepository;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.challenge.repository.ChallengeRepository;
import com.codingarena.common.ratelimit.RateLimitService;
import com.codingarena.config.TestRedisConfig;
import com.codingarena.friendship.repository.FriendshipRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AuthApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class ProblemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserPreferencesRepository userPreferencesRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private User user;
    private Problem seededProblem;

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

        // Register User
        RegisterRequest req = new RegisterRequest("coder1", "coder1@example.com", "pass12345");
        MvcResult res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        userToken = objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
        user = userRepository.findByEmail("coder1@example.com").orElseThrow();

        // Seed Problem
        Problem problem = new Problem("Palindrome Number", "EASY", "Check if integer is a palindrome");
        seededProblem = problemRepository.save(problem);

        // Seed Sample and Hidden Test Cases
        testCaseRepository.save(new TestCase(seededProblem, "121", "true", true));
        testCaseRepository.save(new TestCase(seededProblem, "-121", "false", true));
        testCaseRepository.save(new TestCase(seededProblem, "10", "false", false)); // Hidden case
    }

    @Test
    void getProblems_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/problems"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProblems_Authenticated_ReturnsListWithTags() throws Exception {
        mockMvc.perform(get("/api/problems")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(seededProblem.getId().toString()))
                .andExpect(jsonPath("$[0].title").value("Palindrome Number"))
                .andExpect(jsonPath("$[0].difficulty").value("EASY"))
                .andExpect(jsonPath("$[0].tags").isArray());
    }

    @Test
    void getProblemById_Success_ReturnsProblemWithSampleExamplesOnly() throws Exception {
        mockMvc.perform(get("/api/problems/" + seededProblem.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seededProblem.getId().toString()))
                .andExpect(jsonPath("$.title").value("Palindrome Number"))
                .andExpect(jsonPath("$.difficulty").value("EASY"))
                .andExpect(jsonPath("$.description").value("Check if integer is a palindrome"))
                .andExpect(jsonPath("$.tags").isArray())
                .andExpect(jsonPath("$.examples.length()").value(2))
                .andExpect(jsonPath("$.examples[0].input").value("121"))
                .andExpect(jsonPath("$.examples[0].output").value("true"))
                .andExpect(jsonPath("$.examples[1].input").value("-121"))
                .andExpect(jsonPath("$.examples[1].output").value("false"));
    }

    @Test
    void getProblemById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/problems/" + java.util.UUID.randomUUID())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPracticeSubmission_Success_DoesNotAlterRatingOrMatches() throws Exception {
        SubmissionRequest request = new SubmissionRequest("print(True)", "python");

        mockMvc.perform(post("/api/problems/" + seededProblem.getId() + "/submissions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.matchId").isEmpty())
                .andExpect(jsonPath("$.problemId").value(seededProblem.getId().toString()))
                .andExpect(jsonPath("$.verdict").isNotEmpty());

        // Check user ratings and wins/losses did NOT change
        User refreshedUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(0, refreshedUser.getRating());
        assertEquals(0, refreshedUser.getWins());
        assertEquals(0, refreshedUser.getLosses());

        // Check no match was created
        assertEquals(0, matchRepository.count());

        // Check practice submissions history endpoint
        mockMvc.perform(get("/api/problems/" + seededProblem.getId() + "/submissions/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].problemId").value(seededProblem.getId().toString()))
                .andExpect(jsonPath("$[0].matchId").isEmpty());
    }
}
