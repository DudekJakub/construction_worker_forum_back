package com.construction_worker_forum_back.service;

import com.construction_worker_forum_back.client.NotificationClient;
import com.construction_worker_forum_back.model.Notification;
import com.construction_worker_forum_back.model.dto.PostDto;
import com.construction_worker_forum_back.model.dto.PostRequestDto;
import com.construction_worker_forum_back.model.dto.TopicDto;
import com.construction_worker_forum_back.model.dto.UserDto;
import com.construction_worker_forum_back.model.dto.simple.LikerSimpleDto;
import com.construction_worker_forum_back.model.entity.Post;
import com.construction_worker_forum_back.model.entity.Topic;
import com.construction_worker_forum_back.model.entity.User;
import com.construction_worker_forum_back.repository.PostRepository;
import com.construction_worker_forum_back.repository.UserRepository;
import com.construction_worker_forum_back.validation.EntityUpdateUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final TopicService topicService;
    private final ModelMapper modelMapper;
    private final NotificationClient notificationClient;

    public List<PostDto> getAllPosts() {
        return postRepository
                .findAll()
                .stream()
                .map(post -> modelMapper.map(post, PostDto.class))
                .toList();
    }

    public List<PostDto> getPostsByUsername(String username) {
        return postRepository
                .findByUser_UsernameIgnoreCase(username)
                .stream()
                .map(post -> modelMapper.map(post, PostDto.class))
                .toList();
    }

    public List<PostDto> getPostsByTopicId(
            Long topicId,
            Optional<String> orderBy,
            Optional<Integer> limit,
            Optional<Integer> page
    ) {
        if (limit.isPresent() && page.isPresent() && orderBy.isPresent()) {
            return getPaginatedAndSortedNumberOfPosts(topicId, limit.get(), page.get(), orderBy.get());
        }
        if (orderBy.isPresent()) {
            return getSortedPosts(topicId, orderBy.get());
        }
        if (limit.isPresent() && page.isPresent()) {
            return getPaginatedNumberOfPosts(topicId, limit.get(), page.get());
        }
        return postRepository
                .findByTopic_Id(topicId)
                .stream()
                .map(post -> modelMapper.map(post, PostDto.class))
                .toList();
    }

    public List<LikerSimpleDto> getPostLikers(Long id) {
        return postRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
                .getLikers()
                .stream()
                .map(user -> modelMapper.map(user, LikerSimpleDto.class))
                .toList();
    }

    @Cacheable(value = "postCache", key = "{#id}")
    public Optional<PostDto> findById(Long id) {
        return postRepository.findById(id)
                .map(post -> modelMapper.map(post, PostDto.class));
    }

    @Transactional
    public PostDto createPost(PostRequestDto postRequestDto) {
        Post postToSave = modelMapper.map(postRequestDto, Post.class);
        UserDto userById = userService
                .findById(postRequestDto.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        TopicDto topicById = topicService
                .findTopicById(postRequestDto.getTopicId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        postToSave.setUser(modelMapper.map(userById, User.class));
        postToSave.setTopic(modelMapper.map(topicById, Topic.class));

        return modelMapper.map(postRepository.save(postToSave), PostDto.class);
    }

    @Transactional
    public PostDto likePost(Long postId, Long userId) {
        Post postFromDb = postRepository
                .findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        User userById = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (postFromDb.getLikers().contains(userById)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Post already liked by user!");
        }

        postFromDb.getLikers().add(userById);
        userById.getLikedPosts().add(postFromDb);

        notificationClient.sendNotification(
                        Notification.of(
                                userById.getUsername(),
                                postFromDb.getUser().getId().toString(),
                                "Liked yor post!",
                                "https://127.0.0.1:3000/post/" + postFromDb.getId(),
                                false
                        )
                )
                .doOnNext(notification -> log.info("Notification Response: {}", notification))
                .doOnError(e -> log.info("Error occurred: {}", e.getMessage()))
                .subscribe();

        return modelMapper.map(postFromDb, PostDto.class);
    }

    @Transactional
    @CachePut(value = "postCache", key = "{#id}")
    public PostDto updatePostById(Long id, PostRequestDto postRequestDto) {
        Post postFromDb = postRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        EntityUpdateUtil.throwIfEditorIsUserAndTimeIsExpired(postFromDb);

        TopicDto topicById = topicService
                .findTopicById(postRequestDto.getTopicId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));


        EntityUpdateUtil.setEntityLastEditor(userRepository, postFromDb, postRequestDto.getUserId());
        modelMapper.map(postRequestDto, postFromDb);
        modelMapper.map(postRequestDto, topicById);
        postFromDb.setUpdatedAt(Date.from(Instant.now()));
        postFromDb.setTopic(modelMapper.map(topicById, Topic.class));
        postFromDb.setKeywords(postRequestDto.getKeywords());

        return modelMapper.map(postFromDb, PostDto.class);
    }

    @Transactional
    @CacheEvict(value = "postCache", key = "{#id}")
    public boolean deleteById(Long id) {
        return postRepository.deletePostById(id) == 1;
    }

    @Transactional
    public boolean unlikePost(Long postId, Long userId) {
        Post postFromDb = postRepository
                .findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        User userById = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return postFromDb.getLikers().remove(userById) && userById.getLikedPosts().remove(postFromDb);
    }

    public List<PostDto> findPostByContentOrTitle(String contentOrTitle) {
        return postRepository.findByTitleContainsIgnoreCaseOrContentContainsIgnoreCase(contentOrTitle, contentOrTitle)
                .stream()
                .map(post -> modelMapper.map(post, PostDto.class))
                .toList();

    }

    public List<PostDto> getPaginatedNumberOfPosts(Long topicId, Integer number, Integer page) {
        Pageable pageWithExactNumberOfElements = PageRequest.of(page - 1, number);
        return getListOfPostsByPageableObject(topicId, pageWithExactNumberOfElements);
    }

    public List<PostDto> getListOfPostsByPageableObject(Long topicId, Pageable pageable) {
        return postRepository.findAllPaginatedByTopic_Id(topicId, pageable)
                .stream()
                .map(post -> modelMapper.map(post, PostDto.class))
                .collect(Collectors.toList());
    }

    public List<PostDto> getSortedPosts(Long topicId, String orderBy) {
        String[] splitted = orderBy.split("\\.");
        String sortBy = splitted[0];
        String direction = splitted[1];
        if (direction.equalsIgnoreCase("asc")) {
            Sort sortPostsAscending = Sort.by(Sort.Direction.ASC, sortBy);

            return getListOfPostsBySort(topicId, sortPostsAscending);
        }
        Sort sortPostsDescending = Sort.by(Sort.Direction.DESC, sortBy);

        return getListOfPostsBySort(topicId, sortPostsDescending);
    }

    public List<PostDto> getListOfPostsBySort(Long topicId, Sort sort) {
        return postRepository.findAllSortedByTopic_Id(topicId, sort)
                .stream()
                .map(post -> modelMapper.map(post, PostDto.class))
                .collect(Collectors.toList());
    }

    public List<PostDto> getPaginatedAndSortedNumberOfPosts(Long topicId, Integer number, Integer page, String orderBy) {
        String[] splitted = orderBy.split("\\.");
        String sortBy = splitted[0];
        String direction = splitted[1];
        if (direction.equalsIgnoreCase("asc")) {
            Pageable paginatedAndSortedAscending = PageRequest.of(page - 1, number, Sort.by(sortBy).ascending());
            return getListOfPostsByPageableObject(topicId, paginatedAndSortedAscending);
        }
        Pageable paginatedAndSortedDescending = PageRequest.of(page - 1, number, Sort.by(sortBy).descending());

        return getListOfPostsByPageableObject(topicId, paginatedAndSortedDescending);
    }
}
