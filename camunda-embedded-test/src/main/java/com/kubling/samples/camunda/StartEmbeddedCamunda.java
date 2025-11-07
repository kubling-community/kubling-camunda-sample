package com.kubling.samples.camunda;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ComponentScan(basePackages = {"com.kubling"})
@EnableAutoConfiguration(exclude = {ErrorMvcAutoConfiguration.class})
@EnableTransactionManagement
@Slf4j
public class StartEmbeddedCamunda {

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(StartEmbeddedCamunda.class);
        app.setBannerMode(Banner.Mode.OFF);

        app.run(args);
    }

}
