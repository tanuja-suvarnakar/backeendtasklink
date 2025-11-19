package com.TaskLink.DTO;



import lombok.Data;

@Data
public class InviteRequest {
	private String email;
	private String role; // "ADMIN" or "MEMBER" (optional)
}