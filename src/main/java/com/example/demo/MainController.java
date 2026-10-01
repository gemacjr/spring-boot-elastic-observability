package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
public class MainController {
    private final UserService userService;

    public MainController(UserService userService) {
        this.userService = userService;
    }

    // Heartbeat POSTs here every 5s and asserts the body contains "Saved"
    @PostMapping("/add")
    public String addNewUser(@RequestParam String name, @RequestParam String email) {
        userService.addUser(name, email);
        return "Saved";
    }

    @GetMapping("/all")
    public Iterable<User> getAllUsers() {
        return userService.findAll();
    }
}
