package io.github.maksim0840.apigateway.dto;

public record FileWithContent(
    String fileName,
    byte[] fileBytes
) {
}
