package com.liwx.laborder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.liwx")
public class LabOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabOrderApplication.class, args);
    }

}
