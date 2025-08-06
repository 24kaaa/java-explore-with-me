package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.CompilationMapper;
import ru.practicum.exception.NotFoundException;
import ru.practicum.dto.CompilationDto;
import ru.practicum.dto.NewCompilationDto;
import ru.practicum.dto.UpdateCompilationRequest;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.EventRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        Compilation compilation = new Compilation();
        compilation.setTitle(newCompilationDto.getTitle());
        compilation.setPinned(newCompilationDto.getPinned() != null ? newCompilationDto.getPinned() : false);

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(newCompilationDto.getEvents()));
            compilation.setEvents(events);
        }

        return compilationMapper.toCompilationDto(compilationRepository.save(compilation));
    }

    public void deleteCompilation(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation not found");
        }
        compilationRepository.deleteById(compId);
    }

    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateCompilationRequest) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation not found"));

        if (updateCompilationRequest.getTitle() != null) {
            compilation.setTitle(updateCompilationRequest.getTitle());
        }

        if (updateCompilationRequest.getPinned() != null) {
            compilation.setPinned(updateCompilationRequest.getPinned());
        }

        if (updateCompilationRequest.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(updateCompilationRequest.getEvents()));
            compilation.setEvents(events);
        }

        return compilationMapper.toCompilationDto(compilationRepository.save(compilation));
    }

    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);

        if (pinned != null) {
            return compilationRepository.findAllByPinned(pinned, pageable).stream()
                    .map(compilationMapper::toCompilationDto)
                    .collect(Collectors.toList());
        } else {
            return compilationRepository.findAll(pageable).stream()
                    .map(compilationMapper::toCompilationDto)
                    .collect(Collectors.toList());
        }
    }

    public CompilationDto getCompilation(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation not found"));
        return compilationMapper.toCompilationDto(compilation);
    }
}
