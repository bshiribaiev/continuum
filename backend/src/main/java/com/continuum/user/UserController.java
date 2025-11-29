package com.continuum.user;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Create user
    @PostMapping("api/users")
    public ResponseEntity<UserDto.UserResponse> createUser(
            @Valid @RequestBody UserDto.CreateUserRequest request) {
        try {
            UserDto.UserResponse resp = userService.createUser(request);
            return new ResponseEntity<>(resp, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Get all users
    @GetMapping("api/users")
    public List<UserDto.UserResponse> getUsers(@RequestParam(required = false) String username) {
        if (username == null) {
            return userService.listUsers();
        }
        UserDto.UserResponse user = userService.getUserByUsername(username);
        return user != null ? List.of(user) : List.of();
    }

    // Get specific user
    @GetMapping("api/users/{id}")
    public ResponseEntity<UserDto.UserResponse> getUserById(@PathVariable @NonNull String id) {
        UserDto.UserResponse resp = userService.getUserById(id);
        if (resp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resp);
    }

    // Update user
    @PutMapping("api/users/{id}")
    public ResponseEntity<UserDto.UserResponse> updateUser(@PathVariable @NonNull String id,
            @Valid @RequestBody UserDto.CreateUserRequest request) {
        try {
            UserDto.UserResponse resp = userService.updateUser(id, request);
            if (resp == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Delete user
    @DeleteMapping("api/users/{id}")
    public ResponseEntity<Void> deleteUserById(@PathVariable @NonNull String id) {
        boolean deleted = userService.deleteUserById(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}


