package com.TaskLink.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.TaskLink.Entity.Project;
import com.TaskLink.Repository.ProjectMemberRepository;
import com.TaskLink.Repository.ProjectRepository;
import com.TaskLink.Repository.TaskRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.TaskLink.DTO.ActivityDTO;
import com.TaskLink.DTO.InviteRequest;
import com.TaskLink.DTO.ProjectMemberDTO;
import com.TaskLink.DTO.TaskDTO;
import com.TaskLink.DTO.UserSummaryDTO;
import com.TaskLink.Entity.*;

import com.TaskLink.Service.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:4200", "http://192.168.0.232:4200" })
@Slf4j
public class ProjectController {

	@Autowired
	private TaskRepository taskRepository;
	@Autowired
	private ProjectRepository projectRepository;
	@Autowired
	private CurrentUserService currentUser;
	@Autowired
	private ProjectMembershipService membership;
	@Autowired
	private ProjectMemberRepository memberRepo;

	@Autowired

	@GetMapping
	public List<Project> all() {
// TODO: optionally filter to projects where requester is member
		return projectRepository.findAll();
	}

	@PostMapping
	public ResponseEntity<Project> create(@RequestBody Project p) {
		Project saved = projectRepository.save(p);
		User me = currentUser.requireCurrentUser();
		membership.addMember(saved.getId(), me.getId(), ProjectMemberRole.OWNER);
		return ResponseEntity.ok(saved);
	}

	@GetMapping("/{id}/tasks")
	public ResponseEntity<?> tasksByProject(@PathVariable Long id) {
		User me = currentUser.requireCurrentUser();

		if (!membership.isMember(id, me))
			return ResponseEntity.status(403).body("Forbidden");

		List<Task> tasks = taskRepository.findVisibleByProject(id, me.getId());
		List<TaskDTO> dtos = tasks.stream().map(task -> {
			TaskDTO dto = new TaskDTO();

			dto.setId(task.getId()); // ✅ include ID
			dto.setTitle(task.getTitle());
			dto.setDescription(task.getDescription());
			dto.setStatus(task.getStatus());
			dto.setDueDate(task.getDueDate());
			dto.setAssigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null);
			dto.setReporterId(task.getReporter() != null ? task.getReporter().getId() : null);
			dto.setProjectId(task.getProject() != null ? task.getProject().getId() : null);

			return dto;
		}).toList();

		return ResponseEntity.ok(dtos);
	}

	@GetMapping("/{id}/members/list")
	public ResponseEntity<?> listAllMembers(@PathVariable Long id) {
		User me = currentUser.requireCurrentUser();

		// ensure current user is at least a member
		if (!membership.isMember(id, me.getId())) {
			return ResponseEntity.status(403).body("Forbidden");
		}

		// fetch all members of that project
		var members = membership.findByProjectId(id);

		List<ProjectMemberDTO> result = members.stream().map(m -> {
			User u = m.getUser();
			return new ProjectMemberDTO(u.getId(), u.getFirstname(), u.getEmail(), m.getRole().name());
		}).toList();

		return ResponseEntity.ok(result);
	}

	@GetMapping("/{id}/me")
	public ResponseEntity<?> myMembership(@PathVariable Long id) {
		User me = currentUser.requireCurrentUser();
		return membership.me(id, me).<ResponseEntity<?>>map(ResponseEntity::ok)
				.orElse(ResponseEntity.status(403).body("Forbidden"));
	}

	@PostMapping("/{id}/members/invite")
	public ResponseEntity<?> invite(@PathVariable Long id, @RequestBody InviteRequest req) {
		User me = currentUser.requireCurrentUser();
		var my = membership.me(id, me)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member"));
		if (my.getRole() != ProjectMemberRole.OWNER) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only project creator can invite");
		}

		var invite = membership.invite(id, me, req.getEmail(),
				"ADMIN".equalsIgnoreCase(req.getRole()) ? ProjectMemberRole.ADMIN : ProjectMemberRole.MEMBER);

		// return a small DTO / map to avoid lazy-loading issues
		Map<String, Object> resp = Map.of("id", invite.getId(), "token", invite.getToken(), "email", invite.getEmail(),
				"role", invite.getRole().name(), "status", invite.getStatus().name());
		return ResponseEntity.ok(resp);
	}

	@DeleteMapping("/{id}/members/{memberId}")
	public ResponseEntity<?> remove(@PathVariable Long id, @PathVariable Long memberId) {
		User me = currentUser.requireCurrentUser();
		membership.removeMember(id, memberId, me);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getById(@PathVariable Long id) {
		User me = currentUser.requireCurrentUser();
		return projectRepository.findById(id).<ResponseEntity<?>>map(p -> {
			if (!membership.isMember(id, me.getId())) {
				return ResponseEntity.status(403).body("Forbidden");
			}
			return ResponseEntity.ok(p);
		}).orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/summary")
	public ResponseEntity<?> getUserSummary() {
		User me = currentUser.requireCurrentUser();

		// Fetch all tasks assigned to/visible to this user
		List<Task> tasks = taskRepository.findVisibleForUser(me.getId());

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime weekAgo = now.minusDays(7);

		// Task status summary
		Map<String, Long> statusCount = tasks.stream()
				.collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));

		long totalTasks = tasks.size();
		long completedTasks = tasks.stream().filter(t -> "DONE".equalsIgnoreCase(t.getStatus())).count();
		long dueSoon = tasks.stream().filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(now.plusDays(7)))
				.count();
		long createdLast7 = tasks.stream().filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(weekAgo))
				.count();
		long updatedLast7 = tasks.stream().filter(t -> t.getUpdatedAt() != null && t.getUpdatedAt().isAfter(weekAgo))
				.count();

		// Recent activity
		List<ActivityDTO> recent = tasks.stream().filter(t -> t.getUpdatedAt() != null)
				.sorted(Comparator.comparing(Task::getUpdatedAt).reversed()).limit(5)
				.map(t -> new ActivityDTO(
						t.getReporter() != null ? t.getReporter().getFirstname() + " " + t.getReporter().getLastname()
								: "Unknown",
						t.getCreatedAt().equals(t.getUpdatedAt()) ? "created" : "updated", t.getTitle(), t.getStatus(),
						t.getUpdatedAt()))
				.toList();

		UserSummaryDTO summary = new UserSummaryDTO(totalTasks, completedTasks, createdLast7, updatedLast7, dueSoon,
				statusCount, recent);

		return ResponseEntity.ok(summary);
	}

}