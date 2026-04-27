package com.isi.techcenter_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TechcenterBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TechcenterBackendApplication.class, args);
	}

}
