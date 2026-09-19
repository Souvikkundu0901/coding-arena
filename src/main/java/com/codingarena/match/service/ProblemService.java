package com.codingarena.match.service;

import com.codingarena.match.dto.ProblemDto;
import com.codingarena.match.dto.ProblemExampleDto;
import com.codingarena.match.exception.ProblemNotFoundException;
import com.codingarena.match.model.Problem;
import com.codingarena.match.repository.ProblemRepository;
import com.codingarena.match.repository.TestCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;

    public ProblemService(ProblemRepository problemRepository, TestCaseRepository testCaseRepository) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
    }

    @Transactional(readOnly = true)
    public List<ProblemDto> getAllProblems() {
        return problemRepository.findAll().stream()
                .map(p -> new ProblemDto(p.getId(), p.getTitle(), p.getDifficulty(), null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProblemDto getProblemById(UUID problemId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException("Problem not found: " + problemId));

        List<ProblemExampleDto> examples = testCaseRepository.findByProblemIdAndIsSampleTrue(problemId).stream()
                .limit(3)
                .map(tc -> new ProblemExampleDto(tc.getInput(), tc.getExpectedOutput()))
                .collect(Collectors.toList());

        return ProblemDto.fromEntity(problem, examples);
    }
}
