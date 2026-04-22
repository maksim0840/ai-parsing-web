package io.github.maksim0840.parsingtaskorchestrator.service;

import com.openai.errors.NotFoundException;
import io.github.maksim0840.parsingtaskorchestrator.llm.GigaChat;
import io.github.maksim0840.parsingtaskorchestrator.llm.LLM;
import io.github.maksim0840.parsingtaskorchestrator.llm.YandexGPT;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LLMService {
    private static final String YANDEXGPT_MODEL_VIEW_NAME = System.getenv("YANDEXGPT_MODEL_VIEW_NAME");
    private static final String GIGACHAT_MODEL_VIEW_NAME = System.getenv("GIGACHAT_MODEL_VIEW_NAME");

    private final Map<String, LLM> availableModels;

    public LLMService() {
        this.availableModels = Map.of(
                YANDEXGPT_MODEL_VIEW_NAME, new YandexGPT(),
                GIGACHAT_MODEL_VIEW_NAME, new GigaChat()
        );
    }

    public String sendRequestToModel(String model, String systemMessage, String userMessage, Double temperature, Integer maxOutputTokens) {
        if (availableModels.containsKey(model)) {
            return availableModels.get(model).sendRequest(systemMessage, userMessage, temperature, maxOutputTokens);
        }
        throw new RuntimeException("Unknown model");
    }
}
