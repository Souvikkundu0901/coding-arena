package com.codingarena.submission.service;

import com.codingarena.auth.model.User;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.match.model.Match;
import com.codingarena.match.repository.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EloService {

    private static final Logger log = LoggerFactory.getLogger(EloService.class);
    private static final int K_FACTOR = 32;

    private final UserRepository userRepository;
    private final MatchRepository matchRepository;

    @Autowired
    public EloService(UserRepository userRepository, MatchRepository matchRepository) {
        this.userRepository = userRepository;
        this.matchRepository = matchRepository;
    }

    public EloService(UserRepository userRepository) {
        this(userRepository, null);
    }

    @Transactional
    public void updateRatings(Match match, User winner, User loser) {
        int winnerRating = winner.getRating() != null ? winner.getRating() : 0;
        int loserRating = loser.getRating() != null ? loser.getRating() : 0;

        double expectedWinner = 1.0 / (1.0 + Math.pow(10.0, (loserRating - winnerRating) / 400.0));
        double expectedLoser = 1.0 / (1.0 + Math.pow(10.0, (winnerRating - loserRating) / 400.0));

        int newWinnerRating = (int) Math.round(winnerRating + K_FACTOR * (1.0 - expectedWinner));
        int newLoserRating = (int) Math.round(loserRating + K_FACTOR * (0.0 - expectedLoser));

        int winnerDelta = newWinnerRating - winnerRating;
        int loserDelta = newLoserRating - loserRating;

        winner.setRating(newWinnerRating);
        winner.setWins((winner.getWins() != null ? winner.getWins() : 0) + 1);

        loser.setRating(newLoserRating);
        loser.setLosses((loser.getLosses() != null ? loser.getLosses() : 0) + 1);

        userRepository.save(winner);
        userRepository.save(loser);

        if (match != null) {
            match.setWinnerRatingDelta(winnerDelta);
            match.setLoserRatingDelta(loserDelta);
            if (matchRepository != null) {
                matchRepository.save(match);
            }
        }

        log.info("Updated ratings for match: Winner {} ({} -> {}, delta: +{}), Loser {} ({} -> {}, delta: {})",
                winner.getUsername(), winnerRating, newWinnerRating, winnerDelta,
                loser.getUsername(), loserRating, newLoserRating, loserDelta);
    }

    @Transactional
    public void updateRatings(User winner, User loser) {
        updateRatings(null, winner, loser);
    }

    public int calculateNewRating(int currentRating, int opponentRating, boolean won) {
        double actualScore = won ? 1.0 : 0.0;
        double expectedScore = 1.0 / (1.0 + Math.pow(10.0, (opponentRating - currentRating) / 400.0));
        return (int) Math.round(currentRating + K_FACTOR * (actualScore - expectedScore));
    }
}
