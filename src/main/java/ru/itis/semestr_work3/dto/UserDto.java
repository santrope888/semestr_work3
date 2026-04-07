package ru.itis.semestr_work3.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    @NotBlank(message = "Username is required")
    @Size(min = 5, max = 20, message = "Username between 5 and 20 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+?7|\\+?375|\\+?380|\\+?77)[\\s\\-]?\\(?\\d{3}\\)?[\\s\\-]?\\d{3}[\\s\\-]?\\d{2}[\\s\\-]?\\d{2}$\n",
            message = "Invalid phone number")
    private String phoneNumber;
}
