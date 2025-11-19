package com.TaskLink.DTO;



import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskDTO {
	private Long id; 
  private String title;
  private String description;
  private String status; // OPEN, IN_PROGRESS, DONE
  private LocalDateTime dueDate;

  private Long assigneeId;
  private Long reporterId;
  private Long projectId;
  private Long sprintId;

  private Integer storyPoints;
  private Integer order; // board order

  private List<Long> labelIds;
}
