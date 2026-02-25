package ru.arapov.itqgrouptask.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.arapov.itqgrouptask.model.Document;
import ru.arapov.itqgrouptask.model.DocumentStatus;
import ru.arapov.itqgrouptask.repository.DocumentRepository;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentWorker {

    private final DocumentRepository documentRepository;

    @org.springframework.beans.factory.annotation.Value("${worker.batch-size}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${worker.submit-interval:60000}")
    @Transactional
    public void processSubmitQueue() {
        List<Document> docs = documentRepository.findAndLockDocuments(
                DocumentStatus.DRAFT.name(), batchSize);

        if (docs.isEmpty()) return;

        List<Long> ids = docs.stream().map(Document::getId).toList();

       int updated = documentRepository.bulkUpdateStatus(ids, DocumentStatus.SUBMITTED);

        log.info("Обработано {} DRAFT документов", updated);
    }

    @Scheduled(fixedDelayString = "${worker.approve-interval:60000}")
    @Transactional
    public void processApproveQueue() {
        List<Document> docs = documentRepository.findAndLockDocuments(
                DocumentStatus.SUBMITTED.name(), batchSize);

        if (docs.isEmpty()) return;

        List<Long> ids = docs.stream().map(Document::getId).toList();

        int updated = documentRepository.bulkUpdateStatus(ids, DocumentStatus.APPROVED);

        log.info("Обработано {} SUBMITTED документов", updated);
    }
}

