package org.example.onlinemart.controller;

import org.example.onlinemart.exception.InvalidCredentialsException;
import org.example.onlinemart.service.UserService;
import org.example.onlinemart.dto.LoginRequest;
import org.example.onlinemart.dto.RegistrationRequest;
import org.example.onlinemart.entity.User;
import org.example.onlinemart.security.JwtTokenUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(UserService userService, JwtTokenUtil jwtTokenUtil, BCryptPasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegistrationRequest dto) {
        User newUser = new User();
        newUser.setUsername(dto.getUsername());
        newUser.setEmail(dto.getEmail());
        newUser.setPassword(dto.getPassword());
        userService.register(newUser);
        Map<String, String> response = Collections.singletonMap("message", "Registration successful");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());
        if (user == null) {
            throw new InvalidCredentialsException("Incorrect credentials, please try again.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Incorrect credentials, please try again.");
        }

        String token = jwtTokenUtil.generateToken(
                user.getUsername(),
                user.getRole().name(),
                user.getUserId()
        );

        return ResponseEntity.ok(token);
    }
}