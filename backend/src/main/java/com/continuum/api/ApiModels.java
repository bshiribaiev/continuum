package com.continuum.api;

import jakarta.validation.constraints.NotBlank;

public class ApiModels {

    // Request model for http 
    public static class CreateMemoryRequest {
        @NotBlank
        public String userId;
        
        @NotBlank
        public String source;

        @NotBlank
        public String content;
    }

    // Reponse model for http
    public static class MemoryResponse {
        public String id;
        public String userId;
        public String source;
        public String content;
    }
}
