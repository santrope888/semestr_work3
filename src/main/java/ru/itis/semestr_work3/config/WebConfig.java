package ru.itis.semestr_work3.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.itis.semestr_work3.converter.StringToCategoryConverter;
import ru.itis.semestr_work3.converter.StringToLocalDateConverter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final StringToCategoryConverter stringToCategoryConverter;
    private final StringToLocalDateConverter stringToLocalDateConverter;

    @PostConstruct
    void logUploadDir() {
        Path absolute = Paths.get(uploadDir).toAbsolutePath().normalize();
        log.info("Upload dir resolved to: {} (exists={}, isDir={})",
                absolute,
                Files.exists(absolute),
                Files.isDirectory(absolute));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String avatarsPath = Paths.get(uploadDir, "avatars").toAbsolutePath() + "/";
        String carsPath = Paths.get(uploadDir, "cars").toAbsolutePath() + "/";

        log.info("Registering ResourceHandler /uploads/avatars/** -> {}", avatarsPath);
        log.info("Registering ResourceHandler /uploads/cars/**    -> {}", carsPath);

        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations("file:" + avatarsPath);

        registry.addResourceHandler("/uploads/cars/**")
                .addResourceLocations("file:" + carsPath);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToCategoryConverter);
        registry.addConverter(stringToLocalDateConverter);
    }
}