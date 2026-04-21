package io.github.maksim0840.parsingtaskorchestrator.llm;

public interface LLM {
    String sendRequest(String systemMessage, String userMessage, Double temperature, Integer maxOutputTokens);
}
