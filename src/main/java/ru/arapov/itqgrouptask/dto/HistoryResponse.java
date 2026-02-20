package ru.arapov.itqgrouptask.dto;

import ru.arapov.itqgrouptask.model.DocumentAction;
import ru.arapov.itqgrouptask.model.HistoryEntry;

import java.time.LocalDateTime;

public record HistoryResponse(
        Long id,
        String initiator,
        LocalDateTime timestamp,
        DocumentAction action,
        String comment
) {
    public static HistoryResponse from(HistoryEntry history) {
        return new HistoryResponse(
                history.getId(),
                history.getInitiator(),
                history.getTimestamp(),
                history.getAction(),
                history.getComment()
        );
    }
}
