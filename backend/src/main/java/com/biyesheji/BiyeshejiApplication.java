package com.biyesheji;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.biyesheji.mapper")
@EnableScheduling
public class BiyeshejiApplication {
    public static void main(String[] args) {
        SpringApplication.run(BiyeshejiApplication.class, args);
    }
}
