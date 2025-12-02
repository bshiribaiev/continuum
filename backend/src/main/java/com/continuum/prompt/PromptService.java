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
            promptBuilder
                    .append("You are an AI assistant that uses a persistent memory layer (Continuum) to stay\n")
                    .append("consistent with the user's goals, preferences, decisions, and history.\n\n")
                    .append("Guidelines:\n")
                    .append("1. Read the CONTEXT section before answering the TASK.\n")
                    .append("2. Prefer the user's stated preferences and past decisions over generic defaults.\n")
                    .append("3. If context conflicts, favor the most recent or explicitly corrective items.\n")
                    .append("4. If information is missing, say so rather than guessing.\n")
                    .append("5. Do not repeat the entire context verbatim; reference it succinctly.\n\n");
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

            // Helper to render a section
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
                    promptBuilder.append(String.format(
                            "%d. [source=%s",
                            index++,
                            memory.source));
                    if (memory.topic != null && !memory.topic.isBlank()) {
                        promptBuilder.append(", topic=").append(memory.topic);
                    }
                    if (memory.importance != null) {
                        promptBuilder.append(", importance=").append(memory.importance);
                    }
                    if (memory.tags != null && !memory.tags.isBlank()) {
                        promptBuilder.append(", tags=").append(memory.tags);
                    }
                    promptBuilder.append("] ");
                    promptBuilder.append(memory.content).append("\n\n");
                }
            };

            renderSection.accept("User Preferences", preferences);
            renderSection.accept("Current Goals", goals);
            renderSection.accept("Active Tasks", tasks);
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

    // Generate prompt with context
    public PromptDto.GeneratePromptResponse generatePrompt(
            String userId,
            String task,
            Integer contextLimit,
            Boolean includeSystemInstructions) {

        int limit = (contextLimit == null || contextLimit <= 0) ? 5 : contextLimit;
        boolean includeInstructions = includeSystemInstructions != null && includeSystemInstructions;

        // Get relevant context memories
        List<MemoryDto.MemoryResponse> contextMemories = memoryService.queryContext(userId, task, limit);
        String prompt = buildPrompt(contextMemories, task, includeInstructions);

        PromptDto.GeneratePromptResponse responseModel = new PromptDto.GeneratePromptResponse();
        responseModel.prompt = prompt;
        responseModel.contextMemoriesUsed = contextMemories.size();
        return responseModel;
    }
}