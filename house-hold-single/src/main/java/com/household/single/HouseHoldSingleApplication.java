package com.household.single;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
        exclude = { SecurityAutoConfiguration.class }
)
@ComponentScan(
        basePackages = "com.household",
        excludeFilters = {
                // Exclude SecurityConfig from auth-user and wealth (use SingleSecurityConfig)
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.household\\.(authuser|wealth)\\.config\\.SecurityConfig"
                ),
                // Exclude CacheProperties from both (use CachePropertiesConfig instead)
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.household\\.(authuser|wealth)\\.config\\.CacheProperties"
                ),
                // Exclude @SpringBootApplication main classes
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.household\\.(authuser|wealth|gateway)\\..*Application"
                ),
                // Exclude Kafka events
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.household\\.(authuser|wealth)\\.event\\..*"
                ),
                // Exclude GlobalExceptionHandler from both (unified in single module)
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.household\\.(authuser|wealth)\\.exception\\..*"
                ),
                // Exclude Feign client interface
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.household\\.wealth\\.client\\..*"
                )
        }
)
@EnableScheduling
public class HouseHoldSingleApplication {

    public static void main(String[] args) {
        SpringApplication.run(HouseHoldSingleApplication.class, args);
    }
}
