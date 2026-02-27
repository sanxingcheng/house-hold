package com.household.authuser;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@MapperScan("com.household.authuser.mapper")
public class HouseHoldAuthUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(HouseHoldAuthUserApplication.class, args);
    }
}
