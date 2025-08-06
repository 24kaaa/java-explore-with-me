package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.dto.NewUserRequest;
import ru.practicum.dto.UserDto;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;

import java.util.List;


@Service
@RequiredArgsConstructor

public class UserService {
    private static final Sort DEFAULT_SORT = Sort.by("id").ascending();

    private final UserRepository userRepository;
    private final UserMapper userMapper;


    public UserDto createUser(NewUserRequest newUserRequest) {
        if (userRepository.existsByEmail(newUserRequest.getEmail())) {
            throw new ConflictException("Email уже используется");
        }
        validateEmail(newUserRequest.getEmail());

        User user = userMapper.toUser(newUserRequest);

        return userMapper.toUserDto(userRepository.save(user));
    }

    public List<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
        if (from == null) from = 0;
        if (size == null) size = 10;

        validatePaginationParams(from, size);

        int page = from / size;  // номер страницы

        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);

        if (ids == null || ids.isEmpty()) {
            return userRepository.findAll(pageable)
                    .map(userMapper::toUserDto)
                    .getContent();
        }

        return userRepository.findAllByIdIn(ids, pageable)
                .map(userMapper::toUserDto)
                .getContent();
    }


    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " not found");
        }
        userRepository.deleteById(userId);
    }

    private void validateEmail(String email) {
        if (email.length() < 6) {
            throw new ConflictException("Email must be exactly 6 characters long");
        }

    }

    private void validatePaginationParams(Integer from, Integer size) {
        if (from < 0) {
            throw new IllegalArgumentException("Parameter 'from' must not be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Parameter 'size' must be positive");
        }
    }
}