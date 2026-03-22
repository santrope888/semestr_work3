package ru.itis.semestr_work3;

import org.springframework.boot.SpringApplication;

public class TestSemestrWork3Application {

    public static void main(String[] args) {
        SpringApplication.from(SemestrWork3Application::main).with(TestcontainersConfiguration.class).run(args);
    }

}
