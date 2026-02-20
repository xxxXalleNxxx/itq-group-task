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
@Table(name = "history")
public class HistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    Document document;

    @Column(nullable = false)
    String initiator;

    @Column(nullable = false)
    LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    DocumentAction action;

    String comment;
}
