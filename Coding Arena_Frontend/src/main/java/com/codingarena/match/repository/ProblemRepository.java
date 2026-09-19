package com.codingarena.match.repository;

import com.codingarena.match.model.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, UUID> {

    @Query(value = "SELECT * FROM problems ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Problem> findRandomProblem();
}
