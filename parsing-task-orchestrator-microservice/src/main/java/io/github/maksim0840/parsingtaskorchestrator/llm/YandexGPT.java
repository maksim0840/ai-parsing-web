package io.github.maksim0840.parsingtaskorchestrator.llm;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import io.github.maksim0840.parsingtaskorchestrator.config.properties.LLMProperties;

public class YandexGPT implements LLM {
    private final LLMProperties llmProperties;
    private final OpenAIClient client;

    public YandexGPT(LLMProperties llmProperties) {
        this.llmProperties = llmProperties;
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(llmProperties.yandexgptApiKey())
                .baseUrl(llmProperties.yandexgptBaseUrl())
                .build();
    }

    @Override
    public String sendRequest(String systemMessage, String userMessage, Double temperature, Integer maxOutputTokens) {
        ResponseCreateParams params = ResponseCreateParams.builder()
                .model("gpt://" + llmProperties.yandexgptFolderId() + "/" + llmProperties.yandexgptModelApiName())
                .instructions(systemMessage)
                .input(userMessage)
                .temperature(temperature == null ? llmProperties.defaultTemperature() : temperature)
                .maxOutputTokens(maxOutputTokens == null ? llmProperties.defaultMaxOutputTokens() : maxOutputTokens)
                .build();

        Response response = client.responses().create(params);

        return response.output().stream()
                .flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(outputText -> outputText.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Response text not found"));
    }
}

