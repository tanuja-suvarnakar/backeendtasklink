package com.TaskLink.DTO;



import lombok.Data;

@Data
public class RegisterRequest {
	private String email;
	private String password;
	 private String inviteToken; 
	private String firstname;
	private String role;
	private String lastname;
}