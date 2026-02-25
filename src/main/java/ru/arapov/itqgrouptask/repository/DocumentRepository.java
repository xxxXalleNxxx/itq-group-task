package ru.arapov.itqgrouptask.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query(value = "SELECT DISTINCT d FROM Document d " +
            "LEFT JOIN FETCH d.history " +
            "WHERE (:status IS NULL OR d.status = :status) AND " +
            "(:author IS NULL OR d.author = :author) AND " +
            "(cast(:fromDate as date) IS NULL OR d.createdAt >= :fromDate) AND " +
            "(cast(:toDate as date) IS NULL OR d.createdAt <= :toDate)",
            countQuery = "SELECT COUNT(d) FROM Document d " +
                    "WHERE (:status IS NULL OR d.status = :status) AND " +
                    "(:author IS NULL OR d.author = :author) AND " +
                    "(cast(:fromDate as date) IS NULL OR d.createdAt >= :fromDate) AND " +
                    "(cast(:toDate as date) IS NULL OR d.createdAt <= :toDate)")
    Page<Document> searchDocuments(@Param("status") DocumentStatus status,
                                              @Param("author") String author,
                                              @Param("fromDate") LocalDateTime fromDate,
                                              @Param("toDate") LocalDateTime toDate,
                                              Pageable pageable);

    @Query(value = "SELECT * FROM documents WHERE status = ?1 ORDER BY created_at LIMIT ?2 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<Document> findAndLockDocuments(String status, int limit);

    @Modifying
    @Query("UPDATE Document d SET d.status = :newStatus WHERE d.id IN :ids")
    int bulkUpdateStatus(@Param("ids") List<Long> ids, @Param("newStatus") DocumentStatus newStatus);
}
