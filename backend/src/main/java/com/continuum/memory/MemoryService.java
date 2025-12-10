// Service to handle logic of http requests of the memory endpoint

package com.continuum.memory;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;
import com.continuum.nlu.NluClient;

@Service
public class MemoryService {

    private final MemoryRepository repository;
    private final NluClient nluClient;

    public MemoryService(MemoryRepository repository, NluClient nluClient) {
        this.repository = repository;
        this.nluClient = nluClient;
    }

    // Convert Memory entity into MemoryResponse
    public MemoryDto.MemoryResponse toResponse(Memory memory) {
        MemoryDto.MemoryResponse resp = new MemoryDto.MemoryResponse();
        resp.id = memory.id;
        resp.userId = memory.userId;
        resp.workspaceId = memory.workspaceId;
        resp.source = memory.source;
        resp.content = memory.content;
        resp.type = memory.type;
        resp.topic = memory.topic;
        resp.tags = memory.tags;
        resp.importance = memory.importance;
        resp.active = memory.active;
        return resp;
    }

    // Create a memory
    public MemoryDto.MemoryResponse createMemory(MemoryDto.CreateMemoryRequest request) {
        Memory memory = new Memory();
        memory.id = UUID.randomUUID().toString();
        memory.userId = request.userId;
        memory.workspaceId = request.workspaceId;
        memory.source = request.source;
        memory.content = request.content;
        // Decide on the semantic type. Prefer explicit client type, otherwise ask NLU.
        String resolvedType = request.type;
        if (resolvedType == null || resolvedType.isBlank()) {
            String predicted = nluClient.classifyIntent(memory.content);
            if (predicted != null && !predicted.isBlank()) {
                resolvedType = predicted;
            }
        }
        memory.type = (resolvedType == null || resolvedType.isBlank()) ? "OTHER" : resolvedType;
        memory.topic = request.topic;
        memory.tags = request.tags;
        memory.importance = request.importance;

        // If this is a PREFERENCE with a topic, mark older preferences for the same
        // user+topic as inactive and superseded.
        if ("PREFERENCE".equalsIgnoreCase(memory.type) && memory.topic != null && !memory.topic.isBlank()) {
            List<Memory> existingPrefs = repository.findByUserId(request.userId).stream()
                    .filter(m -> m.type != null
                            && "PREFERENCE".equalsIgnoreCase(m.type)
                            && Objects.equals(m.topic, memory.topic)
                            && Objects.equals(m.workspaceId, memory.workspaceId)
                            && m.active)
                    .toList();

            for (Memory old : existingPrefs) {
                old.active = false;
                old.supersededById = memory.id;
                repository.save(old);
            }
        }

        Memory saved = repository.save(memory);
        return toResponse(saved);
    }

    // Update a memory
    public MemoryDto.MemoryResponse updateMemory(@NonNull String id,
            MemoryDto.CreateMemoryRequest request) {
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
    public List<MemoryDto.MemoryResponse> listMemories() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    // List all memories by user
    public List<MemoryDto.MemoryResponse> listMemoriesByUserId(String userId) {
        return repository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    // Query memories for a user (optionally scoped to a workspace) based on a
    // simple
    // text query. Later this will call vector/graph search.
    public List<MemoryDto.MemoryResponse> queryContext(String userId, String workspaceId, String query, int limit) {
        String normalizedQuery = query.toLowerCase();
        String[] terms = normalizedQuery.split("\\s+");

        List<Memory> userMemories;
        if (workspaceId != null && !workspaceId.isBlank()) {
            userMemories = repository.findByUserIdAndWorkspaceId(userId, workspaceId);
        } else {
            userMemories = repository.findByUserId(userId);
        }

        return userMemories.stream()
                .sorted((a, b) -> {
                    int scoreA = score(a.content, terms);
                    int scoreB = score(b.content, terms);
                    if (scoreA != scoreB) {
                        return Integer.compare(scoreB, scoreA); // higher score first
                    }
                    // tie‑break by recency (newer first)
                    if (a.createdAt == null || b.createdAt == null) {
                        return 0;
                    }
                    return b.createdAt.compareTo(a.createdAt);
                })
                .limit(limit)
                .map(this::toResponse)
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
    public MemoryDto.MemoryResponse getMemoryById(@NonNull String id) {
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
