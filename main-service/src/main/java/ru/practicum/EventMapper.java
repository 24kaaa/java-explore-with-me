package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.*;
import ru.practicum.model.Event;
import ru.practicum.model.RequestStatus;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.statsclient.StatsClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventMapper {
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;

    public EventShortDto toEventShortDto(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(calculateConfirmedRequests(event))
                .eventDate(event.getEventDate())  // Jackson сам сериализует в строку
                .initiator(userMapper.toUserShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(calculateViews(event))
                .build();
    }

    public EventFullDto toEventFullDto(Event event) {
        Objects.requireNonNull(event, "Event cannot be null");
        LocationDto locationDto = LocationDto.builder()
                .lat(event.getLocation().getLat())
                .lon(event.getLocation().getLon())
                .build();


        EventFullDto.EventFullDtoBuilder builder = EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(calculateConfirmedRequests(event))
                .description(event.getDescription())
                .eventDate(event.getEventDate())  // Jackson сам сериализует
                .initiator(userMapper.toUserShortDto(event.getInitiator()))
                .location(locationDto)
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .state(event.getState().name())
                .title(event.getTitle())
                .views(calculateViews(event));

        if (event.getCreatedOn() != null) {
            builder.createdOn(event.getCreatedOn());  // Jackson сам сериализует
        }

        if (event.getPublishedOn() != null) {
            builder.publishedOn(event.getPublishedOn());  // Jackson сам сериализует
        }

        return builder.build();
    }

    private Long calculateConfirmedRequests(Event event) {
        try {
            return requestRepository.countByEventIdAndStatus(
                    event.getId(),
                    RequestStatus.CONFIRMED);
        } catch (Exception e) {
            log.error("Error counting confirmed requests for event {}", event.getId(), e);
            return 0L;
        }
    }

    private Long calculateViews(Event event) {
        try {
            LocalDateTime start = LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now();
            String uri = "/events/" + event.getId();

            List<ViewStats> stats = statsClient.getStats(start, end, List.of(uri), true);
            return stats.isEmpty() ? 0L : stats.get(0).getHits();
        } catch (Exception e) {
            log.error("Failed to get views for event {}", event.getId(), e);
            return 0L;
        }
    }

    public Event newEventDtoToModel(NewEventDto dto) {
        if (dto == null) {
            return null;
        }

        return Event.builder()
                .annotation(dto.getAnnotation())
                .description(dto.getDescription())
                .eventDate(dto.getEventDate())  // Предполагается, что dto.getEventDate() возвращает LocalDateTime
                .paid(dto.getPaid() != null ? dto.getPaid() : false)
                .participantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0)
                .requestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)
                .title(dto.getTitle())
                .build();
    }
}