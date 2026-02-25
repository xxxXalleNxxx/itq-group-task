package ru.arapov.itqgrouptask.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.arapov.itqgrouptask.model.Document;
import ru.arapov.itqgrouptask.model.DocumentAction;
import ru.arapov.itqgrouptask.model.HistoryEntry;
import ru.arapov.itqgrouptask.repository.HistoryRepository;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final HistoryRepository historyRepository;

    public void saveHistory(Document document, DocumentAction action, String initiator, String comment) {
        HistoryEntry history = new HistoryEntry();
        history.setDocument(document);
        history.setAction(action);
        history.setInitiator(initiator);
        history.setTimestamp(LocalDateTime.now());
        history.setComment(comment != null ? comment : "");

        historyRepository.save(history);
    }
}
