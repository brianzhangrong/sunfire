package com.ihomefnt.sunfire.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
@EnableWebSecurity
@ComponentScan("com.ihomefnt.sunfire")
public class SunFireAdminLaunth {

    public static void main(String[] args) {
        SpringApplication.run(SunFireAdminLaunth.class, args);
    }
}
