package com.codingarena.challenge.service;

import com.codingarena.auth.model.User;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.challenge.dto.ChallengeDto;
import com.codingarena.challenge.dto.ChallengeEvent;
import com.codingarena.challenge.dto.CreateChallengeRequest;
import com.codingarena.challenge.exception.*;
import com.codingarena.challenge.model.Challenge;
import com.codingarena.challenge.repository.ChallengeRepository;
import com.codingarena.match.model.Match;
import com.codingarena.match.model.Problem;
import com.codingarena.match.service.MatchCreationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.codingarena.match.repository.ProblemRepository problemRepository;

    @Mock
    private com.codingarena.match.repository.MatchRepository matchRepository;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    private MatchCreationService matchCreationService;
    private ChallengeService challengeService;

    private User challenger;
    private User challenged;
    private UUID challengerId;
    private UUID challengedId;

    @BeforeEach
    void setUp() {
        matchCreationService = new MatchCreationService(problemRepository, matchRepository, messagingTemplate, 30L);

        challengeService = new ChallengeService(
                challengeRepository,
                userRepository,
                matchCreationService,
                messagingTemplate
        );

        challengerId = UUID.randomUUID();
        challenger = new User("challenger", "challenger@example.com", "hash");
        challenger.setId(challengerId);
        challenger.setRating(1200);

        challengedId = UUID.randomUUID();
        challenged = new User("challenged", "challenged@example.com", "hash");
        challenged.setId(challengedId);
        challenged.setRating(1250);
    }

    @Test
    void createChallenge_Success_SavesChallengeAndBroadcastsChallengeReceived() {
        CreateChallengeRequest request = new CreateChallengeRequest("challenged");

        when(userRepository.findByUsername("challenged")).thenReturn(Optional.of(challenged));
        when(challengeRepository.existsPendingChallengeBetween(challengerId, challengedId)).thenReturn(false);

        Challenge savedChallenge = new Challenge(challenger, challenged);
        UUID challengeId = UUID.randomUUID();
        savedChallenge.setId(challengeId);
        when(challengeRepository.save(any(Challenge.class))).thenReturn(savedChallenge);

        ChallengeDto result = challengeService.createChallenge(request, challenger);

        assertNotNull(result);
        assertEquals(challengeId, result.getId());
        assertEquals("PENDING", result.getStatus());
        assertEquals(challengerId, result.getChallengerId());
        assertEquals(challengedId, result.getChallengedId());

        ArgumentCaptor<ChallengeEvent> eventCaptor = ArgumentCaptor.forClass(ChallengeEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/user/" + challengedId), eventCaptor.capture());
        assertEquals("CHALLENGE_RECEIVED", eventCaptor.getValue().getType());
    }

    @Test
    void createChallenge_TargetNotFound_ThrowsUserNotFoundException() {
        CreateChallengeRequest request = new CreateChallengeRequest("unknown");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> challengeService.createChallenge(request, challenger));
    }

    @Test
    void createChallenge_SelfChallenge_ThrowsSelfChallengeException() {
        CreateChallengeRequest request = new CreateChallengeRequest("challenger");
        when(userRepository.findByUsername("challenger")).thenReturn(Optional.of(challenger));

        assertThrows(SelfChallengeException.class,
                () -> challengeService.createChallenge(request, challenger));
    }

    @Test
    void createChallenge_DuplicatePending_ThrowsChallengeConflictException() {
        CreateChallengeRequest request = new CreateChallengeRequest("challenged");
        when(userRepository.findByUsername("challenged")).thenReturn(Optional.of(challenged));
        when(challengeRepository.existsPendingChallengeBetween(challengerId, challengedId)).thenReturn(true);

        assertThrows(ChallengeConflictException.class,
                () -> challengeService.createChallenge(request, challenger));
    }

    @Test
    void acceptChallenge_Success_CreatesMatchAndUpdatesStatusToAccepted() {
        UUID challengeId = UUID.randomUUID();
        Challenge challenge = new Challenge(challenger, challenged);
        challenge.setId(challengeId);

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

        Problem problem = new Problem("Two Sum", "EASY", "Desc");
        when(problemRepository.findRandomProblem()).thenReturn(Optional.of(problem));

        UUID matchId = UUID.randomUUID();
        Match savedMatch = new Match(challenger, challenged, problem, "IN_PROGRESS", LocalDateTime.now());
        savedMatch.setId(matchId);
        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);

        when(challengeRepository.acceptChallengeAtomically(challengeId, matchId)).thenReturn(1);

        ChallengeDto result = challengeService.acceptChallenge(challengeId, challenged);

        assertNotNull(result);
        assertEquals("ACCEPTED", result.getStatus());
        assertEquals(matchId, result.getMatchId());
        verify(matchRepository).save(any(Match.class));
        verify(challengeRepository).acceptChallengeAtomically(challengeId, matchId);
    }

    @Test
    void acceptChallenge_NotChallengedUser_ThrowsChallengeAccessDeniedException() {
        UUID challengeId = UUID.randomUUID();
        Challenge challenge = new Challenge(challenger, challenged);
        challenge.setId(challengeId);

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

        assertThrows(ChallengeAccessDeniedException.class,
                () -> challengeService.acceptChallenge(challengeId, challenger));
    }

    @Test
    void acceptChallenge_NotPendingStatus_ThrowsChallengeConflictException() {
        UUID challengeId = UUID.randomUUID();
        Challenge challenge = new Challenge(challenger, challenged);
        challenge.setId(challengeId);
        challenge.setStatus("DECLINED");

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

        assertThrows(ChallengeConflictException.class,
                () -> challengeService.acceptChallenge(challengeId, challenged));
    }

    @Test
    void declineChallenge_Success_UpdatesStatusToDeclinedAndBroadcastsEvent() {
        UUID challengeId = UUID.randomUUID();
        Challenge challenge = new Challenge(challenger, challenged);
        challenge.setId(challengeId);

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(challengeRepository.declineChallengeAtomically(challengeId)).thenReturn(1);

        ChallengeDto result = challengeService.declineChallenge(challengeId, challenged);

        assertNotNull(result);
        assertEquals("DECLINED", result.getStatus());

        ArgumentCaptor<ChallengeEvent> eventCaptor = ArgumentCaptor.forClass(ChallengeEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/user/" + challengerId), eventCaptor.capture());
        assertEquals("CHALLENGE_DECLINED", eventCaptor.getValue().getType());
    }

    @Test
    void declineChallenge_NotChallengedUser_ThrowsChallengeAccessDeniedException() {
        UUID challengeId = UUID.randomUUID();
        Challenge challenge = new Challenge(challenger, challenged);
        challenge.setId(challengeId);

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

        assertThrows(ChallengeAccessDeniedException.class,
                () -> challengeService.declineChallenge(challengeId, challenger));
    }

    @Test
    void getPendingChallenges_ReturnsPendingOnlyForCaller() {
        Challenge challenge = new Challenge(challenger, challenged);
        challenge.setId(UUID.randomUUID());

        when(challengeRepository.findByChallengedIdAndStatusOrderByCreatedAtDesc(challengedId, "PENDING"))
                .thenReturn(List.of(challenge));

        List<ChallengeDto> list = challengeService.getPendingChallenges(challenged);

        assertEquals(1, list.size());
        assertEquals(challengedId, list.get(0).getChallengedId());
    }
}
