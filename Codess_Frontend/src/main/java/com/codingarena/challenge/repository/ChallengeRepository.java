package com.codingarena.challenge.repository;

import com.codingarena.challenge.model.Challenge;
import com.codingarena.match.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {

    @Query("SELECT COUNT(c) > 0 FROM Challenge c WHERE c.status = 'PENDING' AND ((c.challenger.id = :user1Id AND c.challenged.id = :user2Id) OR (c.challenger.id = :user2Id AND c.challenged.id = :user1Id))")
    boolean existsPendingChallengeBetween(@Param("user1Id") UUID user1Id, @Param("user2Id") UUID user2Id);

    List<Challenge> findByChallengedIdAndStatusOrderByCreatedAtDesc(UUID challengedId, String status);

    @Modifying
    @Query(value = "UPDATE challenges SET status = 'ACCEPTED', match_id = :matchId, responded_at = CURRENT_TIMESTAMP WHERE id = :challengeId AND status = 'PENDING'", nativeQuery = true)
    int acceptChallengeAtomically(@Param("challengeId") UUID challengeId, @Param("matchId") UUID matchId);

    @Modifying
    @Query(value = "UPDATE challenges SET status = 'DECLINED', responded_at = CURRENT_TIMESTAMP WHERE id = :challengeId AND status = 'PENDING'", nativeQuery = true)
    int declineChallengeAtomically(@Param("challengeId") UUID challengeId);

    @Query("SELECT c FROM Challenge c WHERE c.status = 'PENDING' AND c.createdAt < :cutoff")
    List<Challenge> findExpiredPendingChallenges(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query(value = "UPDATE challenges SET status = 'EXPIRED', responded_at = CURRENT_TIMESTAMP WHERE id = :challengeId AND status = 'PENDING'", nativeQuery = true)
    int expireChallengeAtomically(@Param("challengeId") UUID challengeId);
}
