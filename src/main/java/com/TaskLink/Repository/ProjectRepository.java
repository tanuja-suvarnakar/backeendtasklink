package com.TaskLink.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.TaskLink.Entity.Project;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
}
