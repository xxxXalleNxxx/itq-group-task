package ru.arapov.itqgrouptask.dto;

import lombok.Builder;

@Builder
public record OperationResult(
        Long id,
        ResultStatus status,
        String message
) {
    public enum ResultStatus {
        SUCCESS,
        CONFLICT,
        NOT_FOUND,
        REGISTRY_ERROR
    }
}
