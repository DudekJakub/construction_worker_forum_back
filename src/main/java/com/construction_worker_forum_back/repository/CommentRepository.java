package com.construction_worker_forum_back.repository;

import com.construction_worker_forum_back.model.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    int deleteCommentById(Long id);
}
