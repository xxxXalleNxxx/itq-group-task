package ru.arapov.itqgrouptask.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.arapov.itqgrouptask.dto.BulkOperationRequest;
import ru.arapov.itqgrouptask.dto.DocumentRequest;
import ru.arapov.itqgrouptask.dto.DocumentResponse;
import ru.arapov.itqgrouptask.dto.OperationResult;
import ru.arapov.itqgrouptask.exception.RegistryException;
import ru.arapov.itqgrouptask.exception.ResourceNotFoundException;
import ru.arapov.itqgrouptask.model.*;
import ru.arapov.itqgrouptask.repository.ApprovalRegistryRepository;
import ru.arapov.itqgrouptask.repository.DocumentRepository;
import ru.arapov.itqgrouptask.repository.HistoryRepository;

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

    private final ApprovalRegistryRepository approvalRegistryRepository;

    private final HistoryRepository historyRepository;

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

        saveHistory(savedDocument, DocumentAction.CREATE, request.initiator(),
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

    @Transactional
    public List<OperationResult> submitDocuments(BulkOperationRequest request) {
        log.info("Пакетная отправка на согласование. Количество документов: {}, Инициатор: {}",
                request.ids().size(), request.initiator());

        long batchStartTime = System.currentTimeMillis();
        List<OperationResult> results = new ArrayList<>();

        for (Long id : request.ids()) {
            long docStartTime = System.currentTimeMillis();

            try {
                OperationResult result = submitDocument(id, request.initiator(), request.comment());
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

    @Transactional
    public OperationResult submitDocument(Long id, String initiator, String comment) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Документ не найден с id: " + id));

        if (document.getStatus() != DocumentStatus.DRAFT) {
            log.warn("Недопустимый переход статуса. Документ ID: {}, Текущий статус: {}, Ожидаемый: DRAFT",
                    id, document.getStatus());

            return OperationResult.builder()
                    .id(id)
                    .status(OperationResult.ResultStatus.CONFLICT)
                    .message(String.format("Документ в статусе %s. Ожидался статус DRAFT",
                            document.getStatus()))
                    .build();
        }

        document.setStatus(DocumentStatus.SUBMITTED);
        documentRepository.save(document);

        saveHistory(document, DocumentAction.SUBMIT, initiator, comment);

        log.info("Документ {} успешно отправлен на согласование", id);

        return OperationResult.builder()
                .id(id)
                .status(OperationResult.ResultStatus.SUCCESS)
                .message("Документ успешно отправлен на согласование")
                .build();
    }

    @Transactional
    public List<OperationResult> approveDocuments(BulkOperationRequest request) {
        log.info("Пакетное утверждение документов. Количество: {}, Инициатор: {}",
                request.ids().size(), request.initiator());

        long batchStartTime = System.currentTimeMillis();
        List<OperationResult> results = new ArrayList<>();

        for (Long id : request.ids()) {
            long docStartTime = System.currentTimeMillis();

            try {
                OperationResult result = approveDocument(id, request.initiator(), request.comment());
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

    @Transactional
    public OperationResult approveDocument(Long id, String initiator, String comment) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Документ не найден с id: " + id));

        if (document.getStatus() != DocumentStatus.SUBMITTED) {
            log.warn("Недопустимый переход статуса. Документ ID: {}, Текущий статус: {}, Ожидаемый: SUBMITTED",
                    id, document.getStatus());

            return OperationResult.builder()
                    .id(id)
                    .status(OperationResult.ResultStatus.CONFLICT)
                    .message(String.format("Документ в статусе %s. Ожидался статус SUBMITTED",
                            document.getStatus()))
                    .build();
        }

        try {
            ApprovalRegistry registry = new ApprovalRegistry();
            registry.setDocument(document);
            registry.setApprovedBy(initiator);
            registry.setApprovedAt(LocalDateTime.now());
            registry.setRegistryNumber(generateRegistryNumber());

            approvalRegistryRepository.save(registry);
            log.info("Запись в реестре утверждений создана. Документ ID: {}, Номер в реестре: {}",
                    id, registry.getRegistryNumber());

            document.setStatus(DocumentStatus.APPROVED);
            documentRepository.save(document);

            saveHistory(document, DocumentAction.APPROVE, initiator, comment);

            log.info("Документ {} успешно утвержден", id);

            return OperationResult.builder()
                    .id(id)
                    .status(OperationResult.ResultStatus.SUCCESS)
                    .message("Документ успешно утвержден")
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при создании записи в реестре для документа {}: {}", id, e.getMessage());

            throw new RegistryException("Не удалось создать запись в реестре утверждений: " + e.getMessage());
        }
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

    private String generateRegistryNumber() {
        String datePart = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniquePart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "REG-" + datePart + "-" + uniquePart;
    }

    private void saveHistory(Document document, DocumentAction action, String initiator, String comment) {
        HistoryEntry history = new HistoryEntry();
        history.setDocument(document);
        history.setAction(action);
        history.setInitiator(initiator);
        history.setTimestamp(LocalDateTime.now());
        history.setComment(comment != null ? comment : "");

        historyRepository.save(history);
    }
}
