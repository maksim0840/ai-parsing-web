package io.github.maksim0840.parsingtaskorchestrator.service;

import com.openai.errors.NotFoundException;
import io.github.maksim0840.internalapi.common.v1.s3.S3StorageService;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMRequestDTO;
import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.LLMResponseDTO;
import io.github.maksim0840.parsingtaskorchestrator.config.S3Config;
import io.github.maksim0840.parsingtaskorchestrator.config.properties.LLMProperties;
import io.github.maksim0840.parsingtaskorchestrator.config.properties.S3Properties;
import io.github.maksim0840.parsingtaskorchestrator.llm.GigaChat;
import io.github.maksim0840.parsingtaskorchestrator.llm.LLM;
import io.github.maksim0840.parsingtaskorchestrator.llm.YandexGPT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class LLMService {
    private final Map<String, LLM> availableModels;
    private final S3StorageService s3StorageService;

    public LLMService(LLMProperties llmProperties, S3Client s3Client, S3Properties s3Properties) {
        this.availableModels = Map.of(
                llmProperties.yandexgptModelViewName(), new YandexGPT(llmProperties),
                llmProperties.gigachatModelViewName(), new GigaChat(llmProperties)
        );
        this.s3StorageService = new S3StorageService(s3Client, s3Properties.bucketName());
    }

    public String sendRequestToModel(String model, String systemMessage, String userMessage, Double temperature, Integer maxOutputTokens, List<String> htmlPaths, Map<String, String> textByImage) {
        StringBuilder newUserMessage = new StringBuilder(userMessage);

        // Добавляем в контекст содержаение html-документов
        for (int i = 0; i < htmlPaths.size(); ++i) {
            String htmlFileContent = new String(s3StorageService.downloadFileBytes(htmlPaths.get(i)), StandardCharsets.UTF_8);
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

    public LLMResponseDTO processLlmRequest(LLMRequestDTO request) {
        try {
            String output = sendRequestToModel(request.modelName(), request.systemMessage(), request.userMessage(), request.temperature(), request.maxOutputTokens(), request.htmlPaths(), request.textByImage());
            return new LLMResponseDTO(request.taskId(), true, "", output);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new LLMResponseDTO(request.taskId(), false, "llm error: " + e.getMessage(), null);
        }
    }
}
