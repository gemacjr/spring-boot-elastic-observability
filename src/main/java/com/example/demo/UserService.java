package com.example.demo;

import co.elastic.apm.api.CaptureSpan;
import co.elastic.apm.api.ElasticApm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Shows up as its own span in the APM trace waterfall, wrapping the JDBC "INSERT INTO user" span
    @CaptureSpan("addUser")
    public User addUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        User saved = userRepository.save(user);
        ElasticApm.currentTransaction().setLabel("user.id", saved.getId());
        log.info("Saved user id={} name={}", saved.getId(), name);
        return saved;
    }

    @CaptureSpan("findAllUsers")
    public Iterable<User> findAll() {
        return userRepository.findAll();
    }
}
