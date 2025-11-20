// Service to handle logic part of http requests of the memory endpoint

package com.continuum.api;

import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class MemoryService {

    private final Map<String, ApiModels.MemoryResponse> memories = new HashMap<>();

    // Logic for creating a memory
    public ApiModels.MemoryResponse createMemory(ApiModels.CreateMemoryRequest request) {
        ApiModels.MemoryResponse resp = new ApiModels.MemoryResponse();
        resp.id = UUID.randomUUID().toString();
        resp.userId = request.userId;
        resp.source = request.source;
        resp.content = request.content;

        memories.put(resp.id, resp);
        return resp;
    }

    // List all memories
    public List<ApiModels.MemoryResponse> listMemories() {
        return new ArrayList<>(memories.values());
    }

    // Get memory by id
    public ApiModels.MemoryResponse getMemoryById(String id) {
        return memories.get(id);
    }

    // Delete memory 
    public boolean deleteMemoryById(String id) {
        ApiModels.MemoryResponse removed = memories.remove(id);
        return removed != null;
    }
}