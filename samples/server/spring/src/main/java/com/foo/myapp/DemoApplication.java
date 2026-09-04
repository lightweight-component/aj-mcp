package com.foo.myapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Represents demo application.
 */
@SpringBootApplication
public class DemoApplication {
    /**
     * Executes the main operation.
     * @param args the args value.
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}