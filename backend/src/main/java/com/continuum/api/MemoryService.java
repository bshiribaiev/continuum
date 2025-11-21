// Service to handle logic part of http requests of the memory endpoint

package com.continuum.api;

import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class MemoryService {

    private final Map<String, ApiModels.MemoryResponse> memories = new HashMap<>();

    // Create a memory
    public ApiModels.MemoryResponse createMemory(ApiModels.CreateMemoryRequest request) {
        ApiModels.MemoryResponse resp = new ApiModels.MemoryResponse();
        resp.id = UUID.randomUUID().toString();
        resp.userId = request.userId;
        resp.source = request.source;
        resp.content = request.content;

        memories.put(resp.id, resp);
        return resp;
    }

    // Update a memory
    public ApiModels.MemoryResponse updateMemory(String id, ApiModels.CreateMemoryRequest request) {
        ApiModels.MemoryResponse existing = memories.get(id);
        if (existing == null) {
            return null; // controller will turn this into 404
        }

        existing.userId = request.userId;
        existing.source = request.source;
        existing.content = request.content;

        return existing;
    }

    // List all memories
    public List<ApiModels.MemoryResponse> listMemories() {
        return new ArrayList<>(memories.values());
    }

    // List all memories
    public List<ApiModels.MemoryResponse> listMemoriesByUserId(String userId) {
        return memories.values().stream().filter(m -> m.userId.equals(userId)).toList();
    }

    // Query memories for a user based on a simple text query, later this will call
    // vector/graph search
    public List<ApiModels.MemoryResponse> queryContext(String userId, String query, int limit) {
        String normalizedQuery = query.toLowerCase();
        String[] terms = normalizedQuery.split("\\s+");

        return memories.values().stream()
                .filter(m -> m.userId.equals(userId))
                .sorted((a, b) -> {
                    int scoreA = score(a.content, terms);
                    int scoreB = score(b.content, terms);
                    return Integer.compare(scoreB, scoreA);
                })
                .limit(limit)
                .toList();
    }

    // More matching words = higher score (will be replaced with embedded vectors
    // later)
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
    public ApiModels.MemoryResponse getMemoryById(String id) {
        return memories.get(id);
    }

    // Delete memory
    public boolean deleteMemoryById(String id) {
        ApiModels.MemoryResponse removed = memories.remove(id);
        return removed != null;
    }
}