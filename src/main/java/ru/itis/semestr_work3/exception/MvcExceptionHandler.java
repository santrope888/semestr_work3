package ru.itis.semestr_work3.exception;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice(
        basePackages = "ru.itis.semestr_work3.controllers",
        annotations = org.springframework.stereotype.Controller.class
)
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
    public String handleSecurity(SecurityException e,
                                 Model model,
                                 HttpServletResponse response) {
        log.warn("Security exception: {}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        model.addAttribute("errorMessage", e.getMessage());
        return "error/403";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException e,
                                     Model model,
                                     HttpServletResponse response) {
        log.warn("Access denied: {}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        model.addAttribute("errorMessage", "Доступ запрещён");
        return "error/403";
    }

    @ExceptionHandler(ExternalServiceException.class)
    public String handleExternalService(ExternalServiceException e,
                                        Model model,
                                        HttpServletResponse response) {
        log.error("External service error: {}", e.getMessage(), e);
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        model.addAttribute("errorMessage", e.getMessage());
        return "error/500";
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