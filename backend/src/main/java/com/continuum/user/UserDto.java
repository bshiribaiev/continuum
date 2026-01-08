package com.continuum.user;

import jakarta.validation.constraints.NotBlank;

public class UserDto {

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
}