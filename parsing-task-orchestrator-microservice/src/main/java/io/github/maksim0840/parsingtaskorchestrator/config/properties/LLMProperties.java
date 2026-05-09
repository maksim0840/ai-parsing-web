package io.github.maksim0840.parsingtaskorchestrator.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LLMProperties(
    String yandexgptModelApiName,
    String yandexgptModelViewName,
    String yandexgptApiKey,
    String yandexgptFolderId,
    String yandexgptBaseUrl,
    String gigachatModelApiName,
    String gigachatModelViewName,
    String gigachatAuthKey,
    Double defaultTemperature,
    Integer defaultMaxOutputTokens
) {
}
