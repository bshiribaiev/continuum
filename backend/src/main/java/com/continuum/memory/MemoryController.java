package com.continuum.memory;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    // Create memory
    @PostMapping("api/memories")
    public ResponseEntity<MemoryDto.MemoryResponse> createMemory(
            @Valid @RequestBody MemoryDto.CreateMemoryRequest request) {
        MemoryDto.MemoryResponse resp = memoryService.createMemory(request);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    // Get all memories
    @GetMapping("api/memories")
    public List<MemoryDto.MemoryResponse> getMemories(@RequestParam(required = false) String userId) {
        if (userId == null) {
            return memoryService.listMemories();
        }
        return memoryService.listMemoriesByUserId(userId);
    }

    // Get specific memory
    @GetMapping("api/memories/{id}")
    public ResponseEntity<MemoryDto.MemoryResponse> getMemoryById(@PathVariable @NonNull String id) {
        MemoryDto.MemoryResponse resp = memoryService.getMemoryById(id);
        if (resp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resp);
    }

    // Update memory
    @PutMapping("api/memories/{id}")
    public ResponseEntity<MemoryDto.MemoryResponse> updateMemory(@PathVariable @NonNull String id,
            @Valid @RequestBody MemoryDto.CreateMemoryRequest request) {

        MemoryDto.MemoryResponse resp = memoryService.updateMemory(id, request);
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

    // Ingest a raw message and turn it into a memory
    @PostMapping("api/ingestion/messages")
    public ResponseEntity<MemoryDto.MemoryResponse> ingestMessage(
            @Valid @RequestBody MemoryDto.IngestMessageRequest request) {

        MemoryDto.CreateMemoryRequest memoryRequest = new MemoryDto.CreateMemoryRequest();
        memoryRequest.userId = request.userId;
        memoryRequest.source = request.source;
        memoryRequest.content = request.text;
        memoryRequest.type = request.type;
        memoryRequest.topic = request.topic;
        memoryRequest.tags = request.tags;
        memoryRequest.importance = request.importance;

        MemoryDto.MemoryResponse resp = memoryService.createMemory(memoryRequest);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    // Query context: return the most relevant memories for a user + query
    @PostMapping("api/context/query")
    public ResponseEntity<MemoryDto.ContextQueryResponse> queryContext(
            @Valid @RequestBody MemoryDto.ContextQueryRequest request) {

        int limit = (request.limit == null || request.limit <= 0) ? 5 : request.limit;
        List<MemoryDto.MemoryResponse> results = memoryService.queryContext(
                request.userId,
                request.query,
                limit);

        MemoryDto.ContextQueryResponse response = new MemoryDto.ContextQueryResponse();
        response.memories = results;

        return ResponseEntity.ok(response);
    }
}
