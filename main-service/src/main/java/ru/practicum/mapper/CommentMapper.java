package ru.practicum.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.dto.*;
import ru.practicum.model.Comment;
import ru.practicum.model.Event;
import ru.practicum.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CommentMapper {
    private final UserMapper userMapper;
    private final EventMapper eventMapper;

    public Comment toModel(CommentRequestDto dto, User author, Event event) {
        LocalDateTime now = LocalDateTime.now();

        return Comment.builder()
                .text(dto.getText())
                .author(author)
                .event(event)
                .created(dto.getCreated() != null ? dto.getCreated() : now)
                .edited(null)
                .confirmedRequests(dto.getConfirmedRequests() != null ?
                        dto.getConfirmedRequests() :
                        eventMapper.toEventShortDto(event).getConfirmedRequests())
                .build();
    }

    public CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(userMapper.toUserShortDto(comment.getAuthor()))
                .event(eventMapper.toEventShortDto(comment.getEvent()))
                .created(comment.getCreated())
                .edited(comment.getEdited())
                .build();
    }

    public List<CommentDto> toDtoList(List<Comment> comments) {
        return comments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void updateModel(CommentUpdateRequestDto dto, Comment comment) {
        if (dto.getText() != null && !dto.getText().isBlank()) {
            comment.setText(dto.getText());
            comment.setEdited(LocalDateTime.now());
        }
    }
}