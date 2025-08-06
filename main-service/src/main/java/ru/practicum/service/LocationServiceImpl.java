package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.LocationMapper;
import ru.practicum.dto.LocationDto;
import ru.practicum.model.Location;
import ru.practicum.repository.LocationRepository;

import java.util.Objects;

@Transactional
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {
    private final LocationMapper mapper;
    private final LocationRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Location getOrSave(LocationDto dto) {
        Location location = repository.findByLatAndLon(dto.getLat(), dto.getLon());
        return Objects.requireNonNullElseGet(location, () -> repository.save(mapper.locationDtoToModel(dto)));
    }
}
