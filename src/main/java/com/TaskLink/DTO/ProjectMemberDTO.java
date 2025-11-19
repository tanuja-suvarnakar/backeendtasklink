package com.TaskLink.DTO;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMemberDTO {
    private Long userId;
    private String name;
    private String email;
    private String role; // OWNER, ADMIN, MEMBER
}
