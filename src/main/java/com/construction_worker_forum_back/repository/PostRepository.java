package com.construction_worker_forum_back.repository;

import com.construction_worker_forum_back.model.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByUser_UsernameIgnoreCase(@NonNull String username);

    List<Post> findByTopic_Id(@NonNull Long id);

    int deletePostById(Long postId);

    List<Post> findByTitleContainsIgnoreCaseOrContentContainsIgnoreCase(String title, String content);

    List<Post> findAllPaginatedByTopic_Id(Long id, Pageable pageable);

    List<Post> findAllSortedByTopic_Id(Long id, Sort sort);
}
