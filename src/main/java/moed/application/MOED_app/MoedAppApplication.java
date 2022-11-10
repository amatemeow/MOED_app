package moed.application.MOED_app;

import moed.application.MOED_app.business.DataModeller;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class MoedAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoedAppApplication.class, args);
	}

}
