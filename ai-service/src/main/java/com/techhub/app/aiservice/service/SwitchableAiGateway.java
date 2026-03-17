package com.techhub.app.aiservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class SwitchableAiGateway implements AiGateway {

    private final OpenAiGateway openAiGateway;
    private final GeminiGateway geminiGateway;
    private final AiProviderConfigService aiProviderConfigService;

    @Override
    public Object generateStructuredJson(String prompt, Object contextPayload) {
        return activeGateway().generateStructuredJson(prompt, contextPayload);
    }

    @Override
    public Flux<String> generateStreamingResponse(String prompt, String systemPrompt) {
        return activeGateway().generateStreamingResponse(prompt, systemPrompt);
    }

    @Override
    public Flux<String> generateStreamingResponseWithHistory(List<Map<String, String>> messages) {
        return activeGateway().generateStreamingResponseWithHistory(messages);
    }

    private AiGateway activeGateway() {
        String provider = aiProviderConfigService.getProvider();
        if ("openai".equals(provider)) {
            log.debug("Using OpenAI provider");
            return openAiGateway;
        }
        return geminiGateway;
    }
}
