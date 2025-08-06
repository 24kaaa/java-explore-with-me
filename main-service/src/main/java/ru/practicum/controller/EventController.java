package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.UpdateEventAdminRequest;
import ru.practicum.exception.BadRequestException;
import ru.practicum.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventFullDto>> getEvents(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false, name = "rangeStart") String start,
            @RequestParam(required = false, name = "rangeEnd") String end,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        return ResponseEntity.ok(eventService.searchEvents(users, states, categories, start, end, from, size));
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> updateEvent(
            @PathVariable Long eventId,
            @RequestBody @Valid UpdateEventAdminRequest updateEventAdminRequest,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            throw new BadRequestException("Validation failed: " + bindingResult.getAllErrors());
        }

        return ResponseEntity.ok(eventService.updateEventByAdmin(eventId, updateEventAdminRequest));
    }
}