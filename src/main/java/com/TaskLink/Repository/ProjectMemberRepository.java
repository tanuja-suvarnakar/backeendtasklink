package com.TaskLink.Repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.TaskLink.Entity.ProjectMember;

import java.util.*;


@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    boolean existsByProject_IdAndUser_Id(Long projectId, Long userId);

    boolean existsByProject_IdAndUser_EmailIgnoreCase(Long projectId, String email);

    Optional<ProjectMember> findByProject_IdAndUser_Id(Long projectId, Long userId);

    Optional<ProjectMember> findByProject_IdAndUser_EmailIgnoreCase(Long projectId, String email);

    List<ProjectMember> findByProject_Id(Long projectId);

	long countByProjectId(Long projectId);
    
}
