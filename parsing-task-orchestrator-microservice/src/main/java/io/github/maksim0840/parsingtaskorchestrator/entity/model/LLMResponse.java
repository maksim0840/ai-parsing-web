package io.github.maksim0840.parsingtaskorchestrator.entity.model;

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
public class LLMResponse {
    private String taskId;
    private boolean success;
    private String message;
    private String llmOutput;
}
