package io.github.maksim0840.parsingtaskorchestrator.entity.model;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;

@Builder
@Nullable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileInfo {
    private String filePath;
    private String fileName;
    private FileType fileType;
    private Long sizeBytes;
    private String description;
    private boolean isValid;
    private String errorMessage;
}
