package com.TaskLink.DTO;



import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class TaskDetailDTO {
	private Long id;
	private String title;
	private String description;
	private String status;
	private LocalDateTime dueDate;

	private Long projectId;
	private Long sprintId;
	private Integer storyPoints;
	private Integer order;

	// Reporter info
	private Long reporterId;
	private String reporterName;
	private String reporterEmail;

	// Assignee info
	private Long assigneeId;
	private String assigneeName;
	private String assigneeEmail;

	private List<Long> labelIds;
}
