package io.github.maksim0840.usersinfo.entity.model;

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
public class HtmlParserParams {
    private Boolean downloadImages;
    private Map<String, String> headers;
    private Map<String, String> cookies;
    private Map<String, String> proxy;
    private String pageComplexity;
    private Integer additionalPageLoadTimeoutS;
}
