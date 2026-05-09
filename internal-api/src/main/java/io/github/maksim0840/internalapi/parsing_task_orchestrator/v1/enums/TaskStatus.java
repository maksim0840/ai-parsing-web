package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums;

public enum TaskStatus {
    CREATED,
    HTML_PARSING,
    HTML_PREPROCESSING,
    TEXT_RECOGNITION,
    LLM_PROCESSING,
    DONE,
    FAILED
}
