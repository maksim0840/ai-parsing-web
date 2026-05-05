package io.github.maksim0840.apigateway.dto.api;

public record PipelineApiRequest(
        ParsingApiRequest parsing,
        PreprocessingApiRequest preprocessing,
        RecognitionApiRequest recognition,
        LLMApiRequest llm
) {
}
