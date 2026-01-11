package io.github.maksim0840.apigetaway.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DescriptionParams {
    private String url;
    private String jsonStr;

    public DescriptionParams() {}

    public DescriptionParams(String url, String jsonStr) {
        this.url = url;
        this.jsonStr = jsonStr;
    }
}
