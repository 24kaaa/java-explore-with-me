package ru.practicum;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.dto.CompilationDto;
import ru.practicum.model.Compilation;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CompilationMapper {
    private final EventMapper eventMapper;

    public CompilationDto toCompilationDto(Compilation compilation) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .events(compilation.getEvents() != null ?
                        compilation.getEvents().stream()
                                .map(eventMapper::toEventShortDto)
                                .collect(Collectors.toSet()) : Collections.emptySet())
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }
}
