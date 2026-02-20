package ru.arapov.itqgrouptask.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.arapov.itqgrouptask.dto.ConcurrentTestResult;
import ru.arapov.itqgrouptask.service.ConcurrentTestService;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class ConcurrentTestController {

    private final ConcurrentTestService testService;

    @PostMapping("/{documentId}")
    public ConcurrentTestResult runConcurrentTest(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "5") int threads,
            @RequestParam(defaultValue = "3") int attempts
    ) {
        return testService.runTest(documentId, threads, attempts);
    }
}
