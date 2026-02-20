package ru.arapov.itqgrouptask.dto;

import lombok.Builder;
import ru.arapov.itqgrouptask.model.DocumentStatus;

@Builder
public record ConcurrentTestResult(
        Long documentId,
        int successfulAttempts,
        int conflictAttempts,
        int errorAttempts,
        DocumentStatus finalStatus,
        boolean registryCreated,
        long totalTime
) {
}
