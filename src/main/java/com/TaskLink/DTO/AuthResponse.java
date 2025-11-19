package com.TaskLink.DTO;


import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
	private String token;

	private String email;

	private String firstname;

	private String lastname;
}
