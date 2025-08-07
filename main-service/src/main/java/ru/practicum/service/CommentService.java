package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.CommentDto;
import ru.practicum.dto.CommentRequestDto;
import ru.practicum.dto.CommentUpdateRequestDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.model.Event;
import ru.practicum.model.User;
import ru.practicum.repository.CommentRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    @Transactional
    public CommentDto createComment(Long userId, Long eventId, CommentRequestDto requestDto) {
        User author = getUserById(userId);
        Event event = getEventById(eventId);

        Comment comment = commentMapper.toModel(requestDto, author, event);
        comment = commentRepository.save(comment);

        return commentMapper.toDto(comment);
    }

    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, CommentUpdateRequestDto requestDto) {
        checkUserExists(userId);
        Comment comment = getCommentByIdAndAuthorId(commentId, userId);

        commentMapper.updateModel(requestDto, comment);
        comment = commentRepository.save(comment);

        return commentMapper.toDto(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByEvent(Long eventId, int from, int size) {
        getEventById(eventId);
        PageRequest page = PageRequest.of(from / size, size);
        return commentMapper.toDtoList(
                commentRepository.findAllByEventId(eventId, page)
        );
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getUserComments(Long userId, int from, int size) {
        checkUserExists(userId);
        PageRequest page = PageRequest.of(from / size, size);
        return commentMapper.toDtoList(
                commentRepository.findAllByAuthor(getUserById(userId), page)
        );
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        if (!commentRepository.existsByIdAndAuthorId(commentId, userId)) {
            throw new NotFoundException("Comment not found or you are not the author");
        }
        commentRepository.deleteById(commentId);
    }

    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Comment not found");
        }
        commentRepository.deleteById(commentId);
    }

    @Transactional(readOnly = true)
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
    }

    @Transactional(readOnly = true)
    private Comment getCommentByIdAndAuthorId(Long commentId, Long authorId) {
        return commentRepository.findByIdAndAuthorId(commentId, authorId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
    }

    private void checkUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found");
        }
    }
}
