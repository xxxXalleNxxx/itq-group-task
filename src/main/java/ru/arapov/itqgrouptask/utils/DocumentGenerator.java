package ru.arapov.itqgrouptask.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.arapov.itqgrouptask.dto.DocumentRequest;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

@Component
@Slf4j
public class DocumentGenerator implements CommandLineRunner {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${generator.api-url:http://localhost:8080/api/documents}")
    private String apiUrl;

    @Override
    public void run(String... args) {
        for (String arg : args) {
            if ("--generate".equals(arg)) {
                generate();
                return;
            }
        }
    }

    private void generate() {
        log.info("=== ГЕНЕРАЦИЯ ДОКУМЕНТОВ ===");

        try {
            Properties props = new Properties();
            try (InputStream input = new FileInputStream("generator.properties")) {
                props.load(input);
            }

            int total = Integer.parseInt(props.getProperty("count", "10"));
            log.info("Всего документов для создания: {}", total);

            long startTime = System.currentTimeMillis();
            int success = 0;

            for (int i = 0; i < total; i++) {
                try {
                    DocumentRequest request = new DocumentRequest(
                            "Generator",
                            "Документ " + (i + 1),
                            "SYSTEM"
                    );

                    restTemplate.postForObject(apiUrl, request, Object.class);
                    success++;

                    log.info("Создан {}/{}", i + 1, total);

                } catch (Exception e) {
                    log.error("Ошибка создания документа {}/{}: {}", i + 1, total, e.getMessage());
                }
            }

            long time = System.currentTimeMillis() - startTime;
            log.info("=== ГОТОВО ===");
            log.info("Создано: {}/{}", success, total);
            log.info("Время: {} мс", time);

        } catch (Exception e) {
            log.error("Ошибка: {}", e.getMessage());
        }
    }
}
