package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.dto.LocationDto;
import ru.practicum.model.Location;
import ru.practicum.repository.LocationRepository;


@Transactional
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {
    private final LocationMapper mapper;
    private final LocationRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Location getOrSave(LocationDto dto) {
        if (dto == null) {
            return null;
        }

        Location location = repository.findByLatAndLon(dto.getLat(), dto.getLon());

        if (location == null) {
            location = mapper.locationDtoToModel(dto);
            repository.save(location);
        }

        return location;
    }
}
