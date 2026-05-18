package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto;

import lombok.Builder;

import javax.annotation.Nullable;
import java.util.List;

@Builder
@Nullable
public record TextRecognitionRequestDTO(
        String taskId,
        List<FileInfoDTO> images
) {
    // Использовать значения этого же объекта, но с измененным полем images
    public TextRecognitionRequestDTO withImages(List<FileInfoDTO> newImages) {
        return new TextRecognitionRequestDTO(
                this.taskId,
                newImages
        );
    }

    // Добавить несколько элементов в images
    public TextRecognitionRequestDTO addAllToImages(List<FileInfoDTO> extraImages) {
        List<FileInfoDTO> newImages = this.images;
        newImages.addAll(extraImages);
        return withImages(newImages);
    }

    // Добавить один элемент в images
    public TextRecognitionRequestDTO addToImages(FileInfoDTO extraImages) {
        List<FileInfoDTO> newImages = this.images;
        newImages.add(extraImages);
        return withImages(newImages);
    }
}
