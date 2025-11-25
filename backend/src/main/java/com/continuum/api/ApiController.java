// Controller for handling http requests

package com.continuum.api;

import java.util.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import jakarta.validation.Valid;

@RestController
public class ApiController {

    private final MemoryService memoryService;
    private final UserService userService;
    private final WorkspaceService workspaceService;

    // Constructor
    public ApiController(MemoryService memoryService, UserService userService, WorkspaceService workspaceService) {
        this.memoryService = memoryService;
        this.userService = userService;
        this.workspaceService = workspaceService;
    }

    // Create memory
    @PostMapping("api/memories")
    public ResponseEntity<ApiModels.MemoryResponse> createMemory(
            @Valid @RequestBody ApiModels.CreateMemoryRequest request) {
        ApiModels.MemoryResponse resp = memoryService.createMemory(request);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    // Create user
    @PostMapping("api/users")
    public ResponseEntity<ApiModels.UserResponse> createUser(
            @Valid @RequestBody ApiModels.CreateUserRequest request) {
        try {
            ApiModels.UserResponse resp = userService.createUser(request);
            return new ResponseEntity<>(resp, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Create workspace
    @PostMapping("api/workspaces")
    public ResponseEntity<ApiModels.WorkspaceResponse> createWorkspace(
            @Valid @RequestBody ApiModels.CreateWorkspaceRequest request) {
        ApiModels.WorkspaceResponse resp = workspaceService.createWorkspace(request);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    // Get all memories
    @GetMapping("api/memories")
    public List<ApiModels.MemoryResponse> getMemories(@RequestParam(required = false) String userId) {
        if (userId == null) {
            return memoryService.listMemories();
        }
        return memoryService.listMemoriesByUserId(userId);
    }

    // Get all users
    @GetMapping("api/users")
    public List<ApiModels.UserResponse> getUsers(@RequestParam(required = false) String username) {
        if (username == null) {
            return userService.listUsers();
        }
        ApiModels.UserResponse user = userService.getUserByUsername(username);
        return user != null ? List.of(user) : List.of();
    }

    // Get all workspaces
    @GetMapping("api/workspaces")
    public List<ApiModels.WorkspaceResponse> getWorkspaces(@RequestParam(required = false) String ownerId) {
        if (ownerId == null) {
            return workspaceService.listWorkspaces();
        }
        return workspaceService.listWorkspacesByOwner(ownerId);
    }

    // Get specific user
    @GetMapping("api/users/{id}")
    public ResponseEntity<ApiModels.UserResponse> getUserById(@PathVariable @NonNull String id) {
        ApiModels.UserResponse resp = userService.getUserById(id);
        if (resp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resp);
    }

    // Get specific memory
    @GetMapping("api/memories/{id}")
    public ResponseEntity<ApiModels.MemoryResponse> getMemoryById(@PathVariable @NonNull String id) {
        ApiModels.MemoryResponse resp = memoryService.getMemoryById(id);
        if (resp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resp);
    }

    // Get specific workspace
    @GetMapping("api/workspaces/{id}")
    public ResponseEntity<ApiModels.WorkspaceResponse> getWorkspaceById(@PathVariable @NonNull String id) {
        ApiModels.WorkspaceResponse resp = workspaceService.getWorkspaceById(id);
        if (resp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resp);
    }
    
    // Ingest a raw message and turn it into a memory
    @PostMapping("api/ingestion/messages")
    public ResponseEntity<ApiModels.MemoryResponse> ingestMessage(
            @Valid @RequestBody ApiModels.IngestMessageRequest request) {

        ApiModels.CreateMemoryRequest memoryRequest = new ApiModels.CreateMemoryRequest();
        memoryRequest.userId = request.userId;
        memoryRequest.source = request.source;
        memoryRequest.content = request.text;

        ApiModels.MemoryResponse resp = memoryService.createMemory(memoryRequest);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    // Query context: return the most relevant memories for a user + query
    @PostMapping("api/context/query")
    public ResponseEntity<ApiModels.ContextQueryResponse> queryContext(
            @Valid @RequestBody ApiModels.ContextQueryRequest request) {

        int limit = (request.limit == null || request.limit <= 0) ? 5 : request.limit;
        List<ApiModels.MemoryResponse> results = memoryService.queryContext(
                request.userId,
                request.query,
                limit);

        ApiModels.ContextQueryResponse response = new ApiModels.ContextQueryResponse();
        response.memories = results;

        return ResponseEntity.ok(response);
    }

    // Update memory
    @PutMapping("api/memories/{id}")
    public ResponseEntity<ApiModels.MemoryResponse> updateMemory(@PathVariable @NonNull String id,
            @Valid @RequestBody ApiModels.CreateMemoryRequest request) {

        ApiModels.MemoryResponse resp = memoryService.updateMemory(id, request);
        if (resp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resp);
    }

    // Update user
    @PutMapping("api/users/{id}")
    public ResponseEntity<ApiModels.UserResponse> updateUser(@PathVariable @NonNull String id,
            @Valid @RequestBody ApiModels.CreateUserRequest request) {
        try {
            ApiModels.UserResponse resp = userService.updateUser(id, request);
            if (resp == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Update workspace
    @PutMapping("api/workspaces/{id}")
    public ResponseEntity<ApiModels.WorkspaceResponse> updateWorkspace(@PathVariable @NonNull String id,
            @Valid @RequestBody ApiModels.CreateWorkspaceRequest request) {
        ApiModels.WorkspaceResponse resp = workspaceService.updateWorkspace(id, request);
        if (resp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resp);
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

    // Delete memory
    @DeleteMapping("api/memories/{id}")
    public ResponseEntity<Void> deleteMemoryById(@PathVariable @NonNull String id) {
        boolean deleted = memoryService.deleteMemoryById(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    // Delete workspace
    @DeleteMapping("api/workspaces/{id}")
    public ResponseEntity<Void> deleteWorkspaceById(@PathVariable @NonNull String id) {
        boolean deleted = workspaceService.deleteWorkspaceById(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}