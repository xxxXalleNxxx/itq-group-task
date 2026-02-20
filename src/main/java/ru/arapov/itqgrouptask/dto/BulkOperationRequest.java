package ru.arapov.itqgrouptask.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;


public record BulkOperationRequest(

        @NotEmpty(message = "Список айди не может быть пустым")
        @Size(max = 1000, message = "Нельзя обрабатывать больше, чем 1000 документов за раз")
        List<Long> ids,

        @NotBlank(message = "Поле инициатора пропущено")
        String initiator,

        @Size(max = 500, message = "Комментарий не может содержать более 500 символов")
        String comment
) {
}
