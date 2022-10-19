package com.construction_worker_forum_back.controller;

import com.construction_worker_forum_back.model.dto.PostDto;
import com.construction_worker_forum_back.model.dto.PostRequestDto;
import com.construction_worker_forum_back.service.PostService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin("http://localhost:3000")
@RequestMapping("/api/post")
@AllArgsConstructor
public class PostController {

    private PostService postService;

    @GetMapping
    public List<PostDto> getAllPosts() {
        return postService.getAllPosts();
    }

    @GetMapping("/{id}")
    public PostDto getPostById(@PathVariable("id") Long id) {
        return postService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostDto createPost(@Valid @RequestBody PostRequestDto post) {
        return postService.createPost(post);
    }

    @PutMapping("/{id}")
    public PostDto updatePostById(@PathVariable("id") Long id, @RequestBody PostRequestDto post) {
        return postService.updatePostById(id, post);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deletePostById(@PathVariable("id") Long id) {
        if (postService.deleteById(id)) {
            return Map.of(
                    "ID", id + "",
                    "status", "Deleted successfully!"
            );
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
