package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto;

import lombok.Builder;

import javax.annotation.Nullable;
import java.util.List;

@Builder
@Nullable
public record HtmlPreprocessingRequestDTO(
        String taskId,
        List<String> htmlPaths,
        Boolean noscriptProcessing,
        Boolean linkProcessing,
        Boolean styleProcessing,
        Boolean metaProcessing,
        Boolean scriptProcessing,
        Boolean canvasProcessing,
        Boolean svgProcessing,
        Boolean areaProcessing,
        Boolean imgProcessing,
        Boolean videoProcessing,
        Boolean audioProcessing,
        Boolean iframeProcessing,
        Boolean portalProcessing,
        Boolean embedProcessing,
        Boolean objectProcessing,
        Boolean sourceProcessing
) {
    // Использовать значения этого же объекта, но с измененным полем htmlPaths
    public HtmlPreprocessingRequestDTO withHtmlPaths(List<String> newHtmlPaths) {
        return new HtmlPreprocessingRequestDTO(
                this.taskId,
                newHtmlPaths,
                this.noscriptProcessing,
                this.linkProcessing,
                this.styleProcessing,
                this.metaProcessing,
                this.scriptProcessing,
                this.canvasProcessing,
                this.svgProcessing,
                this.areaProcessing,
                this.imgProcessing,
                this.videoProcessing,
                this.audioProcessing,
                this.iframeProcessing,
                this.portalProcessing,
                this.embedProcessing,
                this.objectProcessing,
                this.sourceProcessing
        );
    }

    // Добавить несколько элементов в htmlPaths
    public HtmlPreprocessingRequestDTO addAllToHtmlPaths(List<String> extraHtmlPaths) {
        List<String> newHtmlPaths = this.htmlPaths;
        newHtmlPaths.addAll(extraHtmlPaths);
        return withHtmlPaths(newHtmlPaths);
    }

    // Добавить один элемент в htmlPaths
    public HtmlPreprocessingRequestDTO addToHtmlPaths(String extraHtmlPath) {
        List<String> newHtmlPaths = this.htmlPaths;
        newHtmlPaths.add(extraHtmlPath);
        return withHtmlPaths(newHtmlPaths);
    }
}
