package ru.arapov.itqgrouptask.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.arapov.itqgrouptask.dto.BulkOperationRequest;
import ru.arapov.itqgrouptask.dto.DocumentRequest;
import ru.arapov.itqgrouptask.dto.DocumentResponse;
import ru.arapov.itqgrouptask.dto.OperationResult;
import ru.arapov.itqgrouptask.exception.ResourceNotFoundException;
import ru.arapov.itqgrouptask.model.*;
import ru.arapov.itqgrouptask.repository.DocumentRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;

    private final HistoryService historyService;

    private final DocumentAtomicService documentAtomicService;

    public DocumentResponse createDocument(DocumentRequest request) {
        log.info("Начало создания документа. Автор: {}, Название: {}",
                request.author(), request.title());

        long startTime = System.currentTimeMillis();

        Document document = new Document();
        document.setAuthor(request.author());
        document.setTitle(request.title());
        document.setStatus(DocumentStatus.DRAFT);
        document.setInitiator(request.initiator());

        String documentNumber = generateDocumentNumber();
        document.setDocumentNumber(documentNumber);
        log.info("Сгенерирован номер документа: {}", documentNumber);

        Document savedDocument = documentRepository.save(document);

        historyService.saveHistory(savedDocument, DocumentAction.CREATE, request.initiator(),
                "Документ создан в статусе 'DRAFT'");

        long executionTime = System.currentTimeMillis() - startTime;
        log.info("Документ успешно создан. ID: {}, Номер: {}, Время: {} мс",
                savedDocument.getId(), savedDocument.getDocumentNumber(), executionTime);

        return DocumentResponse.from(savedDocument);
    }

    public DocumentResponse getDocumentWithHistory(Long id) {
        log.info("Получение документа с историей. ID: {}", id);

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Документ не найден. ID: {}", id);
                    return new ResourceNotFoundException("Документ не найден с id: " + id);
                });

        return DocumentResponse.from(document);
    }

    public List<DocumentResponse> getDocumentsByIds(List<Long> ids) {
        log.info("Пакетное получение документов. Количество ID: {}", ids.size());

        List<Document> documents = documentRepository.findAllByIdIn(ids);

        log.info("Найдено документов: {} из {}", documents.size(), ids.size());

        return documents.stream()
                .map(DocumentResponse::from)
                .toList();
    }

    public List<OperationResult> submitDocuments(BulkOperationRequest request) {
        log.info("Пакетная отправка на согласование. Количество документов: {}, Инициатор: {}",
                request.ids().size(), request.initiator());

        long batchStartTime = System.currentTimeMillis();
        List<OperationResult> results = new ArrayList<>();

        for (Long id : request.ids()) {
            long docStartTime = System.currentTimeMillis();

            try {
                OperationResult result = documentAtomicService.submitAtomicDocument(id, request.initiator(), request.comment());
                results.add(result);

                log.debug("Документ {} обработан за {} мс. Результат: {}",
                        id, System.currentTimeMillis() - docStartTime, result.status());

            } catch (Exception e) {
                log.error("Ошибка при обработке документа {}: {}", id, e.getMessage());
                results.add(OperationResult.builder()
                        .id(id)
                        .status(OperationResult.ResultStatus.CONFLICT)
                        .message("Внутренняя ошибка сервера")
                        .build());
            }
        }

        long batchTime = System.currentTimeMillis() - batchStartTime;
        log.info("Пакетная отправка завершена. Всего: {}, Успешно: {}, Время: {} мс",
                results.size(),
                results.stream().filter(r -> r.status() == OperationResult.ResultStatus.SUCCESS).count(),
                batchTime);

        return results;
    }

    public List<OperationResult> approveDocuments(BulkOperationRequest request) {
        log.info("Пакетное утверждение документов. Количество: {}, Инициатор: {}",
                request.ids().size(), request.initiator());

        long batchStartTime = System.currentTimeMillis();
        List<OperationResult> results = new ArrayList<>();

        for (Long id : request.ids()) {
            long docStartTime = System.currentTimeMillis();

            try {
                OperationResult result = documentAtomicService.approveAtomicDocument(id, request.initiator(), request.comment());
                results.add(result);

                log.debug("Документ {} обработан за {} мс. Результат: {}",
                        id, System.currentTimeMillis() - docStartTime, result.status());

            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("Ошибка при обработке документа {}: {}", id, e.getMessage());
                }
                results.add(OperationResult.builder()
                        .id(id)
                        .status(OperationResult.ResultStatus.CONFLICT)
                        .message("Внутренняя ошибка сервера")
                        .build());
            }
        }

        long batchTime = System.currentTimeMillis() - batchStartTime;
        log.info("Пакетное утверждение завершено. Всего: {}, Успешно: {}, Время: {} мс",
                results.size(),
                results.stream().filter(r -> r.status() == OperationResult.ResultStatus.SUCCESS).count(),
                batchTime);

        return results;
    }

    public Page<DocumentResponse> searchDocuments(DocumentStatus status, String author,
                                                  LocalDateTime fromDate, LocalDateTime toDate,
                                                  Pageable pageable) {
        log.info("Поиск документов. Статус: {}, Автор: {}, Дата с: {}, Дата по: {}",
                status, author, fromDate, toDate);

        long startTime = System.currentTimeMillis();

        Page<Document> documents = documentRepository.searchDocuments(
                status, author, fromDate, toDate, pageable);

        log.info("Поиск завершен. Найдено документов: {}, Время: {} мс",
                documents.getTotalElements(), System.currentTimeMillis() - startTime);

        return documents.map(DocumentResponse::from);
    }

    private String generateDocumentNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniquePart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "DOC-" + datePart + "-" + uniquePart;
    }
}
