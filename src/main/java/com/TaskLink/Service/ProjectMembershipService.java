package com.TaskLink.Service;

import com.TaskLink.Entity.*;
import com.TaskLink.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectMembershipService {

	@Autowired
	private ProjectMemberRepository memberRepo;
	@Autowired
	private ProjectInviteRepository inviteRepo;
	@Autowired
	private ProjectRepository projectRepo;
	@Autowired
	private UserRepository userRepo;
	@Autowired
	private EmailService emailService;

	@Value("${app.frontend.base-url:https://tanuja-suvarnakar.github.io/tasklink-frontend}")
	private String frontendBaseUrl;

	// ✅ Check if user is member by ID
	public boolean isMember(Long projectId, Long userId) {
		if (userId == null)
			return false;
		return memberRepo.existsByProject_IdAndUser_Id(projectId, userId);
	}

	// ✅ Check if user (entity) is member by email or id
	public boolean isMember(Long projectId, User requester) {
		if (requester == null)
			return false;
		if (requester.getId() != null && memberRepo.existsByProject_IdAndUser_Id(projectId, requester.getId()))
			return true;
		String email = requester.getEmail();
		return email != null && memberRepo.existsByProject_IdAndUser_EmailIgnoreCase(projectId, email);
	}

	// ✅ Check if user is admin or owner
	public boolean isAdmin(Long projectId, Long userId) {
		if (userId == null)
			return false;
		return memberRepo.findByProject_IdAndUser_Id(projectId, userId)
				.map(pm -> pm.getRole() == ProjectMemberRole.ADMIN || pm.getRole() == ProjectMemberRole.OWNER)
				.orElse(false);
	}

	// ✅ List all project members (only for members)
	public List<ProjectMember> listMembers(Long projectId, User requester) {
		if (!isMember(projectId, requester))
			throw new AccessDeniedException("Forbidden");
		return memberRepo.findByProject_Id(projectId);
	}

	// ✅ Get current user's membership
	public Optional<ProjectMember> me(Long projectId, User requester) {
		if (requester == null) {
			log.info("membership.me: requester is null");
			return Optional.empty();
		}

		if (requester.getId() != null) {
			log.info("membership.me: looking up by user id={}, projectId={}", requester.getId(), projectId);
			var byId = memberRepo.findByProject_IdAndUser_Id(projectId, requester.getId());
			if (byId.isPresent()) {
				log.info("membership.me: found membership by id -> {}", byId.get().getRole());
				return byId;
			} else {
				log.info("membership.me: no membership found by id");
			}
		}

		if (requester.getEmail() != null) {
			log.info("membership.me: looking up by email={}, projectId={}", requester.getEmail(), projectId);
			var byEmail = memberRepo.findByProject_IdAndUser_EmailIgnoreCase(projectId, requester.getEmail());
			byEmail.ifPresent(pm -> log.info("membership.me: found membership by email -> {}", pm.getRole()));
			return byEmail;
		}

		log.info("membership.me: nothing found");
		return Optional.empty();
	}

	// ✅ Add a member directly (used on project creation)
	@Transactional
	public ProjectMember addMember(Long projectId, Long userId, ProjectMemberRole role) {
		Project p = projectRepo.findById(projectId).orElseThrow();
		User u = userRepo.findById(userId).orElseThrow();
		return memberRepo.save(ProjectMember.builder().project(p).user(u).role(role).build());
	}

	// ✅ Send an invite email with token
	@Transactional
	public ProjectInvite invite(Long projectId, User invitedBy, String email, ProjectMemberRole role) {
		Project project = projectRepo.findById(projectId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
		String token = java.util.UUID.randomUUID().toString();

		ProjectInvite invite = ProjectInvite.builder().project(project).invitedBy(invitedBy).email(email).token(token)
				.status(ProjectInvite.InviteStatus.PENDING).role(role).build();

		var savedInvite = inviteRepo.save(invite);

		String base = frontendBaseUrl.endsWith("/") ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
				: frontendBaseUrl;
		String link = base + "/accept-invite?token=" + token;

		String html = """
				<p>Hello,</p>
				<p>You’ve been invited to join project %s.</p>
				<p>Click below to accept your invitation:</p>
				<p><a href="%s">%s</a></p>
				<br>
				<p>If you didn’t expect this invite, you can safely ignore this email.</p>
				""".formatted(project.getName(), link, link);

		emailService.send(email, "Invitation to join project " + project.getName(), html);
		return savedInvite;
	}

//	public ProjectInvite invite(Long projectId, User invitedBy, String email, ProjectMemberRole role) {
//		Project project = projectRepo.findById(projectId)
//				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
//		String token = java.util.UUID.randomUUID().toString();
//
//		ProjectInvite invite = ProjectInvite.builder().project(project).invitedBy(invitedBy).email(email).token(token)
//				.status(ProjectInvite.InviteStatus.PENDING).role(role).build();
//
//		var savedInvite = inviteRepo.save(invite);
//
//		String base = frontendBaseUrl.endsWith("/") ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
//				: frontendBaseUrl;
//		String link = base + "/accept-invite?token=" + token;
//
//		// --- Start of New Email Design ---
//
//		// You can pull these from your application properties or define them here
//		String companyName = "YourCompanyName";
//		String logoUrl = "https://your-cdn.com/path/to/logo.png"; // A publicly accessible URL to your logo
//
//		String html = """
//				<!DOCTYPE html>
//				<html lang="en">
//				<head>
//				    <meta charset="UTF-8">
//				    <meta name="viewport" content="width=device-width, initial-scale=1.0">
//				    <title>Project Invitation</title>
//				</head>
//				<body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f4f4f7;">
//				    <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #f4f4f7;">
//				        <tr>
//				            <td align="center">
//				                <table width="600" border="0" cellspacing="0" cellpadding="0" style="background-color: #ffffff; margin-top: 20px; margin-bottom: 20px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
//				                    <!-- Header with Logo -->
//				                    <tr>
//				                        <td align="center" style="padding: 40px 0 30px 0; border-bottom: 1px solid #eeeeee;">
//				                            <img src="%s" alt="%s Logo" width="150" style="display: block;">
//				                        </td>
//				                    </tr>
//				                    <!-- Content Body -->
//				                    <tr>
//				                        <td style="padding: 40px 30px 40px 30px; color: #333333;">
//				                            <h1 style="font-size: 24px; margin: 0 0 20px 0; font-weight: 600;">You're Invited!</h1>
//				                            <p style="margin: 0 0 24px 0; font-size: 16px; line-height: 1.5;">
//				                                Hello,
//				                            </p>
//				                            <p style="margin: 0 0 24px 0; font-size: 16px; line-height: 1.5;">
//				                                <b>%s</b> has invited you to join the project "<b>%s</b>".
//				                            </p>
//				                            <p style="margin: 0 0 30px 0; font-size: 16px; line-height: 1.5;">
//				                                Click the button below to accept your invitation:
//				                            </p>
//				                            <!-- CTA Button -->
//				                            <table border="0" cellspacing="0" cellpadding="0" width="100%%">
//				                                <tr>
//				                                    <td align="center">
//				                                        <a href="%s" target="_blank" style="background-color: #007bff; color: #ffffff; text-decoration: none; padding: 15px 25px; border-radius: 5px; display: inline-block; font-size: 16px; font-weight: bold;">
//				                                            Accept Invitation
//				                                        </a>
//				                                    </td>
//				                                </tr>
//				                            </table>
//				                            <p style="margin: 30px 0 20px 0; font-size: 14px; line-height: 1.5; color: #555555;">
//				                                If the button above doesn't work, copy and paste this link into your browser:<br>
//				                                <a href="%s" target="_blank" style="color: #007bff; text-decoration: underline; word-break: break-all;">%s</a>
//				                            </p>
//				                            <p style="margin: 0; font-size: 14px; line-height: 1.5; color: #555555;">
//				                                If you didn't expect this invite, you can safely ignore this email.
//				                            </p>
//				                        </td>
//				                    </tr>
//				                    <!-- Footer -->
//				                    <tr>
//				                        <td align="center" style="padding: 20px 30px; background-color: #f9f9f9; border-top: 1px solid #eeeeee;">
//				                            <p style="margin: 0; color: #999999; font-size: 12px;">
//				                                © %d %s. All rights reserved.
//				                            </p>
//				                        </td>
//				                    </tr>
//				                </table>
//				            </td>
//				        </tr>
//				    </table>
//				</body>
//				</html>
//				"""
//				.formatted(logoUrl, // Logo image URL
//						companyName, // Logo alt text
//						invitedBy.getFirstname(), // Name of the person who invited
//						project.getName(), // Project name
//						link, // CTA button link
//						link, // Fallback link href
//						link, // Fallback link text
//						java.time.Year.now().getValue(), // Current year for footer
//						companyName // Company name for footer
//				);
//
//		// --- End of New Email Design ---
//
//		emailService.send(email, "Invitation to join project " + project.getName(), html);
//		return savedInvite;
//	}

	// ✅ Accept an invite
	@Transactional
	public ProjectMember accept(String token, User user) {
		ProjectInvite invite = inviteRepo.findByToken(token)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid invite"));

		if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now()))
			throw new ResponseStatusException(HttpStatus.GONE, "Invite expired");

		if (invite.getStatus() != ProjectInvite.InviteStatus.PENDING)
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Invite already used");

		if (!invite.getEmail().equalsIgnoreCase(user.getEmail()))
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invite not for this email");

		ProjectMember member = memberRepo.findByProject_IdAndUser_Id(invite.getProject().getId(), user.getId())
				.orElseGet(() -> memberRepo.save(ProjectMember.builder().project(invite.getProject()).user(user)
						.role(invite.getRole()).build()));

		invite.setStatus(ProjectInvite.InviteStatus.ACCEPTED);
		inviteRepo.save(invite);
		return member;
	}

	// ✅ Remove member (only for admin/owner)
	@Transactional
	public void removeMember(Long projectId, Long memberId, User requester) {
		if (!isAdmin(projectId, requester.getId()))
			throw new AccessDeniedException("Only admin can remove");

		ProjectMember pm = memberRepo.findById(memberId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

		if (!pm.getProject().getId().equals(projectId))
			throw new IllegalArgumentException("Member not in project");

		memberRepo.delete(pm);
	}

	public List<ProjectMember> findByProjectId(Long projectId) {
		return memberRepo.findByProject_Id(projectId);
	}

	public long countMembers(Long projectId) {
		return memberRepo.countByProjectId(projectId);
	}

}
