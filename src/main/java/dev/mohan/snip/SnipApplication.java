package dev.mohan.snip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SnipApplication {

    public static void main(String[] args) {
        SpringApplication.run(SnipApplication.class, args);
    }
}
