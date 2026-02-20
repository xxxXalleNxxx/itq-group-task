package ru.arapov.itqgrouptask.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "approval_registry")
public class ApprovalRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    Document document;

    @Column(nullable = false)
    String approvedBy;

    @Column(nullable = false)
    LocalDateTime approvedAt;

    @Column(unique = true)
    String registryNumber;
}
