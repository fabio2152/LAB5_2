package com.lab5.marketing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MarketingAiApplication {

    public static void main(String[] args) {
        // Java2D se usa para componer las fotos; en un contenedor no hay pantalla
        System.setProperty("java.awt.headless", "true");
        SpringApplication.run(MarketingAiApplication.class, args);
    }
}
