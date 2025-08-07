package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.ConfirmedRequestsDto;
import ru.practicum.dto.ViewStats;
import ru.practicum.model.RequestStatus;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.statsclient.StatsClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventStatisticsService {
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;

    @Transactional(readOnly = true)
    public Long getConfirmedRequestsCount(Long eventId) {
        try {
            return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        } catch (Exception e) {
            log.error("Error counting confirmed requests for event {}", eventId, e);
            return 0L;
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getConfirmedRequestsCounts(List<Long> eventIds) {
        List<ConfirmedRequestsDto> counts = requestRepository.findConfirmedRequestsCount(
                eventIds,
                RequestStatus.CONFIRMED
        );

        return counts.stream()
                .collect(Collectors.toMap(
                        ConfirmedRequestsDto::getEvent,
                        ConfirmedRequestsDto::getCount
                ));
    }

    @Transactional(readOnly = true)
    public Long getViewsCount(Long eventId) {
        try {
            LocalDateTime start = LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now();
            String uri = "/events/" + eventId;

            List<ViewStats> stats = statsClient.getStats(start, end, List.of(uri), true);
            return stats.isEmpty() ? 0L : stats.get(0).getHits();
        } catch (Exception e) {
            log.error("Failed to get views for event {}", eventId, e);
            return 0L;
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getViewsCounts(List<Long> eventIds) {
        LocalDateTime start = LocalDateTime.now().minusYears(1);
        LocalDateTime end = LocalDateTime.now();
        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .collect(Collectors.toList());

        return statsClient.getStats(start, end, uris, true)
                .stream()
                .collect(Collectors.toMap(
                        stat -> Long.parseLong(stat.getUri().substring("/events/".length())),
                        ViewStats::getHits
                ));
    }
}
