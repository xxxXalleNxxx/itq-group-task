package ru.arapov.itqgrouptask.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.error("не найдено: {}", ex.getMessage());
        return new ErrorResponse(
                "не найдено",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInvalidStatus(InvalidStatusTransitionException ex, WebRequest request) {
        log.error("конфликт/недопустимая операция: {}", ex.getMessage());
        return new ErrorResponse(
                "конфликт/недопустимая операция",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
    }
}
