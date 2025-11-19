package com.TaskLink.Repository;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.TaskLink.Entity.ProjectInvite;

import java.util.*;
@Repository
public interface ProjectInviteRepository extends JpaRepository<ProjectInvite, Long> {
  Optional<ProjectInvite> findByToken(String token);
  List<ProjectInvite> findByProjectIdAndStatus(Long projectId, ProjectInvite.InviteStatus status);
}
