package com.mokujin.test;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@Slf4j
@SpringBootApplication
@EnableEurekaClient
public class TestServiceApplication {

    public static void main(String[] args) {
        log.info("app is running");
        SpringApplication.run(TestServiceApplication.class, args);
    }
}
