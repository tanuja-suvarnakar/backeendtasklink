package com.TaskLink.Controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.TaskLink.DTO.AuthRequest;
import com.TaskLink.DTO.AuthResponse;
import com.TaskLink.DTO.RegisterRequest;
import com.TaskLink.Entity.ProjectInvite;
import com.TaskLink.Entity.User;
import com.TaskLink.Repository.ProjectInviteRepository;
import com.TaskLink.Repository.UserRepository;
import com.TaskLink.Security.JwtUtil;
import com.TaskLink.Service.ProjectMembershipService;
import com.TaskLink.Service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private ProjectInviteRepository inviteRepo;

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProjectMembershipService membershipService;

	// ✅ REGISTER (handles invite acceptance)
	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
		if (userRepository.findByEmail(req.getEmail()).isPresent()) {
			return ResponseEntity.badRequest().body("Email already used");
		}

		User user = userService.register(req.getEmail(), req.getPassword(), req.getFirstname(), req.getLastname(),
				req.getRole());

		// ✅ If registering via invite link
		if (req.getInviteToken() != null && !req.getInviteToken().isEmpty()) {
			ProjectInvite invite = inviteRepo.findByToken(req.getInviteToken())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid invite"));

			if (!invite.getEmail().equalsIgnoreCase(user.getEmail())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invite not for this email");
			}

			// ✅ Mark invite accepted and add to project_members
			membershipService.accept(req.getInviteToken(), user);
		}

		String token = jwtUtil.generateToken(user.getEmail());
		return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getFirstname(), user.getLastname()));
	}

	// ✅ LOGIN (optional: also handle invite acceptance)
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody AuthRequest req) {
		try {
			authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
		} catch (AuthenticationException e) {
			return ResponseEntity.status(401).body("Invalid credentials");
		}

		User user = userRepository.findByEmail(req.getEmail())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		// ✅ If invite token is sent (user logs in via invite link)
		if (req.getInviteToken() != null && !req.getInviteToken().isEmpty()) {
			ProjectInvite invite = inviteRepo.findByToken(req.getInviteToken())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid invite"));

			if (invite.getStatus() == ProjectInvite.InviteStatus.PENDING
					&& invite.getEmail().equalsIgnoreCase(user.getEmail())) {
				membershipService.accept(req.getInviteToken(), user);
			}
		}

		String token = jwtUtil.generateToken(req.getEmail());
		return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getFirstname(), user.getLastname()));
	}

	// ✅ Verify invite endpoint (for prefill info in frontend)
	@GetMapping("/verify")
	public ResponseEntity<?> verify(@RequestParam("token") String token) {
		ProjectInvite invite = inviteRepo.findByToken(token)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid invite"));

		if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now()))
			throw new ResponseStatusException(HttpStatus.GONE, "Invite expired");

		Map<String, Object> response = Map.of("email", invite.getEmail(), "projectName", invite.getProject().getName(),
				"role", invite.getRole(), "status", invite.getStatus());

		return ResponseEntity.ok(response);
	}

	@GetMapping("/check-user")
	public ResponseEntity<Boolean> checkUserExists(@RequestParam String email) {
		boolean exists = userRepository.existsByEmailIgnoreCase(email);
		return ResponseEntity.ok(exists);
	}

}
