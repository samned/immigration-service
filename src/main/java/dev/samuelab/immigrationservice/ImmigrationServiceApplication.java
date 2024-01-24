package dev.samuelab.immigrationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@SpringBootApplication
public class ImmigrationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImmigrationServiceApplication.class, args);
    }

}
