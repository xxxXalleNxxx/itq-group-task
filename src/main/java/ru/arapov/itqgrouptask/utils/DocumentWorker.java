package ru.arapov.itqgrouptask.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.arapov.itqgrouptask.dto.BulkOperationRequest;
import ru.arapov.itqgrouptask.dto.OperationResult;
import ru.arapov.itqgrouptask.model.Document;
import ru.arapov.itqgrouptask.model.DocumentStatus;
import ru.arapov.itqgrouptask.repository.DocumentRepository;
import ru.arapov.itqgrouptask.service.DocumentService;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentWorker {

    private final DocumentRepository documentRepository;
    private final DocumentService documentService;

    @org.springframework.beans.factory.annotation.Value("${worker.batch-size}")
    private int batchSize;

    @org.springframework.beans.factory.annotation.Value("${worker.submit-enabled}")
    private boolean submitEnabled;

    @org.springframework.beans.factory.annotation.Value("${worker.approve-enabled}")
    private boolean approveEnabled;

    @Scheduled(fixedDelayString = "${worker.submit-interval}")
    public void processSubmitQueue() {
        if (!submitEnabled) {
            log.debug("Submit worker отключен в конфигурации");
            return;
        }

        log.info("=== SUBMIT WORKER ЗАПУЩЕН ===");
        long startTime = System.currentTimeMillis();

        long totalInQueue = documentRepository.countByStatus(DocumentStatus.DRAFT);
        log.info("Всего документов в статусе DRAFT: {}", totalInQueue);

        if (totalInQueue == 0) {
            log.info("Нет документов для обработки. Worker завершен.");
            return;
        }

        int processed = 0;
        int batchNumber = 1;

        while (processed < totalInQueue) {
            long batchStartTime = System.currentTimeMillis();

            Pageable pageable = PageRequest.of(0, batchSize);
            Page<Document> batch = documentRepository.findByStatus(DocumentStatus.DRAFT, pageable);

            if (!batch.hasContent()) {
                break;
            }

            List<Long> ids = batch.getContent().stream()
                    .map(Document::getId)
                    .toList();

            log.info("Пакет #{}: обработка {} документов ({} из {} всего)",
                    batchNumber, ids.size(), processed + ids.size(), totalInQueue);

            BulkOperationRequest request = new BulkOperationRequest(
                    ids,
                    "SYSTEM-WORKER",
                    "Автоматическая отправка на согласование"
            );

            List<OperationResult> results = documentService.submitDocuments(request);

            long successCount = results.stream()
                    .filter(r -> r.status() == OperationResult.ResultStatus.SUCCESS)
                    .count();

            long conflictCount = results.stream()
                    .filter(r -> r.status() == OperationResult.ResultStatus.CONFLICT)
                    .count();

            long batchTime = System.currentTimeMillis() - batchStartTime;

            log.info("Пакет #{} завершен. Успешно: {}, Конфликтов: {}, Время: {} мс",
                    batchNumber, successCount, conflictCount, batchTime);

            processed += ids.size();
            batchNumber++;

            int percentComplete = (int) ((processed * 100) / totalInQueue);
            log.info("Прогресс: {}/{} документов обработано ({}% завершено)",
                    processed, totalInQueue, percentComplete);
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("=== SUBMIT WORKER ЗАВЕРШЕН ===");
        log.info("Итого обработано: {} документов, Общее время: {} мс, Среднее время на документ: {} мс",
                processed, totalTime, processed > 0 ? totalTime / processed : 0);
    }

    @Scheduled(fixedDelayString = "${worker.approve-interval}")
    public void processApproveQueue() {
        if (!approveEnabled) {
            log.debug("Approve worker отключен в конфигурации");
            return;
        }

        log.info("=== APPROVE WORKER ЗАПУЩЕН ===");
        long startTime = System.currentTimeMillis();

        long totalInQueue = documentRepository.countByStatus(DocumentStatus.SUBMITTED);
        log.info("Всего документов в статусе SUBMITTED: {}", totalInQueue);

        if (totalInQueue == 0) {
            log.info("Нет документов для обработки. Worker завершен.");
            return;
        }

        int processed = 0;
        int batchNumber = 1;

        while (processed < totalInQueue) {
            long batchStartTime = System.currentTimeMillis();

            Pageable pageable = PageRequest.of(0, batchSize);
            Page<Document> batch = documentRepository.findByStatus(DocumentStatus.SUBMITTED, pageable);

            if (!batch.hasContent()) {
                break;
            }

            List<Long> ids = batch.getContent().stream()
                    .map(Document::getId)
                    .toList();

            log.info("Пакет #{}: обработка {} документов ({} из {} всего)",
                    batchNumber, ids.size(), processed + ids.size(), totalInQueue);

            BulkOperationRequest request = new BulkOperationRequest(
                    ids,
                    "SYSTEM-WORKER",
                    "Автоматическое утверждение"
            );

            List<OperationResult> results = documentService.approveDocuments(request);

            long successCount = results.stream()
                    .filter(r -> r.status() == OperationResult.ResultStatus.SUCCESS)
                    .count();

            long conflictCount = results.stream()
                    .filter(r -> r.status() == OperationResult.ResultStatus.CONFLICT)
                    .count();

            long registryErrorCount = results.stream()
                    .filter(r -> r.status() == OperationResult.ResultStatus.CONFLICT)
                    .count();

            long batchTime = System.currentTimeMillis() - batchStartTime;

            log.info("Пакет #{} завершен. Успешно: {}, Конфликтов: {}, Ошибок реестра: {}, Время: {} мс",
                    batchNumber, successCount, conflictCount, registryErrorCount, batchTime);

            processed += ids.size();
            batchNumber++;

            int percentComplete = (int) ((processed * 100) / totalInQueue);
            log.info("Прогресс: {}/{} документов обработано ({}% завершено)",
                    processed, totalInQueue, percentComplete);
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("=== APPROVE WORKER ЗАВЕРШЕН ===");
        log.info("Итого обработано: {} документов, Общее время: {} мс, Среднее время на документ: {} мс",
                processed, totalTime, processed > 0 ? totalTime / processed : 0);
    }
}

