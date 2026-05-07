package dev.camel.backendlab.scenario.content.dto;

import dev.camel.backendlab.scenario.content.ContentScript;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContentScriptResponse(
        Long id,
        LocalDate targetDate,
        String headlines,
        String script,
        LocalDateTime createdAt
) {
    public static ContentScriptResponse from(ContentScript entity) {
        return new ContentScriptResponse(
                entity.getId(),
                entity.getTargetDate(),
                entity.getHeadlines(),
                entity.getScript(),
                entity.getCreatedAt()
        );
    }
}

