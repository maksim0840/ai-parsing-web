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
public class LLMRequest {
    private String taskId;
    private String modelName;
    private String systemMessage;
    private String userMessage;
    private Double temperature;
    private Integer maxOutputTokens;
    private List<FileInfo> htmlDocs = new ArrayList<>(); // изначально список пустой, а не null
    private List<FileInfo> images = new ArrayList<>(); // изначально список пустой, а не null
}
