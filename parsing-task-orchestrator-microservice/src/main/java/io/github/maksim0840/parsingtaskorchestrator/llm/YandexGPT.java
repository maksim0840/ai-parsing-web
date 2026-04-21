package io.github.maksim0840.parsingtaskorchestrator.llm;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

public class YandexGPT implements LLM {
    private static final String API_KEY = System.getenv("YANDEX_GPT_API_KEY");
    private static final String BASE_URL = System.getenv("YANDEX_GPT_BASE_URL");
    private static final String FOLDER_ID = System.getenv("YANDEX_GPT_FOLDER_ID");
    private static final String MODEL_NAME = System.getenv("YANDEX_GPT_MODEL_NAME");

    private static final Double DEFAULT_TEMPERATURE = Double.valueOf(System.getenv("DEFAULT_TEMPERATURE"));
    private static final Integer DEFAULT_MAX_OUTPUT_TOKENS = Integer.valueOf(System.getenv("DEFAULT_MAX_OUTPUT_TOKENS"));

    private final OpenAIClient client;

    public YandexGPT() {
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(API_KEY)
                .baseUrl(BASE_URL)
                .build();
    }

    @Override
    public String sendRequest(String systemMessage, String userMessage, Double temperature, Integer maxOutputTokens) {
        ResponseCreateParams params = ResponseCreateParams.builder()
                .model("gpt://" + FOLDER_ID + "/" + MODEL_NAME)
                .instructions(systemMessage)
                .input(userMessage)
                .temperature(temperature == null ? DEFAULT_TEMPERATURE : temperature)
                .maxOutputTokens(maxOutputTokens == null ? DEFAULT_MAX_OUTPUT_TOKENS : maxOutputTokens)
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

