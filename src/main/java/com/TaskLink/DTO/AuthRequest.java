package com.TaskLink.DTO;


import lombok.Data;

@Data
public class AuthRequest {
	private String email;
	private String password;
	 private String inviteToken; 
}
