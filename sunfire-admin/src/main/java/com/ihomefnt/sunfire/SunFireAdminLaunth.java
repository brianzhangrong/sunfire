package com.ihomefnt.sunfire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.ihomefnt")
public class SunFireAdminLaunth {

    public static void main(String[] args) {
        SpringApplication.run(SunFireAdminLaunth.class, args);
    }
}
