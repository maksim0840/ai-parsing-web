package io.github.maksim0840.parsingtaskorchestrator.entity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.util.Map;

@Builder
@Nullable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HtmlParserRequest {
    private String taskId;
    private String url;
    private String htmlOutDir;
    private String imagesOutDir;
    private Boolean downloadImages;
    private Map<String, String> headers;
    private Map<String, String> cookies;
    private Map<String, String> proxy;
    private String pageComplexity;
    private Integer additionalPageLoadTimeoutS;
}
