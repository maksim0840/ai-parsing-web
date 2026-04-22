package io.github.maksim0840.parsingtaskorchestrator.llm;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.model.Scope;
import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.ChatMessageRole;
import chat.giga.model.completion.CompletionRequest;

import java.util.List;

public class GigaChat implements LLM {
    private static final String AUTH_KEY = System.getenv("GIGACHAT_AUTH_KEY");
    private static final String MODEL_API_NAME = System.getenv("GIGACHAT_MODEL_API_NAME");

    private static final Double DEFAULT_TEMPERATURE = Double.valueOf(System.getenv("DEFAULT_TEMPERATURE"));
    private static final Integer DEFAULT_MAX_OUTPUT_TOKENS = Integer.valueOf(System.getenv("DEFAULT_MAX_OUTPUT_TOKENS"));

    private final GigaChatClient client;

    public GigaChat() {
        this.client = GigaChatClient.builder()
                .verifySslCerts(false)
                .authClient(
                        AuthClient.builder().withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                .scope(Scope.GIGACHAT_API_PERS) // версия API для физических лиц
                                .authKey(AUTH_KEY)
                                .build())
                        .build())
                .build();
    }

    @Override
    public String sendRequest(String systemMessage, String userMessage, Double temperature, Integer maxOutputTokens) {
        List<ChatMessage> messages = List.of(
                ChatMessage.builder()
                        .content(systemMessage)
                        .role(ChatMessageRole.SYSTEM)
                        .build(),
                ChatMessage.builder()
                        .content(userMessage)
                        .role(ChatMessageRole.USER)
                        .build()
        );

        CompletionRequest request = CompletionRequest.builder()
                .model(MODEL_API_NAME)
                .messages(messages)
                .temperature(temperature == null ? DEFAULT_TEMPERATURE.floatValue() : temperature.floatValue())
                .maxTokens(maxOutputTokens == null ? DEFAULT_MAX_OUTPUT_TOKENS : maxOutputTokens)
                .build();

        return client.completions(request)
                .choices().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Response text not found"))
                .message()
                .content();
    }
}
