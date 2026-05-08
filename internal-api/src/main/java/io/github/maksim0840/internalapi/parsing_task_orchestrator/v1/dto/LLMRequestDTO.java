package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto;

import lombok.Builder;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Builder
@Nullable
public record LLMRequestDTO(
        String taskId,
        String modelName,
        String systemMessage,
        String userMessage,
        Double temperature,
        Integer maxOutputTokens,
        List<String> htmlPaths,
        Map<String, String> textByImage
) {
    // Использовать значения этого же объекта, но с измененным полем htmlPaths
    public LLMRequestDTO withHtmlPaths(List<String> newHtmlPaths) {
        return new LLMRequestDTO(
                this.taskId,
                this.modelName,
                this.systemMessage,
                this.userMessage,
                this.temperature,
                this.maxOutputTokens,
                newHtmlPaths,
                this.textByImage
        );
    }

    // Добавить несколько элементов в htmlPaths
    public LLMRequestDTO addAllToHtmlPaths(List<String> extraHtmlPaths) {
        List<String> newHtmlPaths = this.htmlPaths;
        newHtmlPaths.addAll(extraHtmlPaths);
        return withHtmlPaths(newHtmlPaths);
    }

    // Добавить один элемент в htmlPaths
    public LLMRequestDTO addToHtmlPaths(String extraHtmlPath) {
        List<String> newHtmlPaths = this.htmlPaths;
        newHtmlPaths.add(extraHtmlPath);
        return withHtmlPaths(newHtmlPaths);
    }

    // Использовать значения этого же объекта, но с измененным полем textByImage
    public LLMRequestDTO withTextByImage(Map<String, String> newTextByImage) {
        return new LLMRequestDTO(
                this.taskId,
                this.modelName,
                this.systemMessage,
                this.userMessage,
                this.temperature,
                this.maxOutputTokens,
                this.htmlPaths,
                newTextByImage
        );
    }

    // Добавить несколько элементов в textByImage
    public LLMRequestDTO putAllToTextByImage(Map<String, String> extraTextByImage) {
        Map<String, String> newTextByImage = this.textByImage;
        newTextByImage.putAll(extraTextByImage);
        return withTextByImage(newTextByImage);
    }
}
