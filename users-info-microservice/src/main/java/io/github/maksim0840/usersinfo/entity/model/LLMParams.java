package io.github.maksim0840.usersinfo.entity.model;

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
public class LLMParams {
    private String modelName;
    private String systemMessage;
    private String userMessage;
    private Double temperature;
    private Integer maxOutputTokens;
}
