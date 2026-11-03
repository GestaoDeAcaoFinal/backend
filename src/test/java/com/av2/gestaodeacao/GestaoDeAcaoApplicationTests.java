package com.av2.gestaodeacao;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.config.import=")
@ActiveProfiles("test")
class GestaoDeAcaoApplicationTests {

    @Test
    void contextLoads() {
    }

}
