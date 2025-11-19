package com.TaskLink.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.TaskLink.Entity.User;
import com.TaskLink.Repository.UserRepository;
import com.TaskLink.Security.JwtUtil;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
@Slf4j
public class CurrentUserService {

	@Autowired
	private UserRepository userRepository;

	public User requireCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getName() == null) {

			throw new RuntimeException("Unauthenticated");
		}

		var user = userRepository.findByEmail(auth.getName()).orElseThrow(() -> {

			return new RuntimeException("Unauthenticated");
		});

		return user;
	}
}
