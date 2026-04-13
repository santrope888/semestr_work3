package ru.itis.semestr_work3.exception;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice(basePackages = "ru.itis.semestr_work3.controllers",
        basePackageClasses = {})
public class MvcExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException e,
                                 Model model,
                                 HttpServletResponse response) {
        log.warn("Resource not found: {}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        model.addAttribute("errorMessage", e.getMessage());
        return "error/404";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleBadRequest(IllegalArgumentException e,
                                   Model model,
                                   HttpServletResponse response) {
        log.warn("Bad request: {}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        model.addAttribute("errorMessage", e.getMessage());
        return "error/400";
    }

    @ExceptionHandler(SecurityException.class)
    public String handleForbidden(SecurityException e,
                                  Model model,
                                  HttpServletResponse response) {
        log.warn("Access denied: {}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        model.addAttribute("errorMessage", e.getMessage());
        return "error/403";
    }

    @ExceptionHandler(Exception.class)
    public String handleOther(Exception e,
                              Model model,
                              HttpServletResponse response) {
        log.error("Unexpected MVC error: {}", e.getMessage(), e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        model.addAttribute("errorMessage", "Произошла внутренняя ошибка сервера");
        return "error/500";
    }
}