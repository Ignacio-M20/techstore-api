package cl.techstore.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;



@SpringBootApplication
@EnableAsync
public class TechstoreApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechstoreApiApplication.class, args);
    }
}
