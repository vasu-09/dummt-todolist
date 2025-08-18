package com.om.To_Do.List.ecosystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.om.To_Do.List.ecosystem.client")
public class ToDoListEcosystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToDoListEcosystemApplication.class, args);
	}

}
