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
            promptBuilder.append("## Context Items\n\n");
            for (int i = 0; i < contextMemories.size(); i++) {
                MemoryDto.MemoryResponse memory = contextMemories.get(i);
                promptBuilder.append(String.format(
                        "%d. [source=%s] %s\n\n",
                        i + 1,
                        memory.source,
                        memory.content));
            }
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


