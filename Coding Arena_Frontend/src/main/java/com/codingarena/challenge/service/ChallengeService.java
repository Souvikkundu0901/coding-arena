package com.codingarena.challenge.service;

import com.codingarena.auth.model.User;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.challenge.dto.ChallengeDto;
import com.codingarena.challenge.dto.ChallengeEvent;
import com.codingarena.challenge.dto.CreateChallengeRequest;
import com.codingarena.challenge.exception.ChallengeAccessDeniedException;
import com.codingarena.challenge.exception.ChallengeConflictException;
import com.codingarena.challenge.exception.ChallengeNotFoundException;
import com.codingarena.challenge.exception.SelfChallengeException;
import com.codingarena.challenge.exception.UserNotFoundException;
import com.codingarena.challenge.model.Challenge;
import com.codingarena.challenge.repository.ChallengeRepository;
import com.codingarena.match.model.Match;
import com.codingarena.match.service.MatchCreationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChallengeService {

    private static final Logger log = LoggerFactory.getLogger(ChallengeService.class);

    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final MatchCreationService matchCreationService;
    private final SimpMessageSendingOperations messagingTemplate;

    public ChallengeService(ChallengeRepository challengeRepository,
                            UserRepository userRepository,
                            MatchCreationService matchCreationService,
                            SimpMessageSendingOperations messagingTemplate) {
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.matchCreationService = matchCreationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ChallengeDto createChallenge(CreateChallengeRequest request, User challenger) {
        String targetUsername = request.getUsername() != null ? request.getUsername().trim() : "";

        User challenged = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + targetUsername));

        if (Boolean.TRUE.equals(challenged.getIsDeleted())) {
            throw new UserNotFoundException("User not found: " + targetUsername);
        }

        if (challenger.getId().equals(challenged.getId())) {
            throw new SelfChallengeException("Cannot challenge yourself");
        }

        if (challengeRepository.existsPendingChallengeBetween(challenger.getId(), challenged.getId())) {
            throw new ChallengeConflictException("A pending challenge already exists between these users");
        }

        Challenge challenge = new Challenge(challenger, challenged);
        Challenge savedChallenge = challengeRepository.save(challenge);

        ChallengeDto dto = ChallengeDto.fromEntity(savedChallenge);
        ChallengeEvent event = new ChallengeEvent("CHALLENGE_RECEIVED", dto);

        messagingTemplate.convertAndSend("/topic/user/" + challenged.getId(), event);
        log.info("User {} challenged user {} (challengeId: {})", challenger.getUsername(), challenged.getUsername(), savedChallenge.getId());

        return dto;
    }

    @Transactional
    public ChallengeDto acceptChallenge(UUID challengeId, User currentUser) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException("Challenge not found: " + challengeId));

        if (!challenge.getChallenged().getId().equals(currentUser.getId())) {
            throw new ChallengeAccessDeniedException("Only the challenged user can accept this challenge");
        }

        if (!"PENDING".equalsIgnoreCase(challenge.getStatus())) {
            throw new ChallengeConflictException("Challenge is not in PENDING status");
        }

        Match match = matchCreationService.createMatchAndNotify(challenge.getChallenger(), challenge.getChallenged());

        int updatedRows = challengeRepository.acceptChallengeAtomically(challengeId, match.getId());
        if (updatedRows == 0) {
            throw new ChallengeConflictException("Challenge could not be accepted because it is no longer pending");
        }

        challenge.setStatus("ACCEPTED");
        challenge.setMatch(match);
        challenge.setRespondedAt(LocalDateTime.now());

        ChallengeDto dto = ChallengeDto.fromEntity(challenge);
        ChallengeEvent acceptedEvent = new ChallengeEvent("CHALLENGE_ACCEPTED", dto);
        messagingTemplate.convertAndSend("/topic/user/" + challenge.getChallenger().getId(), acceptedEvent);
        messagingTemplate.convertAndSend("/topic/user/" + challenge.getChallenged().getId(), acceptedEvent);

        log.info("Challenge {} accepted by user {}. Match {} created.", challengeId, currentUser.getUsername(), match.getId());
        return dto;
    }

    @Transactional
    public ChallengeDto declineChallenge(UUID challengeId, User currentUser) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException("Challenge not found: " + challengeId));

        if (!challenge.getChallenged().getId().equals(currentUser.getId())) {
            throw new ChallengeAccessDeniedException("Only the challenged user can decline this challenge");
        }

        if (!"PENDING".equalsIgnoreCase(challenge.getStatus())) {
            throw new ChallengeConflictException("Challenge is not in PENDING status");
        }

        int updatedRows = challengeRepository.declineChallengeAtomically(challengeId);
        if (updatedRows == 0) {
            throw new ChallengeConflictException("Challenge could not be declined because it is no longer pending");
        }

        challenge.setStatus("DECLINED");
        challenge.setRespondedAt(LocalDateTime.now());

        ChallengeDto dto = ChallengeDto.fromEntity(challenge);
        ChallengeEvent event = new ChallengeEvent("CHALLENGE_DECLINED", dto);

        messagingTemplate.convertAndSend("/topic/user/" + challenge.getChallenger().getId(), event);
        log.info("Challenge {} declined by user {}", challengeId, currentUser.getUsername());

        return dto;
    }

    @Transactional(readOnly = true)
    public List<ChallengeDto> getPendingChallenges(User currentUser) {
        return challengeRepository.findByChallengedIdAndStatusOrderByCreatedAtDesc(currentUser.getId(), "PENDING")
                .stream()
                .map(ChallengeDto::fromEntity)
                .collect(Collectors.toList());
    }
}
