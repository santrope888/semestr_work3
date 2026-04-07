package ru.itis.semestr_work3.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.itis.semestr_work3.dto.UserDto;
import ru.itis.semestr_work3.service.UserService;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final UserDetailsService userDetailsService;
    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(UserDto userDto, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.register(userDto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }

        return "redirect:/login";
    }
}
