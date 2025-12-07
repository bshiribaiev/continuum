package com.continuum.prompt;

import java.util.List;
import org.springframework.stereotype.Service;
import com.continuum.memory.MemoryDto;
import com.continuum.memory.MemoryService;

@Service
public class PromptService {

    private final MemoryService memoryService;

    public PromptService(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    // Build formatted prompt string from context and task
    private String buildPrompt(
            List<MemoryDto.MemoryResponse> contextMemories,
            String task,
            boolean includeInstructions) {

        StringBuilder promptBuilder = new StringBuilder();

        if (includeInstructions) {
            // Short, direct system-style instructions
            promptBuilder
                    .append(
                            "You are an AI assistant. Use the CONTEXT section as the user's long-term memory (goals, preferences, history).\n")
                    .append("Read CONTEXT first, then answer TASK. Prefer recent or explicitly corrective items when there is conflict.\n\n");
        }

        promptBuilder.append("===== CONTEXT START =====\n\n");

        if (contextMemories == null || contextMemories.isEmpty()) {
            promptBuilder.append("No prior context was found for this user. Use only the TASK below.\n\n");
        } else {
            // Partition memories by semantic type
            List<MemoryDto.MemoryResponse> preferences = contextMemories.stream()
                    .filter(m -> "PREFERENCE".equalsIgnoreCase(m.type))
                    .toList();

            List<MemoryDto.MemoryResponse> goals = contextMemories.stream()
                    .filter(m -> "GOAL".equalsIgnoreCase(m.type))
                    .toList();

            List<MemoryDto.MemoryResponse> tasks = contextMemories.stream()
                    .filter(m -> "TASK".equalsIgnoreCase(m.type))
                    .toList();
            // De-duplicate tasks by normalized content so we don't repeat the same wording
            tasks = dedupeByContent(tasks);

            List<MemoryDto.MemoryResponse> decisions = contextMemories.stream()
                    .filter(m -> "DECISION".equalsIgnoreCase(m.type)
                            || "CONSTRAINT".equalsIgnoreCase(m.type))
                    .toList();

            List<MemoryDto.MemoryResponse> facts = contextMemories.stream()
                    .filter(m -> m.type == null
                            || (!"PREFERENCE".equalsIgnoreCase(m.type)
                                    && !"GOAL".equalsIgnoreCase(m.type)
                                    && !"TASK".equalsIgnoreCase(m.type)
                                    && !"DECISION".equalsIgnoreCase(m.type)
                                    && !"CONSTRAINT".equalsIgnoreCase(m.type)))
                    .toList();

            // Helper to detect "bad" memories that already contain full woven prompts
            java.util.function.Predicate<MemoryDto.MemoryResponse> isWovenPromptMemory = m -> {
                if (m == null || m.content == null) {
                    return false;
                }
                String c = m.content;
                return c.contains("===== CONTEXT START =====")
                        || c.contains("You are an AI assistant that uses a persistent memory layer (Continuum)");
            };

            // Helper to render a section in a compact, LLM-friendly way
            java.util.function.BiConsumer<String, List<MemoryDto.MemoryResponse>> renderSection = (title, list) -> {
                if (list == null || list.isEmpty()) {
                    return;
                }
                promptBuilder.append("## ").append(title).append("\n\n");
                int index = 1;
                for (MemoryDto.MemoryResponse memory : list) {
                    if (!memory.active) {
                        continue; // skip inactive / superseded memories
                    }
                    if (isWovenPromptMemory.test(memory)) {
                        continue; // skip memories that already contain full woven prompts
                    }

                    promptBuilder.append(index++).append(". ");
                    promptBuilder.append(memory.content);

                    // Only surface metadata that materially helps the model.
                    StringBuilder meta = new StringBuilder();
                    if (memory.importance != null && memory.importance >= 4) {
                        if (meta.length() > 0) {
                            meta.append(", ");
                        }
                        meta.append("importance: ").append(memory.importance);
                    }
                    if (memory.tags != null && !memory.tags.isBlank()) {
                        if (meta.length() > 0) {
                            meta.append(", ");
                        }
                        meta.append("tags: ").append(memory.tags);
                    }

                    if (meta.length() > 0) {
                        promptBuilder.append(" (").append(meta).append(")");
                    }

                    promptBuilder.append("\n\n");
                }
            };

            renderSection.accept("User Preferences", preferences);
            renderSection.accept("Current Goals", goals);
            renderSection.accept("Recent Prompts", tasks);
            renderSection.accept("Important Decisions / Constraints", decisions);
            renderSection.accept("Other Relevant Facts", facts);
        }

        promptBuilder.append("===== CONTEXT END =====\n\n");

        promptBuilder.append("## TASK\n\n");
        promptBuilder.append(task).append("\n");

        promptBuilder.append(
                "\nWhen answering, ground your response in the CONTEXT where relevant and keep it consistent with the user's long-term preferences.");

        return promptBuilder.toString();
    }

    // De-duplicate memories by normalized content, preserving order of first
    // occurrence
    private List<MemoryDto.MemoryResponse> dedupeByContent(List<MemoryDto.MemoryResponse> memories) {
        if (memories == null || memories.isEmpty()) {
            return memories;
        }
        java.util.Map<String, MemoryDto.MemoryResponse> byContent = new java.util.LinkedHashMap<>();
        for (MemoryDto.MemoryResponse memory : memories) {
            if (memory == null || memory.content == null) {
                continue;
            }
            String key = memory.content.trim().toLowerCase();
            if (!byContent.containsKey(key)) {
                byContent.put(key, memory);
            }
        }
        return java.util.List.copyOf(byContent.values());
    }

    // Generate prompt with context
    public PromptDto.GeneratePromptResponse generatePrompt(
            String userId,
            String workspaceId,
            String task,
            Integer contextLimit,
            Boolean includeSystemInstructions) {

        int limit = (contextLimit == null || contextLimit <= 0) ? 5 : contextLimit;
        boolean includeInstructions = includeSystemInstructions != null && includeSystemInstructions;

        // Get relevant context memories
        List<MemoryDto.MemoryResponse> contextMemories = memoryService.queryContext(userId, workspaceId, task, limit);
        String prompt = buildPrompt(contextMemories, task, includeInstructions);

        PromptDto.GeneratePromptResponse responseModel = new PromptDto.GeneratePromptResponse();
        responseModel.prompt = prompt;
        responseModel.contextMemoriesUsed = contextMemories.size();
        return responseModel;
    }
}