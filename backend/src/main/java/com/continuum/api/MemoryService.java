// Service to handle logic of http requests of the memory endpoint

package com.continuum.api;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;

@Service
public class MemoryService {

    private final MemoryRepository repository;

    // Constructor
    public MemoryService(MemoryRepository repository) {
        this.repository = repository;
    }

    // Convert Memory entity into MemoryResponse
    public ApiModels.MemoryResponse toResponse(Memory memory) {
        ApiModels.MemoryResponse resp = new ApiModels.MemoryResponse();
        resp.id = memory.id;
        resp.userId = memory.userId;
        resp.source = memory.source;
        resp.content = memory.content;
        return resp;
    }

    // Create a memory
    public ApiModels.MemoryResponse createMemory(ApiModels.CreateMemoryRequest request) {
        Memory memory = new Memory();
        memory.id = UUID.randomUUID().toString();
        memory.userId = request.userId;
        memory.source = request.source;
        memory.content = request.content;

        Memory saved = repository.save(memory);
        return toResponse(saved);
    }

    // Update a memory
    public ApiModels.MemoryResponse updateMemory(@NonNull String id, ApiModels.CreateMemoryRequest request) {
        Optional<Memory> optional = repository.findById(id);
        if (optional.isEmpty()) {
            return null; // controller will turn this into 404
        }

        Memory existing = optional.get();
        existing.userId = request.userId;
        existing.source = request.source;
        existing.content = request.content;

        Memory updated = repository.save(existing);
        return toResponse(updated);
    }

    // List all memories
    public List<ApiModels.MemoryResponse> listMemories() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    // List all memories by user
    public List<ApiModels.MemoryResponse> listMemoriesByUserId(String userId) {
        return repository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    // Query memories for a user based on a simple text query, later this will call vector/graph search
    public List<ApiModels.MemoryResponse> queryContext(String userId, String query, int limit) {
        String normalizedQuery = query.toLowerCase();
        String[] terms = normalizedQuery.split("\\s+");

        List<Memory> userMemories = repository.findByUserId(userId);
        
        return userMemories.stream()
                .sorted((a, b) -> {
                    int scoreA = score(a.content, terms);
                    int scoreB = score(b.content, terms);
                    return Integer.compare(scoreB, scoreA);
                })
                .limit(limit)
                .map(this::toResponse)
                .toList();
    }

    // More matching words = higher score (will be replaced with embedded vectors later)
    private int score(String content, String[] terms) {
        if (content == null) {
            return 0;
        }
        String lower = content.toLowerCase();
        int score = 0;
        for (String term : terms) {
            if (!term.isEmpty() && lower.contains(term)) {
                score++;
            }
        }
        return score;
    }

    // Get memory by id
    public ApiModels.MemoryResponse getMemoryById(@NonNull String id) {
        Optional<Memory> optional = repository.findById(id);
        if (optional.isEmpty()) {
            return null;
        }
        return toResponse(optional.get());
    }

    // Delete memory
    public boolean deleteMemoryById(@NonNull String id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }
}