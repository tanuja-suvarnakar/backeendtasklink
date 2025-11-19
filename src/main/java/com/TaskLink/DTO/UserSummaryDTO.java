package com.TaskLink.DTO;



import java.util.List;
import java.util.Map;

import lombok.*;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryDTO {
    private long totalTasks;
    private long completedTasks;
    private long createdLast7;
    private long updatedLast7;
    private long dueSoon;
    private Map<String, Long> statusCount; 
    private List<ActivityDTO> recent;
}