package ru.practicum.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.grpc.stats.UserActionProto;
import ru.practicum.kafka.KafkaAvroProducer;
import ru.practicum.mapper.CollectorMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventHandler {

    @Value("${user.topic}")
    private String topic;
    private final KafkaAvroProducer producer;

    public void handle(UserActionProto action) {
        log.info("UserEventHandler received action {}", action);
        producer.send(topic, CollectorMapper.map(action));
    }
}
