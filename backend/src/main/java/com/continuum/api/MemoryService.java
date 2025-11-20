// Service to handle logic part of http requests of the memory endpoint

package com.continuum.api;

import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class MemoryService {
    
    private final Map<String, ApiController.MemoryResponse> memories = new HashMap<>();

    // Logic for creating a memory
    public ApiController.MemoryResponse createMemory(ApiController.CreateMemoryRequest request) {
        ApiController.MemoryResponse resp = new ApiController.MemoryResponse();
        resp.id = UUID.randomUUID().toString();
        resp.userId = request.userId;
        resp.source = request.source;
        resp.content = request.content;

        memories.put(resp.id, resp);
        return resp;
    }

    // List all memories
    public List<ApiController.MemoryResponse> listMemories() {
        return new ArrayList<>(memories.values());
    }

    // Get memory by id
    public ApiController.MemoryResponse getMemoryById(String id) {
        return memories.get(id);
    }
}