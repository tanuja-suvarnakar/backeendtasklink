package com.TaskLink.Controller;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.TaskLink.Entity.*;
import com.TaskLink.Repository.ProjectInviteRepository;
import com.TaskLink.Service.*;

@RestController
@RequestMapping("/api/projects/invites")
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:4200", "http://192.168.0.232:4200" })
public class ProjectInviteController {

	@Autowired
	private ProjectMembershipService membershipService;
	@Autowired
	private CurrentUserService currentUser;
	@Autowired
	private ProjectInviteRepository inviteRepo;

	@PostMapping("/accept")
	public ResponseEntity<ProjectMember> accept(@RequestParam("token") String token) {
		User me = currentUser.requireCurrentUser();
		return ResponseEntity.ok(membershipService.accept(token, me));
	}

}