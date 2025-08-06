package ru.practicum.service;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.*;
import ru.practicum.dto.*;
import ru.practicum.exception.*;
import ru.practicum.EventMapper;
import ru.practicum.RequestMapper;
import ru.practicum.model.*;
import ru.practicum.repository.*;
import ru.practicum.statsclient.StatsClient;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import static ru.practicum.model.EventState.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EventService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final RequestMapper requestMapper;
    private final StatsClient statsClient;
    private final LocationService locationService;
    private final LocationMapper locationMapper;

    public List<EventFullDto> searchEvents(List<Long> users, List<String> states, List<Long> categories,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size) {
        validatePaginationParams(from, size);
        Pageable pageable = PageRequest.of(from / size, size);

        Specification<Event> spec = Specification.where(null);

        if (users != null && !users.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("initiator").get("id").in(users));
        }

        if (states != null && !states.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                List<EventState> stateEnums = states.stream()
                        .map(EventState::valueOf)
                        .collect(Collectors.toList());
                return root.get("state").in(stateEnums);
            });
        }

        if (categories != null && !categories.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("category").get("id").in(categories));
        }

        if (rangeStart != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
        }

        if (rangeEnd != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
        }

        return eventRepository.findAll(spec, pageable).stream()
                .map(eventMapper::toEventFullDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (updateEventAdminRequest.getEventDate() != null) {
            if (updateEventAdminRequest.getEventDate().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Event date must be in the future");
            }
            event.setEventDate(updateEventAdminRequest.getEventDate());
        }

        if (updateEventAdminRequest.getLocation() != null) {
            try {
                LocationDto locationDto = locationMapper.modelToDto(updateEventAdminRequest.getLocation());
                Location location = locationService.getOrSave(locationDto);
                event.setLocation(location);
            } catch (DataIntegrityViolationException e) {
                throw new ConflictException("Location conflict: " + e.getMessage());
            }
        }

        if (updateEventAdminRequest.getStateAction() != null) {
            processStateAction(event, updateEventAdminRequest.getStateAction());
        }

        try {
            updateEventFields(event, updateEventAdminRequest);
            Event updatedEvent = eventRepository.save(event);
            return eventMapper.toEventFullDto(updatedEvent);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Data integrity violation: " + e.getMostSpecificCause().getMessage());
        }
    }

    private void updateEventFields(Event event, UpdateEventAdminRequest request) {
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getCategory() != null) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
    }

    private void processStateAction(Event event, String stateAction) {
        switch (stateAction) {
            case "PUBLISH_EVENT":
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                break;
            case "REJECT_EVENT":
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Cannot reject the event because it's already published");
                }
                event.setState(EventState.CANCELED);
                break;
            default:
                throw new BadRequestException("Invalid state action: " + stateAction);
        }
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        validatePaginationParams(from, size);
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        Pageable pageable = PageRequest.of(from / size, size);
        return eventRepository.findAllByInitiatorId(userId, pageable).stream()
                .map(eventMapper::toEventShortDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id=" + newEventDto.getCategory() + " was not found"));

        Location location = locationService.getOrSave(newEventDto.getLocation());

        validateEventDateForUser(newEventDto.getEventDate());

        Event event = Event.builder()
                .annotation(newEventDto.getAnnotation())
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())  // Используем LocalDateTime напрямую
                .paid(newEventDto.getPaid() != null ? newEventDto.getPaid() : false)
                .participantLimit(newEventDto.getParticipantLimit() != null ? newEventDto.getParticipantLimit() : 0)
                .requestModeration(newEventDto.getRequestModeration() != null ? newEventDto.getRequestModeration() : true)
                .title(newEventDto.getTitle())
                .initiator(user)
                .category(category)
                .location(location)
                .createdOn(LocalDateTime.now())
                .state(EventState.PENDING)
                .build();

        Event savedEvent = eventRepository.save(event);
        return eventMapper.toEventFullDto(savedEvent);
    }

    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        return eventMapper.toEventFullDto(event);
    }

    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (updateEventUserRequest.getEventDate() != null) {
            LocalDateTime newEventDate = parseDateTime(updateEventUserRequest.getEventDate());
            validateEventDateForUser(newEventDate);
            event.setEventDate(newEventDate);
        }

        if (updateEventUserRequest.getStateAction() != null) {
            processUserStateAction(event, updateEventUserRequest.getStateAction());
        }

        updateEventFields(event, updateEventUserRequest);
        Event updatedEvent = eventRepository.save(event);
        return eventMapper.toEventFullDto(updatedEvent);
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                               String sort, Integer from, Integer size,
                                               HttpServletRequest request) {

        sendHitToStatsService(request.getRequestURI(), request.getRemoteAddr());

        validatePaginationParams(from, size);

        LocalDateTime start = rangeStart != null ? parseDateTime(rangeStart) : LocalDateTime.now();
        LocalDateTime end = rangeEnd != null ? parseDateTime(rangeEnd) : null;

        if (end != null && start.isAfter(end)) {
            throw new BadRequestException("Start date must be before end date");
        }

        Specification<Event> spec = buildPublicEventsSpecification(text, categories, paid, start, end, onlyAvailable);
        Pageable pageable = buildPageable(sort, from, size);

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        return enrichEventsWithStats(events);
    }

    @Transactional(readOnly = true)
    public EventFullDto getPublicEventById(Long id, HttpServletRequest request) {
        Event event = eventRepository.findByIdAndState(id, PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        sendHitToStatsService("/events/" + id, request.getRemoteAddr());

        Long views = getEventViews(id);
        EventFullDto dto = eventMapper.toEventFullDto(event);
        dto.setViews(views);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        if (!eventRepository.existsByIdAndInitiatorId(eventId, userId)) {
            throw new NotFoundException("Event not found for this user");
        }

        return requestRepository.findAllByEventId(eventId).stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest requestStatusUpdateRequest) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            throw new ConflictException("Event does not require request moderation");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByIdInAndEventId(
                requestStatusUpdateRequest.getRequestIds(), eventId);

        if (requests.isEmpty()) {
            throw new NotFoundException("No requests found with provided ids");
        }

        return processRequestStatusUpdate(event, requests, requestStatusUpdateRequest.getStatus());
    }

    private Specification<Event> buildPublicEventsSpecification(String text, List<Long> categories, Boolean paid,
                                                                LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                                Boolean onlyAvailable) {
        Specification<Event> spec = Specification.where((root, query, cb) -> cb.equal(root.get("state"), PUBLISHED));

        if (text != null) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("annotation")), "%" + text.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + text.toLowerCase() + "%")
            ));
        }

        if (categories != null && !categories.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("category").get("id").in(categories));
        }

        if (paid != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("paid"), paid));
        }

        spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("eventDate"), rangeStart));

        if (rangeEnd != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("eventDate"), rangeEnd));
        }

        if (Boolean.TRUE.equals(onlyAvailable)) {
            spec = spec.and((root, query, cb) -> {
                // Создаем подзапрос для подсчета подтвержденных запросов
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<ParticipationRequest> requestRoot = subquery.from(ParticipationRequest.class);
                subquery.select(cb.count(requestRoot))
                        .where(cb.and(
                                cb.equal(requestRoot.get("event"), root),
                                cb.equal(requestRoot.get("status"), RequestStatus.CONFIRMED)
                        ));

                return cb.or(
                        cb.equal(root.get("participantLimit"), 0),
                        cb.greaterThan(root.get("participantLimit"), subquery)
                );
            });
        }

        return spec;
    }

    private Pageable buildPageable(String sort, Integer from, Integer size) {
        if ("EVENT_DATE".equals(sort)) {
            return PageRequest.of(from / size, size, Sort.by("eventDate"));
        } else if ("VIEWS".equals(sort)) {
            return PageRequest.of(from / size, size, Sort.by("views").descending());
        }
        return PageRequest.of(from / size, size);
    }

    private List<EventShortDto> enrichEventsWithStats(List<Event> events) {
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());

        Map<Long, Long> confirmedRequests = requestRepository.findConfirmedRequestsCount(eventIds, RequestStatus.CONFIRMED)
                .stream()
                .collect(Collectors.toMap(
                        ConfirmedRequestsDto::getEvent,
                        ConfirmedRequestsDto::getCount
                ));

        Map<Long, Long> views = getEventsViews(eventIds);

        return events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toEventShortDto(event);
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L));
                    dto.setViews(views.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    private Map<Long, Long> getEventsViews(List<Long> eventIds) {
        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .collect(Collectors.toList());

        List<ViewStats> stats = statsClient.getStats(
                LocalDateTime.now().minusYears(1),
                LocalDateTime.now(),
                uris,
                true
        );

        return stats.stream()
                .collect(Collectors.toMap(
                        stat -> Long.parseLong(stat.getUri().substring("/events/".length())),
                        ViewStats::getHits
                ));
    }

    @Transactional(readOnly = true)
    private long getEventViews(Long eventId) {
        List<ViewStats> stats = statsClient.getStats(
                LocalDateTime.now().minusYears(1),
                LocalDateTime.now(),
                List.of("/events/" + eventId),
                true
        );
        return stats.isEmpty() ? 0 : stats.get(0).getHits();
    }

    private void sendHitToStatsService(String uri, String ip) {
        EndpointHit hit = EndpointHit.builder()
                .app("ewm-main-service")
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();

        try {
            statsClient.hit(hit);
            log.debug("Hit successfully sent for URI: {}", uri);

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            log.error("Failed to send hit for URI: {}. Error: {}", uri, e.getMessage());
        }
    }

    private EventRequestStatusUpdateResult processRequestStatusUpdate(Event event,
                                                                      List<ParticipationRequest> requests,
                                                                      String status) {
        long confirmedCount = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        if (confirmedCount > Integer.MAX_VALUE) {
            throw new IllegalStateException("Confirmed requests count exceeds integer limit");
        }
        int availableSlots = event.getParticipantLimit() - (int) confirmedCount;

        if (status.equals("CONFIRMED") && availableSlots <= 0) {
            throw new ConflictException("The participant limit has been reached");
        }

        List<ParticipationRequest> confirmedRequests = new ArrayList<>();
        List<ParticipationRequest> rejectedRequests = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            if (!request.getStatus().equals(RequestStatus.PENDING)) {
                throw new ConflictException("Request must have status PENDING");
            }

            if (status.equals("CONFIRMED") && availableSlots > 0) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmedRequests.add(request);
                availableSlots--;
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(request);
            }
        }

        requestRepository.saveAll(requests);

        if (availableSlots == 0 && status.equals("CONFIRMED")) {
            rejectPendingRequests(event.getId(), rejectedRequests);
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests.stream()
                        .map(requestMapper::toParticipationRequestDto)
                        .collect(Collectors.toList()))
                .rejectedRequests(rejectedRequests.stream()
                        .map(requestMapper::toParticipationRequestDto)
                        .collect(Collectors.toList()))
                .build();
    }

    private void rejectPendingRequests(Long eventId, List<ParticipationRequest> rejectedRequests) {
        List<ParticipationRequest> pendingRequests = requestRepository
                .findAllByEventIdAndStatus(eventId, RequestStatus.PENDING);

        pendingRequests.forEach(r -> r.setStatus(RequestStatus.REJECTED));
        requestRepository.saveAll(pendingRequests);
        rejectedRequests.addAll(pendingRequests);
    }

    private void validateEventDateForUser(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours from now");
        }
    }

    private void validateEventDateForAdmin(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException("Event date must be at least 1 hour from now");
        }
    }

    private void processAdminStateAction(Event event, String stateAction) {
        if ("PUBLISH_EVENT".equals(stateAction)) {
            if (event.getState() == PUBLISHED) {
                throw new ConflictException("Cannot publish already published event");
            }
            if (event.getState() == CANCELED) {
                throw new ConflictException("Cannot publish canceled event");
            }
            event.setState(PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if ("REJECT_EVENT".equals(stateAction)) {
            if (event.getState() == PUBLISHED) {
                throw new ConflictException("Cannot reject published event");
            }
            event.setState(CANCELED);
        }
    }

    private void processUserStateAction(Event event, String stateAction) {
        if (stateAction.equals("SEND_TO_REVIEW")) {
            event.setState(PENDING);
        } else if (stateAction.equals("CANCEL_REVIEW")) {
            event.setState(CANCELED);
        }
    }

    private void setDefaultValuesIfNull(Event event) {
        if (event.getPaid() == null) {
            event.setPaid(false);
        }
        if (event.getParticipantLimit() == null) {
            event.setParticipantLimit(0);
        }
        if (event.getRequestModeration() == null) {
            event.setRequestModeration(true);
        }
    }

    private void validatePaginationParams(Integer from, Integer size) {
        if (from == null || from < 0) {
            throw new BadRequestException("Parameter 'from' must be positive");
        }
        if (size == null || size <= 0) {
            throw new BadRequestException("Parameter 'size' must be positive");
        }
    }

    private LocalDateTime parseDateTime(String dateTime) {
        if (dateTime == null) {
            throw new BadRequestException("Date/time cannot be null");
        }
        try {
            return LocalDateTime.parse(dateTime, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid date format. Expected format: yyyy-MM-dd HH:mm:ss");
        }
    }

    private void updateEventFields(Event event, UpdateEventUserRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getLocation() != null) {
            event.setLocation(updateRequest.getLocation());
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
    }
}