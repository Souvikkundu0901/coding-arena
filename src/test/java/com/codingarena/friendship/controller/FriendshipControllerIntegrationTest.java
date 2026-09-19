package com.codingarena.friendship.controller;

import com.codingarena.auth.AuthApplication;
import com.codingarena.auth.dto.RegisterRequest;
import com.codingarena.auth.repository.UserPreferencesRepository;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.challenge.repository.ChallengeRepository;
import com.codingarena.common.ratelimit.RateLimitService;
import com.codingarena.config.TestRedisConfig;
import com.codingarena.friendship.dto.SendFriendRequest;
import com.codingarena.friendship.repository.FriendshipRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AuthApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class FriendshipControllerIntegrationTest {

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
    private String userBToken;
    private String userCToken;

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
    void sendFriendRequest_Success_Returns201AndPendingRequest() throws Exception {
        SendFriendRequest request = new SendFriendRequest("userb");

        mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.requesterUsername").value("usera"))
                .andExpect(jsonPath("$.addresseeUsername").value("userb"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void sendFriendRequest_Self_Returns400() throws Exception {
        SendFriendRequest request = new SendFriendRequest("usera");

        mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot send a friend request to yourself"));
    }

    @Test
    void sendFriendRequest_UserNotFound_Returns404() throws Exception {
        SendFriendRequest request = new SendFriendRequest("nonexistent");

        mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found: nonexistent"));
    }

    @Test
    void sendFriendRequest_DuplicatePendingOrAccepted_Returns409() throws Exception {
        SendFriendRequest request = new SendFriendRequest("userb");

        mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate from A to B returns 409
        mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // Reverse from B to A returns 409
        SendFriendRequest reverseRequest = new SendFriendRequest("usera");
        mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reverseRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void getPendingIncomingRequests_ReturnsPendingOnlyForCaller() throws Exception {
        SendFriendRequest request = new SendFriendRequest("userb");
        mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // User B has 1 pending incoming request
        mockMvc.perform(get("/api/friends/requests/pending")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].requesterUsername").value("usera"));

        // User A has 0 incoming requests
        mockMvc.perform(get("/api/friends/requests/pending")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void acceptFriendRequest_Success_UpdatesStatusAndAddsToFriendsList() throws Exception {
        SendFriendRequest request = new SendFriendRequest("userb");
        MvcResult createResult = mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String requestId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // User C cannot accept (403 Forbidden)
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + userCToken))
                .andExpect(status().isForbidden());

        // User B accepts (200 OK)
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Accepting again returns 409
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isConflict());

        // User A's friends list includes User B
        mockMvc.perform(get("/api/friends")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("userb"));

        // User B's friends list includes User A
        mockMvc.perform(get("/api/friends")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("usera"));
    }

    @Test
    void declineFriendRequest_Success() throws Exception {
        SendFriendRequest request = new SendFriendRequest("userb");
        MvcResult createResult = mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String requestId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // User B declines (200 OK)
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/decline")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));

        // Declining again returns 409
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/decline")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isConflict());

        // User A has 0 friends
        mockMvc.perform(get("/api/friends")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
