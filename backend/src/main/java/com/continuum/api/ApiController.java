// Controller for handling http requests

package com.continuum.api;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import java.util.*;

@RestController
public class ApiController {

    // Request model for http 
    public static class CreateMemoryRequest {
        public String userId;
        public String source;
        public String content;
    }

    // Reponse model for http
    public static class MemoryResponse {
        public String id;
        public String userId;
        public String source;
        public String content;
    }

    private final MemoryService memoryService;
    
    // Constructor
    public ApiController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    // Create memory
    @PostMapping("api/memories")
    public ResponseEntity<MemoryResponse> createMemory(@RequestBody CreateMemoryRequest request) {
        MemoryResponse resp = memoryService.createMemory(request);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    // Get all memories
    @GetMapping("api/memories")
    public List<MemoryResponse> getMemory() {
        return memoryService.listMemories();
    }

    // Get specific memory
    @GetMapping("api/memories/{id}")
    public ResponseEntity<MemoryResponse> getMemoryById(@PathVariable String id) {
        MemoryResponse resp = memoryService.getMemoryById(id);
        if (resp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resp);
    }
}