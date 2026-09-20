package com.codingarena.match.service;

import com.codingarena.challenge.model.Challenge;
import com.codingarena.challenge.repository.ChallengeRepository;
import com.codingarena.match.dto.MatchDto;
import com.codingarena.match.dto.MatchEvent;
import com.codingarena.match.model.Match;
import com.codingarena.match.repository.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MatchExpiryService {

    private static final Logger log = LoggerFactory.getLogger(MatchExpiryService.class);

    private final MatchRepository matchRepository;
    private final ChallengeRepository challengeRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    public MatchExpiryService(MatchRepository matchRepository,
                              ChallengeRepository challengeRepository,
                              SimpMessageSendingOperations messagingTemplate) {
        this.matchRepository = matchRepository;
        this.challengeRepository = challengeRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public MatchExpiryService(MatchRepository matchRepository,
                              SimpMessageSendingOperations messagingTemplate) {
        this(matchRepository, null, messagingTemplate);
    }

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void expireMatches() {
        try {
            List<Match> expiredMatches = matchRepository.findExpiredMatches();

            for (Match match : expiredMatches) {
                int updatedRows = matchRepository.expireMatchAtomically(match.getId());

                if (updatedRows == 1) {
                    match.setStatus("EXPIRED");
                    match.setEndedAt(LocalDateTime.now());

                    MatchDto matchDto = MatchDto.fromEntity(match);
                    MatchEvent endEvent = new MatchEvent("MATCH_END", matchDto);
                    messagingTemplate.convertAndSend("/topic/match/" + match.getId(), endEvent);

                    log.info("Match {} reached time limit and transitioned to EXPIRED", match.getId());
                } else {
                    log.debug("Match {} was already completed with a winner, skipping expiry", match.getId());
                }
            }

            if (challengeRepository != null) {
                expireChallenges();
            }
        } catch (Exception e) {
            log.error("Error running match expiry scheduler", e);
        }
    }

    private void expireChallenges() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        List<Challenge> expiredChallenges = challengeRepository.findExpiredPendingChallenges(cutoff);

        for (Challenge challenge : expiredChallenges) {
            int updatedRows = challengeRepository.expireChallengeAtomically(challenge.getId());
            if (updatedRows == 1) {
                log.info("Pending challenge {} older than 5 minutes transitioned to EXPIRED", challenge.getId());
            }
        }
    }
}
