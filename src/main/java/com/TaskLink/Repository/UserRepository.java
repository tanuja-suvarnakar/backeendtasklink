package com.TaskLink.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.TaskLink.Entity.User;

import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	  Optional<User> findByEmail(String email);
	  Optional<User> findByEmailIgnoreCase(String email);
	boolean existsByEmailIgnoreCase(String email);
	}