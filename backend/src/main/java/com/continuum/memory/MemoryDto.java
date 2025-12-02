package com.continuum.memory;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class MemoryDto {

    // Request model for creating/updating a memory directly
    public static class CreateMemoryRequest {
        @NotBlank
        public String userId;

        @NotBlank
        public String source;

        @NotBlank
        public String content;

        // Optional semantic fields
        // e.g. "PREFERENCE", "GOAL", "TASK", "DECISION", "FACT", "CONSTRAINT"
        public String type;

        // Optional topic key (e.g. "tone", "language")
        public String topic;

        // Optional comma-separated tags (e.g. "coding,python")
        public String tags;

        // Optional importance from 1 (low) to 5 (high)
        public Integer importance;
    }

    // Request model for ingesting a raw message that will become a memory
    public static class IngestMessageRequest {
        @NotBlank
        public String userId;

        @NotBlank
        public String source;

        @NotBlank
        public String text;

        // Optional semantic fields when ingesting directly from a client
        public String type;
        public String topic;
        public String tags;
        public Integer importance;
    }

    // Request model for querying context (memories) for a user + task/query
    public static class ContextQueryRequest {
        @NotBlank
        public String userId;

        @NotBlank
        public String query;

        // Limit for how many memories to return, null means "use default"
        public Integer limit;
    }

    // Response model for memory data over HTTP
    public static class MemoryResponse {
        public String id;
        public String userId;
        public String source;
        public String content;

        // Semantic metadata
        public String type;
        public String topic;
        public String tags;
        public Integer importance;
        public boolean active;
    }

    // Response model for context query
    public static class ContextQueryResponse {
        public List<MemoryResponse> memories;
    }
}
