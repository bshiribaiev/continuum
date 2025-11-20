// Controller for handling http requests

package com.continuum.api;

import java.util.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<ApiModels.MemoryResponse> createMemory(@Valid @RequestBody ApiModels.CreateMemoryRequest request) {
        ApiModels.MemoryResponse resp = memoryService.createMemory(request);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    // Get all memories
    @GetMapping("api/memories")
    public List<ApiModels.MemoryResponse> getMemory() {
        return memoryService.listMemories();
    }

    // Get specific memory
    @GetMapping("api/memories/{id}")
    public ResponseEntity<ApiModels.MemoryResponse> getMemoryById(@PathVariable String id) {
        ApiModels.MemoryResponse resp = memoryService.getMemoryById(id);
        if (resp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resp);
    }

    // Delete memory
    @DeleteMapping("api/memories/{id}")
    public ResponseEntity<Void> deleteMemoryById(@PathVariable String id) {
        boolean deleted = memoryService.deleteMemoryById(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}