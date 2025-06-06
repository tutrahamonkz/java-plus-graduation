package ru.practicum.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class RecordHandler {
    private static final double VIEW = 0.4;
    private static final double LIKE = 1;
    private static final double REGISTER = 0.8;

    private Map<Integer, Double> totalSum = new HashMap<>();
    private Map<Integer, Map<Integer, Double>> minWeightsSum = new HashMap<>();

    Optional<EventSimilarityAvro> updateState(UserActionAvro userAction) {
        log.info("UserAction state is {}", userAction);
        return Optional.empty();
    }
}