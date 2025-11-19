package com.TaskLink.Service;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.TaskLink.Entity.Project;
import com.TaskLink.Entity.Task;
import com.TaskLink.Entity.User;
import com.TaskLink.Repository.ProjectRepository;
import com.TaskLink.Repository.TaskRepository;
import com.TaskLink.Repository.UserRepository;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProjectRepository projectRepository;

	public Task create(Task t, Long assigneeId, Long projectId) {
		if (assigneeId != null) {
			Optional<User> uu = userRepository.findById(assigneeId);
			uu.ifPresent(t::setAssignee);
		}
		if (projectId != null) {
			Optional<Project> pp = projectRepository.findById(projectId);
			pp.ifPresent(t::setProject);
		}
		return taskRepository.save(t);
	}

	public List<Task> listAll() {
		return taskRepository.findAll();
	}

	public Optional<Task> get(Long id) {
		return taskRepository.findById(id);
	}

	public void delete(Long id) {
		taskRepository.deleteById(id);
	}
}
