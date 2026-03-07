package com.household.authuser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(
        exclude = { SecurityAutoConfiguration.class },
        scanBasePackages = "com.household"
)
@EnableDiscoveryClient
public class HouseHoldAuthUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(HouseHoldAuthUserApplication.class, args);
    }
}
