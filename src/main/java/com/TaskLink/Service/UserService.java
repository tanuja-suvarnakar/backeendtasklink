package com.TaskLink.Service;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.TaskLink.Entity.User;
import com.TaskLink.Repository.UserRepository;

@Service
public class UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	public User register(String email, String rawPassword, String firstname, String lastname, String role) {
		User u = User.builder().email(email).password(passwordEncoder.encode(rawPassword)).firstname(firstname)
				.lastname(lastname).role(role == null ? "USER" : role).build();
		return userRepository.save(u);
	}

}
