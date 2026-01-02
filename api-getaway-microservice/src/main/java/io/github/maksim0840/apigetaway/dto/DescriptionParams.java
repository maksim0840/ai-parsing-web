package io.github.maksim0840.apigetaway.dto;

public class DescriptionParams {
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public DescriptionParams() {}

    public DescriptionParams(String url) {
        this.url = url;
    }
}
