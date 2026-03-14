package com.household.wealth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.household")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.household.wealth.client")
@EnableScheduling
public class HouseHoldWealthApplication {

    public static void main(String[] args) {
        SpringApplication.run(HouseHoldWealthApplication.class, args);
    }
}
