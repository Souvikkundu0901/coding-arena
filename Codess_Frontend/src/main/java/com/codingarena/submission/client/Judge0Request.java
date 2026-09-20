package com.codingarena.submission.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Judge0Request {

    @JsonProperty("source_code")
    private String sourceCode;

    @JsonProperty("language_id")
    private Integer languageId;

    @JsonProperty("stdin")
    private String stdin;

    @JsonProperty("expected_output")
    private String expectedOutput;

    @JsonProperty("cpu_time_limit")
    private Double cpuTimeLimit = 5.0;

    @JsonProperty("memory_limit")
    private Integer memoryLimit = 256000;

    public Judge0Request() {
    }

    public Judge0Request(String sourceCode, Integer languageId, String stdin, String expectedOutput) {
        this.sourceCode = sourceCode;
        this.languageId = languageId;
        this.stdin = stdin;
        this.expectedOutput = expectedOutput;
        this.cpuTimeLimit = 5.0;
        this.memoryLimit = 256000;
    }

    @JsonProperty("source_code")
    public String getSourceCode() {
        return sourceCode;
    }

    @JsonProperty("source_code")
    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    @JsonProperty("language_id")
    public Integer getLanguageId() {
        return languageId;
    }

    @JsonProperty("language_id")
    public void setLanguageId(Integer languageId) {
        this.languageId = languageId;
    }

    @JsonProperty("stdin")
    public String getStdin() {
        return stdin;
    }

    @JsonProperty("stdin")
    public void setStdin(String stdin) {
        this.stdin = stdin;
    }

    @JsonProperty("expected_output")
    public String getExpectedOutput() {
        return expectedOutput;
    }

    @JsonProperty("expected_output")
    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    @JsonProperty("cpu_time_limit")
    public Double getCpuTimeLimit() {
        return cpuTimeLimit;
    }

    @JsonProperty("cpu_time_limit")
    public void setCpuTimeLimit(Double cpuTimeLimit) {
        this.cpuTimeLimit = cpuTimeLimit;
    }

    @JsonProperty("memory_limit")
    public Integer getMemoryLimit() {
        return memoryLimit;
    }

    @JsonProperty("memory_limit")
    public void setMemoryLimit(Integer memoryLimit) {
        this.memoryLimit = memoryLimit;
    }
}
