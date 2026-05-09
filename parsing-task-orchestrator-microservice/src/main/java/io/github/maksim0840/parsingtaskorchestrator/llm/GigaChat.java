package io.github.maksim0840.parsingtaskorchestrator.llm;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.model.Scope;
import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.ChatMessageRole;
import chat.giga.model.completion.CompletionRequest;
import io.github.maksim0840.parsingtaskorchestrator.config.properties.LLMProperties;

import java.util.List;

public class GigaChat implements LLM {
    private final LLMProperties llmProperties;
    private final GigaChatClient client;

    public GigaChat(LLMProperties llmProperties) {
        this.llmProperties = llmProperties;
        this.client = GigaChatClient.builder()
                .verifySslCerts(false)
                .authClient(
                        AuthClient.builder().withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                .scope(Scope.GIGACHAT_API_PERS) // версия API для физических лиц
                                .authKey(llmProperties.gigachatAuthKey())
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
                .model(llmProperties.gigachatModelApiName())
                .messages(messages)
                .temperature(temperature == null ? llmProperties.defaultTemperature().floatValue() : temperature.floatValue())
                .maxTokens(maxOutputTokens == null ? llmProperties.defaultMaxOutputTokens() : maxOutputTokens)
                .build();

        return client.completions(request)
                .choices().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Response text not found"))
                .message()
                .content();
    }
}
