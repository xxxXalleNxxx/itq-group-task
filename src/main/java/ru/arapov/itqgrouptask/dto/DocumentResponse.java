package ru.arapov.itqgrouptask.dto;

import ru.arapov.itqgrouptask.model.Document;
import ru.arapov.itqgrouptask.model.DocumentStatus;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record DocumentResponse(
        Long id,
        String documentNumber,
        String author,
        String title,
        DocumentStatus status,
        String initiator,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<HistoryResponse> history
) {
    public static DocumentResponse from(Document document) {
        List<HistoryResponse> historyList = document.getHistory() != null
                ? document.getHistory().stream()
                .map(HistoryResponse::from)
                .collect(Collectors.toList())
                : Collections.emptyList();


        return new DocumentResponse(
                document.getId(),
                document.getDocumentNumber(),
                document.getAuthor(),
                document.getTitle(),
                document.getStatus(),
                document.getInitiator(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                historyList
        );
    }
}
