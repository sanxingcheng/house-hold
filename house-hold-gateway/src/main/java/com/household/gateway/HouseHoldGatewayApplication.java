package com.household.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class HouseHoldGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(HouseHoldGatewayApplication.class, args);
    }
}
