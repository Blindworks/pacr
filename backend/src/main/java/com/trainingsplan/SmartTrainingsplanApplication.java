package com.trainingsplan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(
        basePackages = "com.trainingsplan",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.trainingsplan\\.pacr\\.ai\\..*"
        )
)
public class SmartTrainingsplanApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartTrainingsplanApplication.class, args);
    }
}
