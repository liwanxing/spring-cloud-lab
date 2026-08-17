package com.liwx.labuser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.liwx")
public class LabUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabUserApplication.class, args);
    }

}
