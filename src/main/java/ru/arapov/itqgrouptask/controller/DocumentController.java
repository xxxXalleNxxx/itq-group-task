package ru.arapov.itqgrouptask.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.arapov.itqgrouptask.dto.BulkOperationRequest;
import ru.arapov.itqgrouptask.dto.DocumentRequest;
import ru.arapov.itqgrouptask.dto.DocumentResponse;
import ru.arapov.itqgrouptask.dto.OperationResult;
import ru.arapov.itqgrouptask.model.DocumentStatus;
import ru.arapov.itqgrouptask.service.DocumentService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(@Valid @RequestBody DocumentRequest request) {
        DocumentResponse response = documentService.createDocument(request);

        log.info("Документ создан. ID: {}, Номер: {}", response.id(), response.documentNumber());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable Long id) {
        DocumentResponse response = documentService.getDocumentWithHistory(id);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<DocumentResponse>> getDocumentsBatch(@RequestBody List<Long> ids) {
        List<DocumentResponse> responses = documentService.getDocumentsByIds(ids);

        log.info("Найдено документов: {}", responses.size());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/submit")
    public ResponseEntity<List<OperationResult>> submitDocuments(@Valid @RequestBody BulkOperationRequest request) {
        List<OperationResult> results = documentService.submitDocuments(request);

        long successCount = results.stream()
                .filter(r -> r.status() == OperationResult.ResultStatus.SUCCESS)
                .count();

        log.info("Отправка на согласование завершена. Успешно: {}, Всего: {}", successCount, results.size());
        return ResponseEntity.ok(results);
    }

    @PostMapping("/approve")
    public ResponseEntity<List<OperationResult>> approveDocuments(@Valid @RequestBody BulkOperationRequest request) {
        List<OperationResult> results = documentService.approveDocuments(request);

        long successCount = results.stream()
                .filter(r -> r.status() == OperationResult.ResultStatus.SUCCESS)
                .count();

        log.info("Утверждение завершено. Успешно: {}, Всего: {}", successCount, results.size());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<DocumentResponse>> searchDocuments(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<DocumentResponse> page = documentService.searchDocuments(status, author, fromDate, toDate, pageable);

        log.info("Поиск завершен. Найдено элементов: {}, Всего страниц: {}",
                page.getNumberOfElements(), page.getTotalPages());

        return ResponseEntity.ok(page);
    }

}
