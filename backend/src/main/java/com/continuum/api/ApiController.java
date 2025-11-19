package com.continuum.api;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.UUID;

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

    @PostMapping("api/memories")
    public ResponseEntity<MemoryResponse> createMemory(@RequestBody CreateMemoryRequest request) {
        MemoryResponse resp = new MemoryResponse();
        resp.id = UUID.randomUUID().toString();
        resp.userId = request.userId;
        resp.source = request.source;
        resp.content = request.content;
        
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }
}
