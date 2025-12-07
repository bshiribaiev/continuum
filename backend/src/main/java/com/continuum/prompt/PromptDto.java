package com.continuum.prompt;

import jakarta.validation.constraints.NotBlank;

public class PromptDto {

    // Request model for generating a prompt with context
    public static class GeneratePromptRequest {
        @NotBlank
        public String userId;

        // Optional workspace/project to scope the context search
        public String workspaceId;

        @NotBlank
        public String task;

        // Optional: limit for context memories to include
        public Integer contextLimit;

        // Optional: whether to include system instructions
        public Boolean includeSystemInstructions;
    }

    // Response model for generated prompt
    public static class GeneratePromptResponse {
        public String prompt;
        public int contextMemoriesUsed;
    }
}
