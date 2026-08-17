package com.liwx.laborder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.liwx")
@EnableFeignClients(basePackages = "com.liwx")
public class LabOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabOrderApplication.class, args);
    }

}
