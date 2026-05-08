package io.github.maksim0840.parsingtaskorchestrator.service;

import com.openai.errors.NotFoundException;
import io.github.maksim0840.parsingtaskorchestrator.llm.GigaChat;
import io.github.maksim0840.parsingtaskorchestrator.llm.LLM;
import io.github.maksim0840.parsingtaskorchestrator.llm.YandexGPT;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public String sendRequestToModel(String model, String systemMessage, String userMessage, Double temperature, Integer maxOutputTokens, List<String> htmlPaths, Map<String, String> textByImage) {
        StringBuilder newUserMessage = new StringBuilder(userMessage);
        // Добавляем в контекст текст с html-документов
        for (int i = 0; i < htmlPaths.size(); ++i) {
            String htmlFileContent = "!!!ТЕКСТ ДОКУМЕНТА!!!";
            newUserMessage.append(String.format("\n\n=== HTML-страница №%d ===\n\n%s", i + 1, htmlFileContent));
        }
        // Добавляем в контекст текст с изображений
        int curImgNum = 0;
        for (Map.Entry<String, String> entry : textByImage.entrySet()) {
            if (!entry.getValue().isBlank()) {
                newUserMessage.append(String.format("\n\n=== Текст с картинки №%d ===\n\n%s", curImgNum + 1, entry.getValue()));
                ++curImgNum;
            }
        }

        if (availableModels.containsKey(model)) {
            return availableModels.get(model).sendRequest(systemMessage, newUserMessage.toString(), temperature, maxOutputTokens);
        }
        throw new RuntimeException("Unknown model");
    }
}
