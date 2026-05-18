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
        List<FileInfoDTO> htmlDocs,
        List<FileInfoDTO> images
) {
    // Использовать значения этого же объекта, но с измененным полем htmlDocs
    public LLMRequestDTO withHtmlDocs(List<FileInfoDTO> newHtmlDocs) {
        return new LLMRequestDTO(
                this.taskId,
                this.modelName,
                this.systemMessage,
                this.userMessage,
                this.temperature,
                this.maxOutputTokens,
                newHtmlDocs,
                this.images
        );
    }

    // Добавить несколько элементов в htmlDocs
    public LLMRequestDTO addAllToHtmlDocs(List<FileInfoDTO> extraHtmlDocs) {
        List<FileInfoDTO> newHtmlDocs = this.htmlDocs;
        newHtmlDocs.addAll(extraHtmlDocs);
        return withHtmlDocs(newHtmlDocs);
    }

    // Добавить один элемент в htmlDocs
    public LLMRequestDTO addToHtmlDocs(FileInfoDTO extraHtmlDoc) {
        List<FileInfoDTO> newHtmlDocs = this.htmlDocs;
        newHtmlDocs.add(extraHtmlDoc);
        return withHtmlDocs(newHtmlDocs);
    }

    // Использовать значения этого же объекта, но с измененным полем images
    public LLMRequestDTO withImages(List<FileInfoDTO> newImages) {
        return new LLMRequestDTO(
                this.taskId,
                this.modelName,
                this.systemMessage,
                this.userMessage,
                this.temperature,
                this.maxOutputTokens,
                this.htmlDocs,
                newImages
        );
    }

    // Добавить несколько элементов в images
    public LLMRequestDTO addAllToImages(List<FileInfoDTO> extraImages) {
        List<FileInfoDTO> newImages = this.images;
        newImages.addAll(extraImages);
        return withImages(newImages);
    }
}
