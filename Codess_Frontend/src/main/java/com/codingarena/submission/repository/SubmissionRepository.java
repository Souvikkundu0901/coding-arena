package com.codingarena.submission.repository;

import com.codingarena.submission.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByMatchIdOrderBySubmittedAtAsc(UUID matchId);

    List<Submission> findByProblemIdAndUserIdAndMatchIsNullOrderBySubmittedAtDesc(UUID problemId, UUID userId);
}
