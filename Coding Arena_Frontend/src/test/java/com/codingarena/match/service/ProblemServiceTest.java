package com.codingarena.match.service;

import com.codingarena.match.dto.ProblemDto;
import com.codingarena.match.exception.ProblemNotFoundException;
import com.codingarena.match.model.Problem;
import com.codingarena.match.model.TestCase;
import com.codingarena.match.repository.ProblemRepository;
import com.codingarena.match.repository.TestCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    private ProblemService problemService;

    @BeforeEach
    void setUp() {
        problemService = new ProblemService(problemRepository, testCaseRepository);
    }

    @Test
    void getAllProblems_ReturnsListOfProblemDtosWithTags() {
        Problem p1 = new Problem("Two Sum", "EASY", "Desc 1");
        p1.setId(UUID.randomUUID());
        Problem p2 = new Problem("Reverse String", "MEDIUM", "Desc 2");
        p2.setId(UUID.randomUUID());

        when(problemRepository.findAll()).thenReturn(List.of(p1, p2));

        List<ProblemDto> result = problemService.getAllProblems();

        assertEquals(2, result.size());
        assertEquals("Two Sum", result.get(0).getTitle());
        assertEquals("EASY", result.get(0).getDifficulty());
        assertNotNull(result.get(0).getTags());
        assertTrue(result.get(0).getTags().isEmpty());

        assertEquals("Reverse String", result.get(1).getTitle());
        assertEquals("MEDIUM", result.get(1).getDifficulty());
    }

    @Test
    void getProblemById_Success_ReturnsProblemWithSampleExamples() {
        UUID problemId = UUID.randomUUID();
        Problem problem = new Problem("Two Sum", "EASY", "Description text");
        problem.setId(problemId);

        TestCase sample1 = new TestCase(problem, "[2,7,11,15], 9", "[0,1]", true);
        TestCase sample2 = new TestCase(problem, "[3,2,4], 6", "[1,2]", true);

        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(testCaseRepository.findByProblemIdAndIsSampleTrue(problemId)).thenReturn(List.of(sample1, sample2));

        ProblemDto result = problemService.getProblemById(problemId);

        assertNotNull(result);
        assertEquals(problemId, result.getId());
        assertEquals("Two Sum", result.getTitle());
        assertEquals("EASY", result.getDifficulty());
        assertEquals("Description text", result.getDescription());
        assertNotNull(result.getTags());
        assertEquals(2, result.getExamples().size());
        assertEquals("[2,7,11,15], 9", result.getExamples().get(0).getInput());
        assertEquals("[0,1]", result.getExamples().get(0).getOutput());
    }

    @Test
    void getProblemById_NotFound_ThrowsProblemNotFoundException() {
        UUID problemId = UUID.randomUUID();
        when(problemRepository.findById(problemId)).thenReturn(Optional.empty());

        assertThrows(ProblemNotFoundException.class, () -> problemService.getProblemById(problemId));
    }
}
