package com.worldsbank.ai.controller;

import com.worldsbank.ai.dto.AiRequest;
import com.worldsbank.ai.dto.AiResponse;
import com.worldsbank.ai.service.AiAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/assistant")
    public ResponseEntity<AiResponse> askAssistant(
            @Valid @RequestBody AiRequest request) {
        return ResponseEntity.ok(aiAssistantService.askAssistant(request));
    }
}