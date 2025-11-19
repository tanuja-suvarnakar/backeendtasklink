package com.TaskLink.Repository;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.TaskLink.Entity.Task;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
	List<Task> findByAssigneeId(Long userId);

	List<Task> findByProjectId(Long projectId);

	@Query("select t from Task t join ProjectMember m on m.project = t.project where m.user.id = :userId")
	List<Task> findAllVisibleToUser(@Param("userId") Long userId);

	@Query("select t from Task t join ProjectMember m on m.project = t.project where t.project.id = :projectId and m.user.id = :userId")
	List<Task> findVisibleByProject(@Param("projectId") Long projectId, @Param("userId") Long userId);

	@Query("""
			    select t from Task t
			    where t.id = :id
			      and exists (
			        select 1 from ProjectMember m
			        where m.project.id = t.project.id
			          and m.user.id = :userId
			      )
			""")
	Optional<Task> findByIdVisibleToUser(@Param("id") Long id, @Param("userId") Long userId);

	
	@Query("SELECT t FROM Task t WHERE t.assignee.id = :userId OR t.reporter.id = :userId")
	List<Task> findVisibleForUser(@Param("userId") Long userId);

}
