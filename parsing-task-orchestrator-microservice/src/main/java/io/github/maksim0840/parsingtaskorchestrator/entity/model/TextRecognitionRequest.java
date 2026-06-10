package io.github.maksim0840.parsingtaskorchestrator.entity.model;

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
public class TextRecognitionRequest {
    private String taskId;
    private List<FileInfo> images;
}
