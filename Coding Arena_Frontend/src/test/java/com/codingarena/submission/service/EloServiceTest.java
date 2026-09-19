package com.codingarena.submission.service;

import com.codingarena.auth.model.User;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.match.model.Match;
import com.codingarena.match.model.Problem;
import com.codingarena.match.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EloServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MatchRepository matchRepository;

    private EloService eloService;

    private User winner;
    private User loser;

    @BeforeEach
    void setUp() {
        eloService = new EloService(userRepository, matchRepository);

        winner = new User("winner", "winner@example.com", "hash");
        winner.setId(UUID.randomUUID());
        winner.setRating(1200);
        winner.setWins(0);
        winner.setLosses(0);

        loser = new User("loser", "loser@example.com", "hash");
        loser.setId(UUID.randomUUID());
        loser.setRating(1200);
        loser.setWins(0);
        loser.setLosses(0);
    }

    @Test
    void calculateRatings_EqualRatings_UpdatesExpectedDeltaAndIncrementsWinsLosses() {
        Problem problem = new Problem("Two Sum", "EASY", "Desc");
        Match match = new Match(winner, loser, problem, "IN_PROGRESS", LocalDateTime.now());
        match.setId(UUID.randomUUID());

        eloService.updateRatings(match, winner, loser);

        // For equal ratings (1200 vs 1200), expected score is 0.5.
        // Winner gains 32 * (1 - 0.5) = +16 -> 1216
        // Loser loses 32 * (0 - 0.5) = -16 -> 1184
        assertEquals(1216, winner.getRating());
        assertEquals(1, winner.getWins());
        assertEquals(0, winner.getLosses());

        assertEquals(1184, loser.getRating());
        assertEquals(0, loser.getWins());
        assertEquals(1, loser.getLosses());

        assertEquals(16, match.getWinnerRatingDelta());
        assertEquals(-16, match.getLoserRatingDelta());

        verify(userRepository).save(winner);
        verify(userRepository).save(loser);
        verify(matchRepository).save(match);
    }

    @Test
    void calculateRatings_HigherRatedWinner_GainsFewerPoints() {
        winner.setRating(1400);
        loser.setRating(1000);

        eloService.updateRatings(winner, loser);

        // Expected score for 1400 vs 1000 is ~0.909
        // Winner gains ~32 * (1 - 0.909) ~ 3 points
        assertTrue(winner.getRating() > 1400);
        assertTrue(winner.getRating() < 1410);
        assertTrue(loser.getRating() < 1000);
        assertTrue(loser.getRating() > 990);
        assertEquals(1, winner.getWins());
        assertEquals(1, loser.getLosses());
    }

    @Test
    void calculateNewRating_DirectHelperMethod() {
        int newWinnerRating = eloService.calculateNewRating(1200, 1200, true);
        int newLoserRating = eloService.calculateNewRating(1200, 1200, false);

        assertEquals(1216, newWinnerRating);
        assertEquals(1184, newLoserRating);
    }
}
