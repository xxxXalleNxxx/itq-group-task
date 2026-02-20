package ru.arapov.itqgrouptask.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.arapov.itqgrouptask.dto.BulkOperationRequest;
import ru.arapov.itqgrouptask.dto.ConcurrentTestResult;
import ru.arapov.itqgrouptask.dto.OperationResult;
import ru.arapov.itqgrouptask.model.Document;
import ru.arapov.itqgrouptask.model.DocumentStatus;
import ru.arapov.itqgrouptask.repository.ApprovalRegistryRepository;
import ru.arapov.itqgrouptask.repository.DocumentRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConcurrentTestService {

    private final DocumentRepository documentRepository;
    private final ApprovalRegistryRepository approvalRegistryRepository;
    private final DocumentService documentService;

    public ConcurrentTestResult runTest(Long documentId, int threads, int attempts) {
        log.info("Запуск теста: документ {}, потоки {}, попыток {}", documentId, threads, attempts);

        Document doc = documentRepository.findById(documentId).orElseThrow();
        doc.setStatus(DocumentStatus.SUBMITTED);
        documentRepository.save(doc);
        log.info("Документ сброшен в SUBMITTED");

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger conflict = new AtomicInteger(0);
        AtomicInteger error = new AtomicInteger(0);

        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                try {
                    startSignal.await();
                    for (int a = 0; a < attempts; a++) {
                        try {
                            BulkOperationRequest request = new BulkOperationRequest(
                            List.of(documentId),
                            "test",
                            "concurrent test"
                            );

                            List<OperationResult> results = documentService.approveDocuments(request);
                            OperationResult result = results.getFirst();

                            switch (result.status()) {
                                case SUCCESS -> success.incrementAndGet();
                                case CONFLICT -> conflict.incrementAndGet();
                                default -> error.incrementAndGet();
                            }
                        } catch (Exception e) {
                            error.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    log.error("Ошибка в потоке", e);
                } finally {
                    doneSignal.countDown();
                }
            }).start();
        }

        startSignal.countDown();

        try {
            doneSignal.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Document finalDoc = documentRepository.findById(documentId).orElseThrow();
        boolean registryExists = approvalRegistryRepository.existsByDocumentId(documentId);

        log.info("Результаты: успех={}, конфликт={}, ошибки={}, статус={}, реестр={}",
                success.get(), conflict.get(), error.get(), finalDoc.getStatus(), registryExists);

        return ConcurrentTestResult.builder()
                .documentId(documentId)
                .successfulAttempts(success.get())
                .conflictAttempts(conflict.get())
                .errorAttempts(error.get())
                .finalStatus(finalDoc.getStatus())
                .registryCreated(registryExists)
                .build();
    }
}
