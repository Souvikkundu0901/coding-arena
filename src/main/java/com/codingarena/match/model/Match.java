package com.codingarena.match.model;

import com.codingarena.auth.model.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_a_id", nullable = false)
    private User playerA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_b_id", nullable = false)
    private User playerB;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    /**
     * Note: winner_id must ONLY be updated via atomic conditional query in MatchRepository:
     * UPDATE matches SET winner_id = ?, status = 'COMPLETED', ended_at = now() WHERE id = ? AND winner_id IS NULL
     */
    @Column(name = "winner_id")
    private UUID winnerId;

    @Column(name = "winner_rating_delta")
    private Integer winnerRatingDelta = 0;

    @Column(name = "loser_rating_delta")
    private Integer loserRatingDelta = 0;

    @Column(name = "status", nullable = false)
    private String status; // WAITING / IN_PROGRESS / COMPLETED

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public Match() {
    }

    public Match(User playerA, User playerB, Problem problem, String status, LocalDateTime startedAt) {
        this.playerA = playerA;
        this.playerB = playerB;
        this.problem = problem;
        this.status = status;
        this.startedAt = startedAt;
    }

    public Match(User playerA, User playerB, Problem problem, String status, LocalDateTime startedAt, LocalDateTime expiresAt) {
        this.playerA = playerA;
        this.playerB = playerB;
        this.problem = problem;
        this.status = status;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getPlayerA() {
        return playerA;
    }

    public void setPlayerA(User playerA) {
        this.playerA = playerA;
    }

    public User getPlayerB() {
        return playerB;
    }

    public void setPlayerB(User playerB) {
        this.playerB = playerB;
    }

    public Problem getProblem() {
        return problem;
    }

    public void setProblem(Problem problem) {
        this.problem = problem;
    }

    public UUID getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(UUID winnerId) {
        this.winnerId = winnerId;
    }

    public Integer getWinnerRatingDelta() {
        return winnerRatingDelta;
    }

    public void setWinnerRatingDelta(Integer winnerRatingDelta) {
        this.winnerRatingDelta = winnerRatingDelta;
    }

    public Integer getLoserRatingDelta() {
        return loserRatingDelta;
    }

    public void setLoserRatingDelta(Integer loserRatingDelta) {
        this.loserRatingDelta = loserRatingDelta;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }
}
