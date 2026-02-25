package ru.arapov.itqgrouptask.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.arapov.itqgrouptask.dto.OperationResult;
import ru.arapov.itqgrouptask.exception.RegistryException;
import ru.arapov.itqgrouptask.model.*;
import ru.arapov.itqgrouptask.repository.ApprovalRegistryRepository;
import ru.arapov.itqgrouptask.repository.DocumentRepository;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAtomicService {

    private final DocumentRepository documentRepository;

    private final ApprovalRegistryRepository approvalRegistryRepository;


    private final HistoryService historyService;

    @Transactional
    public OperationResult submitAtomicDocument(Long id, String initiator, String comment) {
        Document document = documentRepository.findById(id)
                .orElse(null);
        if (document == null) {
            return OperationResult.builder()
                    .id(id)
                    .status(OperationResult.ResultStatus.NOT_FOUND)
                    .message("Документ не найден с id: " + id)
                    .build();
        }

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
        historyService.saveHistory(document, DocumentAction.SUBMIT, initiator, comment);

        log.info("Документ {} успешно отправлен на согласование", id);

        return OperationResult.builder()
                .id(id)
                .status(OperationResult.ResultStatus.SUCCESS)
                .message("Документ успешно отправлен на согласование")
                .build();
    }

    @Transactional
    public OperationResult approveAtomicDocument(Long id, String initiator, String comment) {
        Document document = documentRepository.findById(id)
                .orElse(null);
        if (document == null) {
            return OperationResult.builder()
                    .id(id)
                    .status(OperationResult.ResultStatus.NOT_FOUND)
                    .message("Документ не найден с id: " + id)
                    .build();
        }

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
            historyService.saveHistory(document, DocumentAction.APPROVE, initiator, comment);

            log.info("Документ {} успешно утвержден", id);

            return OperationResult.builder()
                    .id(id)
                    .status(OperationResult.ResultStatus.SUCCESS)
                    .message("Документ успешно утвержден")
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при создании записи в реестре для документа {}: {}", id, e.getMessage());

            OperationResult.ResultStatus status = e instanceof RegistryException
                    ? OperationResult.ResultStatus.REGISTRY_ERROR
                    : OperationResult.ResultStatus.CONFLICT;

            return OperationResult.builder()
                    .id(id)
                    .status(status)
                    .message(e.getMessage())
                    .build();
        }
    }

    private String generateRegistryNumber() {
        String datePart = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniquePart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "REG-" + datePart + "-" + uniquePart;
    }
}
