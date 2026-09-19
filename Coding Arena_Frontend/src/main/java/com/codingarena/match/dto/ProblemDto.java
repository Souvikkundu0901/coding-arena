package com.codingarena.match.dto;

import com.codingarena.match.model.Problem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProblemDto {

    private UUID id;
    private String title;
    private String difficulty;
    private String description;
    private List<String> tags = new ArrayList<>();
    private List<ProblemExampleDto> examples = new ArrayList<>();

    public ProblemDto() {
    }

    public ProblemDto(UUID id, String title, String difficulty, String description) {
        this.id = id;
        this.title = title;
        this.difficulty = difficulty;
        this.description = description;
        this.tags = new ArrayList<>();
        this.examples = new ArrayList<>();
    }

    public ProblemDto(UUID id, String title, String difficulty, String description, List<ProblemExampleDto> examples) {
        this.id = id;
        this.title = title;
        this.difficulty = difficulty;
        this.description = description;
        this.tags = new ArrayList<>();
        this.examples = examples != null ? examples : new ArrayList<>();
    }

    public static ProblemDto fromEntity(Problem problem) {
        if (problem == null) return null;
        return new ProblemDto(
                problem.getId(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getDescription()
        );
    }

    public static ProblemDto fromEntity(Problem problem, List<ProblemExampleDto> examples) {
        if (problem == null) return null;
        return new ProblemDto(
                problem.getId(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getDescription(),
                examples
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public List<ProblemExampleDto> getExamples() {
        return examples;
    }

    public void setExamples(List<ProblemExampleDto> examples) {
        this.examples = examples != null ? examples : new ArrayList<>();
    }
}
