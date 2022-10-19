package com.construction_worker_forum_back.model.dto;

import com.construction_worker_forum_back.model.entity.AccountStatus;
import com.construction_worker_forum_back.model.entity.Comment;
import com.construction_worker_forum_back.model.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Date createdAt;
    private Date updatedAt;
    private AccountStatus accountStatus;
    private Role userRoles;
    private List<Comment> userComments;
}
