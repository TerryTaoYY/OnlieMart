package org.example.onlinemart.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

public class RegistrationRequest {
    @NotBlank
    private String username;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    // getters, setters
}
