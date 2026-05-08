package com.cuit.academic;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cuit.academic.mapper")
public class AcademicBlockchainApplication {
    public static void main(String[] args) {
        SpringApplication.run(AcademicBlockchainApplication.class, args);
    }
}
