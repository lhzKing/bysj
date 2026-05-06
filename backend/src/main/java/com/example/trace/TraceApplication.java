package com.example.trace;

import com.example.trace.config.CorsProperties;
import com.example.trace.config.TraceBatchProperties;
import com.example.trace.config.TraceDemoDataProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({CorsProperties.class, TraceDemoDataProperties.class, TraceBatchProperties.class})
public class TraceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TraceApplication.class, args);
    }
}
