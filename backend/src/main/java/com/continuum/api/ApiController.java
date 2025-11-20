package com.continuum.api;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;

@RestController
public class ApiController {

    public static class CreateMemoryRequest {
        public String userId;
        public String source;
        public String content;
    }

    public static class MemoryResponse {
        public String id;
        public String userId;
        public String source;
        public String content;
    }

    private final MemoryService memoryService;

    public ApiController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @PostMapping("api/memories")
    public ResponseEntity<MemoryResponse> createMemory(@RequestBody CreateMemoryRequest request) {
        MemoryResponse resp = memoryService.createMemory(request);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }
}
