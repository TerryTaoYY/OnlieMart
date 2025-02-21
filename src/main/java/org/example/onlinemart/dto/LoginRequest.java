package org.example.onlinemart.dto;

import javax.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    // getters, setters
}
