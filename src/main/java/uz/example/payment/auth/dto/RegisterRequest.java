package uz.example.payment.auth.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username is required")
        @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "username must be 4-32 characters and contain only letters, digits, and _")
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password
) {
}
