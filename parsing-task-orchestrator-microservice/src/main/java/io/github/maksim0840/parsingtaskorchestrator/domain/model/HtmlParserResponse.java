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
public class HtmlParserResponse {
    private String taskId;
    private boolean success;
    private String message;
    private List<FileInfo> htmlDocs;
    private List<FileInfo> images;
}
