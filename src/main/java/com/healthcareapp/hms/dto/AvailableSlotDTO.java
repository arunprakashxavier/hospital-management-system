package com.healthcareapp.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSlotDTO {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}