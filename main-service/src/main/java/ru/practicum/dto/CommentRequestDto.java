package ru.practicum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentRequestDto {
    @NotBlank
    @Size(min = 10, max = 1000, message = "Comment text must be between 10 and 2000 characters")
    private String text;
    private LocalDateTime created;
    private LocalDateTime edited;
    private Long confirmedRequests;
}
