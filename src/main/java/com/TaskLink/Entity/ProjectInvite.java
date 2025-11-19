package com.TaskLink.Entity;





import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Data
@Builder
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@AllArgsConstructor
@Table(name = "project_invites",
       indexes = @Index(columnList = "token", unique = true))
public class ProjectInvite {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invited_by_id", nullable = false)
  private User invitedBy;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false, unique = true, length = 100)
  private String token;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InviteStatus status;

  // ✅ NEW: store what role the invited member will have once they accept
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ProjectMemberRole role;

  private LocalDateTime createdAt;
  private LocalDateTime expiresAt;

  public enum InviteStatus { PENDING, ACCEPTED, EXPIRED, CANCELLED }

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
    if (expiresAt == null) expiresAt = createdAt.plusDays(7);
    if (status == null) status = InviteStatus.PENDING;
    if (role == null) role = ProjectMemberRole.MEMBER; // default role
  }
}
