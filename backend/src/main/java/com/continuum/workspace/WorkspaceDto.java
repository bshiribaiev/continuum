package com.continuum.workspace;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class WorkspaceDto {

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


