package ru.arapov.itqgrouptask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.arapov.itqgrouptask.dto.BulkOperationRequest;
import ru.arapov.itqgrouptask.dto.DocumentRequest;
import ru.arapov.itqgrouptask.dto.DocumentResponse;
import ru.arapov.itqgrouptask.dto.OperationResult;
import ru.arapov.itqgrouptask.model.Document;
import ru.arapov.itqgrouptask.model.DocumentStatus;
import ru.arapov.itqgrouptask.repository.ApprovalRegistryRepository;
import ru.arapov.itqgrouptask.repository.DocumentRepository;
import ru.arapov.itqgrouptask.service.DocumentService;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DocumentServiceTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ApprovalRegistryRepository registryRepository;

    private final AtomicLong counter = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        registryRepository.deleteAll();
        documentRepository.deleteAll();
    }

    @Test
    @DisplayName("Happy path всех статусов")
    void testHappyPathOneDocument() {
        DocumentRequest createReq = new DocumentRequest("Автор", "Документ", "Инициатор");
        DocumentResponse created = documentService.createDocument(createReq);

        assertEquals("DRAFT", created.status().toString());
        assertNotNull(created.id());
        assertNotNull(created.documentNumber());

        BulkOperationRequest submitReq = new BulkOperationRequest(
                List.of(created.id()), "Инициатор", "комментарий"
        );
        List<OperationResult> submitResults = documentService.submitDocuments(submitReq);

        assertEquals(1, submitResults.size());
        assertEquals(OperationResult.ResultStatus.SUCCESS, submitResults.getFirst().status());

        BulkOperationRequest approveReq = new BulkOperationRequest(
                List.of(created.id()), "Инициатор", "комментарий"
        );
        List<OperationResult> approveResults = documentService.approveDocuments(approveReq);

        assertEquals(1, approveResults.size());
        assertEquals(OperationResult.ResultStatus.SUCCESS, approveResults.getFirst().status());

        Document finalDoc = documentRepository.findById(created.id()).get();
        assertEquals(DocumentStatus.APPROVED, finalDoc.getStatus());
        assertTrue(registryRepository.existsByDocumentId(created.id()));
    }

    @Test
    @DisplayName("Пакетное рассмотрение с частичными результатами")
    void testBulkSubmitWithPartialResults() {
        Document doc1 = createDoc(DocumentStatus.DRAFT);
        Document doc2 = createDoc(DocumentStatus.DRAFT);
        Document doc3 = createDoc(DocumentStatus.APPROVED);

        BulkOperationRequest request = new BulkOperationRequest(
                List.of(doc1.getId(), doc2.getId(), doc3.getId(), 9999L),
                "arapov",
                "test"
        );

        List<OperationResult> results = documentService.submitDocuments(request);

        assertEquals(4, results.size());
        assertEquals(OperationResult.ResultStatus.SUCCESS, results.get(0).status());
        assertEquals(OperationResult.ResultStatus.SUCCESS, results.get(1).status());
        assertEquals(OperationResult.ResultStatus.CONFLICT, results.get(2).status());
    }

    @Test
    @DisplayName("Пакетный approve с частичными результатами (успех/конфликт)")
    void testBulkApproveWithPartialResults() {
        Document doc1 = createDoc(DocumentStatus.SUBMITTED);
        Document doc2 = createDoc(DocumentStatus.SUBMITTED);
        Document doc3 = createDoc(DocumentStatus.DRAFT);

        BulkOperationRequest request = new BulkOperationRequest(
                List.of(doc1.getId(), doc2.getId(), doc3.getId()),
                "arapov",
                "test"
        );

        List<OperationResult> results = documentService.approveDocuments(request);

        assertEquals(3, results.size());
        assertEquals(OperationResult.ResultStatus.SUCCESS, results.get(0).status());
        assertEquals(OperationResult.ResultStatus.SUCCESS, results.get(1).status());
        assertEquals(OperationResult.ResultStatus.CONFLICT, results.get(2).status());

        assertTrue(registryRepository.existsByDocumentId(doc1.getId()));
        assertTrue(registryRepository.existsByDocumentId(doc2.getId()));
        assertFalse(registryRepository.existsByDocumentId(doc3.getId()));
    }

    @Test
    @DisplayName("Откат approve при ошибке записи в регистр")
    void testApproveRollbackOnRegistryError() {
        Document doc = createDoc(DocumentStatus.SUBMITTED);

        BulkOperationRequest request1 = new BulkOperationRequest(
                List.of(doc.getId()),
                "arapov",
                "first approve"
        );

        List<OperationResult> results1 = documentService.approveDocuments(request1);
        assertEquals(OperationResult.ResultStatus.SUCCESS, results1.getFirst().status());

        BulkOperationRequest request2 = new BulkOperationRequest(
                List.of(doc.getId()),
                "arapov",
                "second approve"
        );

        List<OperationResult> results2 = documentService.approveDocuments(request2);

        assertEquals(1, results2.size());
        assertEquals(OperationResult.ResultStatus.CONFLICT, results2.getFirst().status());

        Document updated = documentRepository.findById(doc.getId()).get();
        assertEquals(DocumentStatus.APPROVED, updated.getStatus());
    }

    private Document createDoc(DocumentStatus status) {
        Document doc = new Document();
        doc.setAuthor("Arapov");
        doc.setTitle("Test");
        doc.setStatus(status);
        doc.setInitiator("arapov");
        doc.setDocumentNumber("DOC-" + System.currentTimeMillis() + "-" + counter.getAndIncrement());
        return documentRepository.save(doc);
    }
}
