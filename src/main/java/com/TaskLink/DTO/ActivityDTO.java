package com.TaskLink.DTO;

import java.time.LocalDateTime;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityDTO {
	private String userName;
	private String action;
	private String taskTitle;
	private String taskStatus;
	private LocalDateTime timestamp;
}