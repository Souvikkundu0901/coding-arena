package com.codingarena.auth.controller;

import com.codingarena.auth.AuthApplication;
import com.codingarena.auth.dto.*;
import com.codingarena.auth.model.User;
import com.codingarena.auth.repository.UserPreferencesRepository;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.challenge.repository.ChallengeRepository;
import com.codingarena.common.ratelimit.RateLimitService;
import com.codingarena.config.TestRedisConfig;
import com.codingarena.friendship.repository.FriendshipRepository;
import com.codingarena.match.model.Match;
import com.codingarena.match.model.Problem;
import com.codingarena.match.repository.MatchRepository;
import com.codingarena.match.repository.ProblemRepository;
import com.codingarena.match.repository.TestCaseRepository;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AuthApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class UserControllerIntegrationTest {

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

    private String userAToken;
    private User userA;
    private User userB;
    private User userC;
    private Problem problem;

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

        // Register User A
        RegisterRequest reqA = new RegisterRequest("usera", "usera@example.com", "pass12345");
        MvcResult resA = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(status().isCreated())
                .andReturn();
        userAToken = objectMapper.readTree(resA.getResponse().getContentAsString()).get("token").asText();
        userA = userRepository.findByEmail("usera@example.com").orElseThrow();

        // Register User B
        RegisterRequest reqB = new RegisterRequest("userb", "userb@example.com", "pass12345");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isCreated());
        userB = userRepository.findByEmail("userb@example.com").orElseThrow();

        // Register User C
        RegisterRequest reqC = new RegisterRequest("userc", "userc@example.com", "pass12345");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqC)))
                .andExpect(status().isCreated());
        userC = userRepository.findByEmail("userc@example.com").orElseThrow();

        problem = problemRepository.save(new Problem("Two Sum", "EASY", "Desc"));
    }

    @Test
    void getMyMatches_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/me/matches"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyMatches_ReturnsCompletedAndExpiredMatchesWithAccurateDeltas() throws Exception {
        LocalDateTime baseTime = LocalDateTime.now().minusHours(2);

        // Match 1: User A wins against User B
        Match match1 = new Match(userA, userB, problem, "COMPLETED", baseTime, baseTime.plusMinutes(15));
        match1.setWinnerId(userA.getId());
        match1.setEndedAt(baseTime.plusMinutes(5));
        match1.setWinnerRatingDelta(16);
        match1.setLoserRatingDelta(-16);
        matchRepository.save(match1);

        // Match 2: User A loses against User C
        Match match2 = new Match(userA, userC, problem, "COMPLETED", baseTime.plusMinutes(20), baseTime.plusMinutes(35));
        match2.setWinnerId(userC.getId());
        match2.setEndedAt(baseTime.plusMinutes(25));
        match2.setWinnerRatingDelta(18);
        match2.setLoserRatingDelta(-18);
        matchRepository.save(match2);

        // Match 3: User A vs User B expired
        Match match3 = new Match(userA, userB, problem, "EXPIRED", baseTime.plusMinutes(40), baseTime.plusMinutes(55));
        match3.setWinnerId(null);
        match3.setEndedAt(baseTime.plusMinutes(55));
        matchRepository.save(match3);

        mockMvc.perform(get("/api/users/me/matches")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                // Most recent: Match 3 (EXPIRED)
                .andExpect(jsonPath("$[0].id").value(match3.getId().toString()))
                .andExpect(jsonPath("$[0].opponentUsername").value("userb"))
                .andExpect(jsonPath("$[0].result").value("EXPIRED"))
                .andExpect(jsonPath("$[0].ratingDelta").value(0))
                // Middle: Match 2 (LOSS)
                .andExpect(jsonPath("$[1].id").value(match2.getId().toString()))
                .andExpect(jsonPath("$[1].opponentUsername").value("userc"))
                .andExpect(jsonPath("$[1].result").value("LOSS"))
                .andExpect(jsonPath("$[1].ratingDelta").value(-18))
                // Oldest: Match 1 (WIN)
                .andExpect(jsonPath("$[2].id").value(match1.getId().toString()))
                .andExpect(jsonPath("$[2].opponentUsername").value("userb"))
                .andExpect(jsonPath("$[2].result").value("WIN"))
                .andExpect(jsonPath("$[2].ratingDelta").value(16));
    }

    @Test
    void getMyMatches_WithLimit_ReturnsLimitedMatches() throws Exception {
        LocalDateTime baseTime = LocalDateTime.now().minusHours(2);

        Match match1 = new Match(userA, userB, problem, "COMPLETED", baseTime, baseTime.plusMinutes(15));
        match1.setWinnerId(userA.getId());
        match1.setEndedAt(baseTime.plusMinutes(5));
        match1.setWinnerRatingDelta(16);
        match1.setLoserRatingDelta(-16);
        matchRepository.save(match1);

        Match match2 = new Match(userA, userC, problem, "COMPLETED", baseTime.plusMinutes(20), baseTime.plusMinutes(35));
        match2.setWinnerId(userC.getId());
        match2.setEndedAt(baseTime.plusMinutes(25));
        match2.setWinnerRatingDelta(18);
        match2.setLoserRatingDelta(-18);
        matchRepository.save(match2);

        mockMvc.perform(get("/api/users/me/matches?limit=1")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(match2.getId().toString()));
    }

    @Test
    void updateUsername_SuccessAndConflict() throws Exception {
        // Successful username update
        UpdateUsernameRequest req = new UpdateUsernameRequest("usera_new");
        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("usera_new"));

        // Conflict when taken by User B
        UpdateUsernameRequest conflictReq = new UpdateUsernameRequest("userb");
        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username is already taken"));
    }

    @Test
    void updateEmail_SuccessAndBadPassword() throws Exception {
        // Bad password
        UpdateEmailRequest badPwdReq = new UpdateEmailRequest("new_usera@example.com", "wrongpass");
        mockMvc.perform(patch("/api/users/me/email")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badPwdReq)))
                .andExpect(status().isUnauthorized());

        // Successful email update
        UpdateEmailRequest validReq = new UpdateEmailRequest("new_usera@example.com", "pass12345");
        mockMvc.perform(patch("/api/users/me/email")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new_usera@example.com"));

        // Conflict when taking User B's email
        UpdateEmailRequest conflictReq = new UpdateEmailRequest("userb@example.com", "pass12345");
        mockMvc.perform(patch("/api/users/me/email")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictReq)))
                .andExpect(status().isConflict());
    }

    @Test
    void updatePassword_SuccessAndBadCurrentPassword() throws Exception {
        // Bad current password
        UpdatePasswordRequest badPwdReq = new UpdatePasswordRequest("wrongpass", "brand_new_pass");
        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badPwdReq)))
                .andExpect(status().isUnauthorized());

        // Successful password update
        UpdatePasswordRequest validReq = new UpdatePasswordRequest("pass12345", "brand_new_pass");
        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk());

        // Verify login with new password works
        LoginRequest loginReq = new LoginRequest("usera@example.com", "brand_new_pass");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk());
    }

    @Test
    void preferences_GetAndPatch() throws Exception {
        // Get preferences (auto-creates dark theme default)
        mockMvc.perform(get("/api/users/me/preferences")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("dark"))
                .andExpect(jsonPath("$.emailNotifications").value(true));

        // Patch preferences
        UpdatePreferencesRequest updateReq = new UpdatePreferencesRequest("light", false);
        mockMvc.perform(patch("/api/users/me/preferences")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("light"))
                .andExpect(jsonPath("$.emailNotifications").value(false));
    }

    @Test
    void deleteAccount_SuccessAndBlocksSubsequentLogin() throws Exception {
        // Bad password rejected
        DeleteAccountRequest badReq = new DeleteAccountRequest("wrongpassword");
        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isUnauthorized());

        // Successful account deletion
        DeleteAccountRequest validReq = new DeleteAccountRequest("pass12345");
        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account deleted successfully"));

        User deletedUser = userRepository.findByEmail("usera@example.com").orElseThrow();
        assertTrue(deletedUser.getIsDeleted());

        // Login fails for deleted account
        LoginRequest loginReq = new LoginRequest("usera@example.com", "pass12345");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());
    }
}
