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

    // Constructor
    public ApiController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    // Create memory
    @PostMapping("api/memories")
    public ResponseEntity<ApiModels.MemoryResponse> createMemory(
            @Valid @RequestBody ApiModels.CreateMemoryRequest request) {
        ApiModels.MemoryResponse resp = memoryService.createMemory(request);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
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

    // Get all memories
    @GetMapping("api/memories")
    public List<ApiModels.MemoryResponse> getMemories(@RequestParam(required = false) String userId) {
        if (userId == null) {
            return memoryService.listMemories();
        }
        return memoryService.listMemoriesByUserId(userId);
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

    // Delete memory
    @DeleteMapping("api/memories/{id}")
    public ResponseEntity<Void> deleteMemoryById(@PathVariable @NonNull String id) {
        boolean deleted = memoryService.deleteMemoryById(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}