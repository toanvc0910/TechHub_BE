package com.techhub.app.aiservice.service;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface AiGateway {
    Object generateStructuredJson(String prompt, Object contextPayload);

    Flux<String> generateStreamingResponse(String prompt, String systemPrompt);

    Flux<String> generateStreamingResponseWithHistory(List<Map<String, String>> messages);
}
