package com.devmetrics.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 2, max = 80) String displayName,
        @Size(max = 64) String timezone,
        @Min(10) @Max(5000) Integer weeklyGoalPoints,
        @Pattern(regexp = "^[a-zA-Z0-9-]{3,40}$",
                message = "use de 3 a 40 letras, numeros ou hifen") String username,
        Boolean publicProfile
) {
}
