package com.av2.gestaodeacao;

import com.av2.gestaodeacao.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class GestaoDeAcaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestaoDeAcaoApplication.class, args);
    }

}
