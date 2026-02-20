package ru.arapov.itqgrouptask.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentRequest(
        @NotBlank(message = "Поле автора пропущено")
        @Size(min = 2, max = 25, message = "Имя автора должно быть от 2 до 25 символов")
        String author,

        @NotBlank(message = "Поле заголовка пропущено")
        @Size(min = 1, max = 255, message = "Заголовок должен содержать от 1 до 255 символов")
        String title,

        @NotBlank(message = "Поле инициатора пропущено")
        String initiator
) {
}
