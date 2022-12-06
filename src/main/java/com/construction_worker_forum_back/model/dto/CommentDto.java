package com.construction_worker_forum_back.model.dto;

import com.construction_worker_forum_back.model.dto.simple.CommentSimpleDto;
import com.construction_worker_forum_back.model.dto.simple.LikerSimpleDto;
import com.construction_worker_forum_back.model.dto.simple.PostSimpleDto;
import com.construction_worker_forum_back.model.dto.simple.UserSimpleDto;
import lombok.*;

import java.util.Date;
import java.util.List;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class CommentDto {
    private Long id;
    private String content;
    private Date createdAt;
    private Date updatedAt;
    private UserSimpleDto user;
    private PostSimpleDto post;
    private CommentSimpleDto parentComment;
    private List<LikerSimpleDto> likers;
    private Long subCommentsQuantity;
}
