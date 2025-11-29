package com.continuum.prompt;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
public class PromptController {

    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    // Generate prompt
    @PostMapping("api/prompts/generate")
    public ResponseEntity<PromptDto.GeneratePromptResponse> generatePrompt(
            @Valid @RequestBody PromptDto.GeneratePromptRequest request) {
        PromptDto.GeneratePromptResponse res = promptService.generatePrompt(
                request.userId,
                request.task,
                request.contextLimit,
                request.includeSystemInstructions);

        return ResponseEntity.ok(res);
    }
}


