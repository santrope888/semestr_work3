package ru.itis.semestr_work3.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class TaskController {
    @GetMapping("/")
    @ResponseBody
    public String task() {
        return "hello world";
    }

    @GetMapping("/gethello_html")
    public String task2() {
        return "index";
    }
}
