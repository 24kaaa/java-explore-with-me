package ru.practicum.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflictException(ConflictException e) {
        return new ErrorResponse(
                "Integrity constraint violation",
                e.getMessage(),
                "For the requested operation the conditions are not met.",
                "CONFLICT",
                LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        );
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundException(NotFoundException e) {
        return new ErrorResponse(
                "Object not found",
                e.getMessage(),
                "The required object was not found.",
                "NOT_FOUND",
                LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        );
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequestException(BadRequestException e) {
        return new ErrorResponse(
                "Validation error",
                e.getMessage(),
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        );
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return ErrorResponse.builder()
                .error("Validation error")
                .message(errorMessage)
                .reason("Incorrectly made request")
                .status("BAD_REQUEST")
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .build();
    }
}