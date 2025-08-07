package ru.practicum.service;

import ru.practicum.dto.LocationDto;
import ru.practicum.model.Location;

public interface LocationService {

    Location getOrSave(LocationDto dto);
}
