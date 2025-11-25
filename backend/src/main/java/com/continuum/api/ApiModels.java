// Objects to define the structure of requests and transfer data

package com.continuum.api;

import jakarta.validation.constraints.NotBlank;
import java.util.*;
import java.time.LocalDateTime;

public class ApiModels {

    // Request model for creating/updating a memory directly
    public static class CreateMemoryRequest {
        @NotBlank
        public String userId;

        @NotBlank
        public String source;

        @NotBlank
        public String content;
    }

    // Request model for ingesting a raw message that will become a memory
    public static class IngestMessageRequest {
        @NotBlank
        public String userId;

        @NotBlank
        public String source;

        @NotBlank
        public String text;
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
    }

    // Response model for context query
    public static class ContextQueryResponse {
        public List<MemoryResponse> memories;
    }

    // Request model for creating/updating a user
    public static class CreateUserRequest {
        @NotBlank
        public String username;

        @NotBlank
        public String email;

        public String displayName;
    }

    // Response model for user data over HTTP
    public static class UserResponse {
        public String id;
        public String username;
        public String email;
        public String displayName;
    }

    // Request model for creating/updating a workspace
    public static class CreateWorkspaceRequest {
        @NotBlank
        public String name;

        @NotBlank
        public String ownerId;

        public String description;
    }

    // Response model for workspace data over HTTP
    public static class WorkspaceResponse {
        public String id;
        public String name;
        public String ownerId;
        public LocalDateTime createdAt;
        public String description;
    }    
}
