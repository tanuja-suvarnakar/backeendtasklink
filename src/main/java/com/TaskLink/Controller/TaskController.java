package com.TaskLink.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.TaskLink.DTO.TaskDTO;
import com.TaskLink.Entity.*;
import com.TaskLink.Repository.*;
import com.TaskLink.Service.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TaskController {
	@Autowired
	private TaskService taskService;
	@Autowired
	private TaskRepository taskRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private CurrentUserService currentUser;
	@Autowired
	private ProjectMembershipService membership;
	@Value("${app.frontend.base-url}")
	private String frontendBaseUrl;

	@Autowired
	private SmtpEmailService emailService;

	@GetMapping
	public ResponseEntity<?> all() {
		User me = currentUser.requireCurrentUser();
		return ResponseEntity.ok(taskRepository.findAllVisibleToUser(me.getId()));
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> get(@PathVariable Long id) {
		User me = currentUser.requireCurrentUser();
		Optional<Task> taskOpt = taskRepository.findByIdVisibleToUser(id, me.getId());

		if (taskOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("status", "error", "message", "Task not found or access denied"));
		}

		Task task = taskOpt.get();

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("id", task.getId());
		response.put("title", task.getTitle());
		response.put("description", task.getDescription());
		response.put("status", task.getStatus());
		response.put("dueDate", task.getDueDate());

		if (task.getReporter() != null) {
			response.put("reporter", Map.of("id", task.getReporter().getId(), "email", task.getReporter().getEmail(),
					"name", task.getReporter().getFirstname() + " " + task.getReporter().getLastname()));
		}

		if (task.getAssignee() != null) {
			response.put("assignee", Map.of("id", task.getAssignee().getId(), "email", task.getAssignee().getEmail(),
					"name", task.getAssignee().getFirstname() + " " + task.getAssignee().getLastname()));
		}

		if (task.getProject() != null) {
			response.put("project", Map.of("id", task.getProject().getId(), "name", task.getProject().getName(),
					"description", task.getProject().getDescription()));
		}

		return ResponseEntity.ok(response);
	}

	@PostMapping
	public ResponseEntity<?> create(@RequestBody TaskDTO dto) {
		User me = currentUser.requireCurrentUser();
		if (dto.getProjectId() == null)
			return ResponseEntity.badRequest().body("projectId is required");
		if (!membership.isMember(dto.getProjectId(), me.getId()))
			return ResponseEntity.status(403).body("Forbidden");

		if (dto.getAssigneeId() != null && !membership.isAdmin(dto.getProjectId(), me.getId()))
			return ResponseEntity.status(403).body("Only admin can assign");

		Task t = Task.builder().title(dto.getTitle()).description(dto.getDescription())
				.status(dto.getStatus() == null ? "OPEN" : dto.getStatus())
				.dueDate(dto.getDueDate() == null ? LocalDateTime.now().plusDays(7) : dto.getDueDate()).reporter(me)
				.build();

		Task created = taskService.create(t, dto.getAssigneeId(), dto.getProjectId());
		return ResponseEntity.ok(created);
	}

	@PutMapping("/{id}")
	public ResponseEntity<?> update(@PathVariable Long id, @RequestBody TaskDTO dto) {
		User me = currentUser.requireCurrentUser();

		return taskRepository.findById(id).map(existing -> {
			Long projectId = Optional.ofNullable(existing.getProject()).map(Project::getId).orElse(dto.getProjectId());
			if (projectId == null)
				return ResponseEntity.badRequest().body("Task has no project");
			if (!membership.isMember(projectId, me.getId()))
				return ResponseEntity.status(403).body("Forbidden");

			if (dto.getAssigneeId() != null && !membership.isAdmin(projectId, me.getId()))
				return ResponseEntity.status(403).body("Only admin can assign");

			existing.setTitle(dto.getTitle());
			existing.setDescription(dto.getDescription());
			existing.setStatus(dto.getStatus());
			existing.setDueDate(dto.getDueDate());

			if (dto.getAssigneeId() != null) {
				var assignee = userRepository.findById(dto.getAssigneeId());
				if (assignee.isEmpty())
					return ResponseEntity.badRequest().body("Assignee not found");
				if (!membership.isMember(projectId, assignee.get().getId()))
					return ResponseEntity.badRequest().body("Assignee must be project member");
				existing.setAssignee(assignee.get());
			}

			taskService.create(existing, null, projectId); // save
			return ResponseEntity.ok(existing);
		}).orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> delete(@PathVariable Long id) {
		User me = currentUser.requireCurrentUser();
		return taskRepository.findById(id).map(existing -> {
			Long projectId = Optional.ofNullable(existing.getProject()).map(Project::getId).orElse(null);
			if (projectId == null)
				return ResponseEntity.badRequest().body("Task has no project");
			if (!membership.isAdmin(projectId, me.getId()))
				return ResponseEntity.status(403).body("Only admin can delete");
			taskService.delete(id);
			return ResponseEntity.ok().build();
		}).orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/{id}/assign")
	public ResponseEntity<?> assign(@PathVariable Long id, @RequestParam Long userId) {
		User me = currentUser.requireCurrentUser();
		Task task = taskRepository.findById(id).orElse(null);
		if (task == null)
			return ResponseEntity.notFound().build();

		Long projectId = Optional.ofNullable(task.getProject()).map(Project::getId).orElse(null);
		if (projectId == null)
			return ResponseEntity.badRequest().body("Task has no project");

		if (!membership.isAdmin(projectId, me.getId()))
			return ResponseEntity.status(403).body("Only admin can assign");

		if (!membership.isMember(projectId, userId))
			return ResponseEntity.badRequest().body("Assignee must be project member");

		var assignee = userRepository.findById(userId).orElse(null);
		if (assignee == null)
			return ResponseEntity.badRequest().body("User not found");

		task.setAssignee(assignee);
		taskRepository.save(task);

		// ✅ Send email notification
		try {
			String subject = "New Task Assigned: " + task.getTitle();

			String projectName = task.getProject() != null ? task.getProject().getName() : "Project";
			String taskUrl = String.format("%s/projects/%d/tasks/%d", frontendBaseUrl, projectId, task.getId());

			String htmlBody = """
					<div style='font-family:Arial,sans-serif;padding:20px;background:#f9f9f9;'>
					  <h2 style='color:#333;'>New Task Assigned to You</h2>
					  <p>Hello <b>%s</b>,</p>
					  <p>You have been assigned a new task in project <b>%s</b>.</p>
					  <p><b>Task Title:</b> %s</p>
					  <p><b>Due Date:</b> %s</p>
					  <p>Click below to view the task:</p>
					  <a href='%s' style='display:inline-block;margin-top:10px;background:#007bff;color:#fff;
					      padding:10px 15px;border-radius:5px;text-decoration:none;'>View Task</a>
					  <p style='margin-top:20px;color:#777;'>- Datagateway CRM</p>
					</div>
					""".formatted(assignee.getFirstname() != null ? assignee.getFirstname() : assignee.getEmail(),
					projectName, task.getTitle(), task.getDueDate() != null ? task.getDueDate().toString() : "Not set",
					taskUrl);

			emailService.send(assignee.getEmail(), subject, htmlBody);
		} catch (Exception e) {
		
		}

		return ResponseEntity.ok(task);
	}

//	@PostMapping("/{id}/assign")
//	public ResponseEntity<?> assign(@PathVariable Long id, @RequestParam Long userId) {
//		User me = currentUser.requireCurrentUser();
//		Task task = taskRepository.findById(id).orElse(null);
//		if (task == null)
//			return ResponseEntity.notFound().build();
//
//		Long projectId = Optional.ofNullable(task.getProject()).map(Project::getId).orElse(null);
//		if (projectId == null)
//			return ResponseEntity.badRequest().body("Task has no project");
//
//		if (!membership.isAdmin(projectId, me.getId()))
//			return ResponseEntity.status(403).body("Only admin can assign");
//
//		if (!membership.isMember(projectId, userId))
//			return ResponseEntity.badRequest().body("Assignee must be project member");
//
//		var assignee = userRepository.findById(userId).orElse(null);
//		if (assignee == null)
//			return ResponseEntity.badRequest().body("User not found");
//
//		task.setAssignee(assignee);
//		taskRepository.save(task);
//
//		// ✅ Send email notification
//		try {
//			String subject = "New Task Assigned: " + task.getTitle();
//
//			String projectName = task.getProject() != null ? task.getProject().getName() : "a project";
//			String taskUrl = String.format("%s/projects/%d/tasks/%d", frontendBaseUrl, projectId, task.getId());
//
//			// --- Start of New Email Design ---
//			String assigneeName = assignee.getFirstname() != null ? assignee.getFirstname() : assignee.getEmail();
//			String assignerName = me.getFirstname(); // Assumes User object has a getName() method
//			String dueDateStr = task.getDueDate() != null
//					? task.getDueDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
//					: "Not set";
//
//			// You can pull these from your application properties
//			String companyName = "Datagateway CRM";
//			String logoUrl = "https://your-cdn.com/path/to/datagateway-logo.png"; // A publicly accessible URL to your
//																					// logo
//
//			String htmlBody = """
//					<!DOCTYPE html>
//					<html lang="en">
//					<head>
//					    <meta charset="UTF-8">
//					    <meta name="viewport" content="width=device-width, initial-scale=1.0">
//					    <title>New Task Assigned</title>
//					</head>
//					<body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f4f4f7;">
//					    <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #f4f4f7;">
//					        <tr>
//					            <td align="center">
//					                <table width="600" border="0" cellspacing="0" cellpadding="0" style="background-color: #ffffff; margin-top: 20px; margin-bottom: 20px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
//					                    <!-- Header with Logo -->
//					                    <tr>
//					                        <td align="center" style="padding: 40px 0 30px 0; border-bottom: 1px solid #eeeeee;">
//					                            <img src="%s" alt="%s Logo" width="150" style="display: block;">
//					                        </td>
//					                    </tr>
//					                    <!-- Content Body -->
//					                    <tr>
//					                        <td style="padding: 40px 30px 40px 30px; color: #333333;">
//					                            <h1 style="font-size: 24px; margin: 0 0 20px 0; font-weight: 600;">A New Task Was Assigned to You</h1>
//					                            <p style="margin: 0 0 24px 0; font-size: 16px; line-height: 1.5;">
//					                                Hello <b>%s</b>,
//					                            </p>
//					                            <p style="margin: 0 0 24px 0; font-size: 16px; line-height: 1.5;">
//					                                <b>%s</b> has assigned you a new task in the project "<b>%s</b>".
//					                            </p>
//
//					                            <!-- Task Details Box -->
//					                            <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #f9f9f9; border-left: 4px solid #007bff; padding: 15px 20px; margin-bottom: 30px;">
//					                                <tr>
//					                                    <td>
//					                                        <p style="margin: 0 0 10px 0; font-size: 16px; color: #555;"><b>Task:</b> %s</p>
//					                                        <p style="margin: 0; font-size: 16px; color: #555;"><b>Due Date:</b> %s</p>
//					                                    </td>
//					                                </tr>
//					                            </table>
//
//					                            <!-- CTA Button -->
//					                            <table border="0" cellspacing="0" cellpadding="0" width="100%%">
//					                                <tr>
//					                                    <td align="center">
//					                                        <a href="%s" target="_blank" style="background-color: #007bff; color: #ffffff; text-decoration: none; padding: 15px 25px; border-radius: 5px; display: inline-block; font-size: 16px; font-weight: bold;">
//					                                            View Task Details
//					                                        </a>
//					                                    </td>
//					                                </tr>
//					                            </table>
//					                        </td>
//					                    </tr>
//					                    <!-- Footer -->
//					                    <tr>
//					                        <td align="center" style="padding: 20px 30px; background-color: #f9f9f9; border-top: 1px solid #eeeeee;">
//					                            <p style="margin: 0; color: #999999; font-size: 12px;">
//					                                © %d %s. All rights reserved.
//					                            </p>
//					                        </td>
//					                    </tr>
//					                </table>
//					            </td>
//					        </tr>
//					    </table>
//					</body>
//					</html>
//					"""
//					.formatted(logoUrl, // Logo image URL
//							companyName, // Logo alt text
//							assigneeName, // "Hello [Name]"
//							assignerName, // "[Assigner] has assigned..."
//							projectName, // "...in project [Project Name]"
//							task.getTitle(), // Task title in details box
//							dueDateStr, // Due date in details box
//							taskUrl, // CTA button link
//							java.time.Year.now().getValue(), // Current year for footer
//							companyName // Company name for footer
//					);
//
//			// --- End of New Email Design ---
//
//			emailService.send(assignee.getEmail(), subject, htmlBody);
//		} catch (Exception e) {
//			// It's good practice to log this exception
//			// log.error("Failed to send task assignment email to {}: {}",
//			// assignee.getEmail(), e.getMessage());
//		}
//
//		return ResponseEntity.ok(task);
//	}

}