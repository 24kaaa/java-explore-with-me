package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;
import ru.practicum.service.StatsService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@RequestBody EndpointHit hit) {
        statsService.saveHit(hit);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {

        try {
            LocalDateTime startDate = start != null ? parseDateTime(start) : LocalDateTime.now().minusYears(1);
            LocalDateTime endDate = end != null ? parseDateTime(end) : LocalDateTime.now();

            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Start date must be before end date"));
            }

            return ResponseEntity.ok(statsService.getStats(startDate, endDate, uris, unique));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid date format. Use 'yyyy-MM-dd HH:mm:ss'"));
        }
    }
    private LocalDateTime parseDateTime(String dateStr) throws DateTimeParseException {
        return LocalDateTime.parse(dateStr,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}