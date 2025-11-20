package com.continuum.api;

import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class MemoryService {
    private final Map<String, ApiController.MemoryResponse> memories = new HashMap<>();

    public ApiController.MemoryResponse createMemory(ApiController.CreateMemoryRequest request) {
        ApiController.MemoryResponse resp = new ApiController.MemoryResponse();
        resp.id = UUID.randomUUID().toString();
        resp.userId = request.userId;
        resp.source = request.source;
        resp.content = request.content;

        memories.put(resp.id, resp);
        return resp;
    }

    public List<ApiController.MemoryResponse> listMemories() {
        return new ArrayList<>(memories.values());
    }
}