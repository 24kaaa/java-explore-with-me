package ru.practicum.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.*;
import ru.practicum.model.Event;
import ru.practicum.model.RequestStatus;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.service.EventService;
import ru.practicum.service.EventStatisticsService;
import ru.practicum.statsclient.StatsClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventMapper {
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final EventStatisticsService eventStatsService;

    public EventShortDto toEventShortDto(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(eventStatsService.getConfirmedRequestsCount(event.getId()))
                .eventDate(event.getEventDate())
                .initiator(userMapper.toUserShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(eventStatsService.getViewsCount(event.getId()))
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
                .confirmedRequests(eventStatsService.getConfirmedRequestsCount(event.getId()))
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(userMapper.toUserShortDto(event.getInitiator()))
                .location(locationDto)
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .state(event.getState().name())
                .title(event.getTitle())
                .views(eventStatsService.getViewsCount(event.getId()));

        if (event.getCreatedOn() != null) {
            builder.createdOn(event.getCreatedOn());
        }

        if (event.getPublishedOn() != null) {
            builder.publishedOn(event.getPublishedOn());
        }

        return builder.build();
    }

    public Event newEventDtoToModel(NewEventDto dto) {
        if (dto == null) {
            return null;
        }

        return Event.builder()
                .annotation(dto.getAnnotation())
                .description(dto.getDescription())
                .eventDate(dto.getEventDate())
                .paid(dto.getPaid() != null ? dto.getPaid() : false)
                .participantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0)
                .requestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)
                .title(dto.getTitle())
                .build();
    }
}