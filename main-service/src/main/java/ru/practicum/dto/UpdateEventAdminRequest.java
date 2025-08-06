package ru.practicum.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.model.Location;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEventAdminRequest {
    @Size(min = 20, max = 2000)
    private String annotation;

    private Long category;

    @Size(min = 20, max = 7000)
    private String description;

    private String eventDate;

    private Location location;

    private Boolean paid;

    @Min(0)
    private Integer participantLimit;

    private Boolean requestModeration;

    private String stateAction; // PUBLISH_EVENT, REJECT_EVENT

    @Size(min = 3, max = 120)
    private String title;
}
