package com.construction_worker_forum_back;

import com.construction_worker_forum_back.model.entity.User;
import com.construction_worker_forum_back.repository.UserRepository;
import com.construction_worker_forum_back.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ConstructionWorkerForumBackApplicationTests {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Test
    void contextLoads() {
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void saveUser() {
        User user = User.builder()
                .username("jake")
                .password("secret")
                .email("jake@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        userService.register(user);

        assertTrue(userRepository.existsByUsernameIgnoreCase(user.getUsername()));
    }
}
