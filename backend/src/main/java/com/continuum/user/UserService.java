// Service to handle logic of http requests of the user endpoint

package com.continuum.user;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;

@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    // Convert User entity into UserResponse DTO
    private UserDto.UserResponse toResponse(User user) {
        UserDto.UserResponse resp = new UserDto.UserResponse();
        resp.id = user.id;
        resp.username = user.username;
        resp.email = user.email;
        resp.displayName = user.displayName;
        return resp;
    }

    // Create a user
    public UserDto.UserResponse createUser(UserDto.CreateUserRequest request) {
        // Check if username already exists
        if (repository.findByUsername(request.username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email already exists
        if (repository.findByEmail(request.email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.id = UUID.randomUUID().toString();
        user.username = request.username;
        user.email = request.email;
        user.displayName = request.displayName;

        User saved = repository.save(user);
        return toResponse(saved);
    }

    // Update a user
    public UserDto.UserResponse updateUser(@NonNull String id, UserDto.CreateUserRequest request) {
        Optional<User> optional = repository.findById(id);
        if (optional.isEmpty()) {
            return null; // controller will turn this into 404
        }

        User existing = optional.get();

        // Check if username is being changed and already exists
        if (!existing.username.equals(request.username)) {
            if (repository.findByUsername(request.username).isPresent()) {
                throw new IllegalArgumentException("Username already exists");
            }
        }

        // Check if email is being changed and already exists
        if (!existing.email.equals(request.email)) {
            if (repository.findByEmail(request.email).isPresent()) {
                throw new IllegalArgumentException("Email already exists");
            }
        }

        existing.username = request.username;
        existing.email = request.email;
        existing.displayName = request.displayName;

        User updated = repository.save(existing);
        return toResponse(updated);
    }

    // List all users
    public List<UserDto.UserResponse> listUsers() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    // Get user by id
    public UserDto.UserResponse getUserById(@NonNull String id) {
        Optional<User> optional = repository.findById(id);
        if (optional.isEmpty()) {
            return null;
        }
        return toResponse(optional.get());
    }

    // Get user by username
    public UserDto.UserResponse getUserByUsername(@NonNull String username) {
        Optional<User> optional = repository.findByUsername(username);
        if (optional.isEmpty()) {
            return null;
        }
        return toResponse(optional.get());
    }

    // Delete user
    public boolean deleteUserById(@NonNull String id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }
}


