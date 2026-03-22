package ru.itis.semestr_work3;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SemestrWork3ApplicationTests {

    @Test
    void contextLoads() {
    }

}
