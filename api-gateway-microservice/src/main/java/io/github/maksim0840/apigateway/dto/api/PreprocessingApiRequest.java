package io.github.maksim0840.apigateway.dto.api;

public record PreprocessingApiRequest(
        boolean noscript,
        boolean link,
        boolean style,
        boolean meta,
        boolean script,
        boolean canvas,
        boolean svg,
        boolean area,
        boolean img,
        boolean video,
        boolean audio,
        boolean iframe,
        boolean portal,
        boolean embed,
        boolean object,
        boolean source
) {
}
