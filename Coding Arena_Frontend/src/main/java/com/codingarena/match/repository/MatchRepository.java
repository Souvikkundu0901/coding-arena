package com.codingarena.match.repository;

import com.codingarena.match.model.Match;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    /**
     * Atomic conditional update to set winner:
     * UPDATE matches SET winner_id = ?, status = 'COMPLETED', ended_at = now() WHERE id = ? AND winner_id IS NULL
     * Returns 1 if winner was successfully set, 0 if match already had a winner.
     */
    @Modifying
    @Query("UPDATE Match m SET m.winnerId = :winnerId, m.status = 'COMPLETED', m.endedAt = CURRENT_TIMESTAMP WHERE m.id = :matchId AND m.winnerId IS NULL")
    int setWinnerAtomically(@Param("matchId") UUID matchId, @Param("winnerId") UUID winnerId);

    /**
     * Finds all matches that are currently IN_PROGRESS and have passed their expires_at timestamp.
     */
    @Query("SELECT m FROM Match m WHERE m.status = 'IN_PROGRESS' AND m.expiresAt < CURRENT_TIMESTAMP")
    List<Match> findExpiredMatches();

    /**
     * Atomic conditional update to expire a match:
     * UPDATE matches SET status = 'EXPIRED', ended_at = now() WHERE id = ? AND status = 'IN_PROGRESS' AND winner_id IS NULL
     * Returns 1 if match was successfully expired, 0 if match was already completed with a winner.
     */
    @Modifying
    @Query("UPDATE Match m SET m.status = 'EXPIRED', m.endedAt = CURRENT_TIMESTAMP WHERE m.id = :matchId AND m.status = 'IN_PROGRESS' AND m.winnerId IS NULL")
    int expireMatchAtomically(@Param("matchId") UUID matchId);

    /**
     * Finds completed and expired matches for a specific user, ordered most recent first.
     */
    @Query("SELECT m FROM Match m WHERE (m.playerA.id = :userId OR m.playerB.id = :userId) AND m.status IN ('COMPLETED', 'EXPIRED') ORDER BY coalesce(m.endedAt, m.startedAt) DESC, m.startedAt DESC")
    List<Match> findCompletedOrExpiredMatchesByUserId(@Param("userId") UUID userId, Pageable pageable);
}
