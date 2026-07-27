package com.devlil0.tramposeditor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TramposEditorApplicationTests {

    @Test
    void contextLoads() {
    }

}
