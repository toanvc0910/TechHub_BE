package com.techhub.app.aiservice.service;

import com.techhub.app.aiservice.config.GeminiProperties;
import com.techhub.app.aiservice.config.OpenAiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class AiProviderConfigService {

    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("openai", "gemini");
    private static final Map<String, Set<String>> SUPPORTED_CHAT_MODELS = Map.of(
            "openai", Set.of("gpt-4o-mini", "gpt-4.1-mini", "gpt-4.1"),
            "gemini", Set.of("gemini-2.5-flash", "gemini-2.5-pro"));

    private final AtomicReference<String> currentProvider;
    private final AtomicReference<String> openAiChatModel;
    private final AtomicReference<String> geminiChatModel;

    public AiProviderConfigService(
            @Value("${ai.provider:gemini}") String initialProvider,
            OpenAiProperties openAiProperties,
            GeminiProperties geminiProperties) {
        this.currentProvider = new AtomicReference<>(normalize(initialProvider));
        this.openAiChatModel = new AtomicReference<>(normalizeModel("openai", openAiProperties.getChat().getModel()));
        this.geminiChatModel = new AtomicReference<>(normalizeModel("gemini", geminiProperties.getChat().getModel()));
        log.info("AI provider initialized: {}", this.currentProvider.get());
        log.info("AI chat models initialized: openai={}, gemini={}", this.openAiChatModel.get(),
                this.geminiChatModel.get());
    }

    public String getProvider() {
        return currentProvider.get();
    }

    public String setProvider(String provider) {
        String normalized = normalize(provider);
        currentProvider.set(normalized);
        log.info("AI provider switched to: {}", normalized);
        return normalized;
    }

    public String getModelForProvider(String provider) {
        String normalizedProvider = normalize(provider);
        if ("openai".equals(normalizedProvider)) {
            return openAiChatModel.get();
        }
        return geminiChatModel.get();
    }

    public String getActiveChatModel() {
        return getModelForProvider(getProvider());
    }

    public String setModelForProvider(String provider, String model) {
        String normalizedProvider = normalize(provider);
        String normalizedModel = normalizeModel(normalizedProvider, model);

        if ("openai".equals(normalizedProvider)) {
            openAiChatModel.set(normalizedModel);
        } else {
            geminiChatModel.set(normalizedModel);
        }

        log.info("AI model updated: provider={}, model={}", normalizedProvider, normalizedModel);
        return normalizedModel;
    }

    public Map<String, String> getCurrentModels() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("openai", openAiChatModel.get());
        result.put("gemini", geminiChatModel.get());
        return result;
    }

    public Map<String, Set<String>> getSupportedChatModels() {
        return SUPPORTED_CHAT_MODELS;
    }

    public boolean isModelSupported(String provider, String model) {
        if (model == null || model.isBlank()) {
            return false;
        }
        String normalizedProvider = normalize(provider);
        Set<String> allowed = SUPPORTED_CHAT_MODELS.get(normalizedProvider);
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        return allowed.contains(model.trim().toLowerCase(Locale.ROOT));
    }

    public boolean isSupported(String provider) {
        if (provider == null) {
            return false;
        }
        return SUPPORTED_PROVIDERS.contains(provider.trim().toLowerCase(Locale.ROOT));
    }

    private String normalize(String provider) {
        if (provider == null) {
            return "gemini";
        }
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_PROVIDERS.contains(normalized)) {
            log.warn("Unknown ai.provider='{}', fallback to gemini", provider);
            return "gemini";
        }
        return normalized;
    }

    private String normalizeModel(String provider, String model) {
        Set<String> allowed = SUPPORTED_CHAT_MODELS.get(normalize(provider));
        if (model == null || model.isBlank()) {
            return fallbackModel(provider);
        }
        String normalizedModel = model.trim().toLowerCase(Locale.ROOT);
        if (allowed != null && !allowed.isEmpty() && !allowed.contains(normalizedModel)) {
            log.warn("Unsupported model '{}' for provider '{}', fallback to default", model, provider);
            return fallbackModel(provider);
        }
        return normalizedModel;
    }

    private String fallbackModel(String provider) {
        return "openai".equals(normalize(provider)) ? "gpt-4o-mini" : "gemini-2.5-flash";
    }
}