package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.practicum.processor.EventProcessor;
import ru.practicum.processor.UserProcessor;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Analyzer {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Analyzer.class, args);

        EventProcessor eventProcessor = context.getBean(EventProcessor.class);
        final UserProcessor userProcessor = context.getBean(UserProcessor.class);

        Thread userThread = new Thread(userProcessor);
        userThread.setName("eventHandlerThread");
        userThread.start();

        eventProcessor.start();
    }
}