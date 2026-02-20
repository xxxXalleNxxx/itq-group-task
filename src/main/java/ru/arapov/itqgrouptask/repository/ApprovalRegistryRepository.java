package ru.arapov.itqgrouptask.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.arapov.itqgrouptask.model.ApprovalRegistry;

@Repository
public interface ApprovalRegistryRepository extends JpaRepository<ApprovalRegistry, Long> {
    boolean existsByDocumentId(Long documentId);
}
