package ru.arapov.itqgrouptask.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.arapov.itqgrouptask.model.Document;
import ru.arapov.itqgrouptask.model.DocumentStatus;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query("SELECT d FROM Document d WHERE d.id IN :ids")
    List<Document> findAllByIdIn(@Param("ids") List<Long> ids);

    Page<Document> findByStatus(DocumentStatus status, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE " +
            "(:status IS NULL OR d.status = :status) AND " +
            "(:author IS NULL OR d.author = :author) AND " +
            "(:fromDate IS NULL OR d.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR d.createdAt <= :toDate)")
    Page<Document> searchDocuments(
            @Param("status") DocumentStatus status,
            @Param("author") String author,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    long countByStatus(DocumentStatus status);
}
