package io.github.maksim0840.parsingtaskorchestrator.domain.model;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto.FileInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
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
    private List<FileInfo> htmlDocs;
    private List<FileInfo> images;
}
