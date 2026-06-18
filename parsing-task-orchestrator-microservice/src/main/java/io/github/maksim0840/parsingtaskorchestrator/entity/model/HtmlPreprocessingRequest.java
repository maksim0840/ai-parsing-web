package io.github.maksim0840.parsingtaskorchestrator.entity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Builder
@Nullable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HtmlPreprocessingRequest {
    private String taskId;
    private List<FileInfo> htmlDocs = new ArrayList<>(); // изначально список пустой, а не null
    private Boolean noscriptProcessing;
    private Boolean linkProcessing;
    private Boolean styleProcessing;
    private Boolean metaProcessing;
    private Boolean scriptProcessing;
    private Boolean canvasProcessing;
    private Boolean svgProcessing;
    private Boolean areaProcessing;
    private Boolean imgProcessing;
    private Boolean videoProcessing;
    private Boolean audioProcessing;
    private Boolean iframeProcessing;
    private Boolean portalProcessing;
    private Boolean embedProcessing;
    private Boolean objectProcessing;
    private Boolean sourceProcessing;
}
