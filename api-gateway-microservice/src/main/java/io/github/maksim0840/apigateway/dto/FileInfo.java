package io.github.maksim0840.apigateway.dto;

public record FileInfo(
    String fileName,
    byte[] fileBytes
) {
}
