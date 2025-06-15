package ru.practicum.kafka;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordHandler {
    private static final double VIEW = 0.4;
    private static final double LIKE = 1;
    private static final double REGISTER = 0.8;

    private final Map<Long, Map<Long, Double>> maxUserAction = new HashMap<>();
    private final Map<Long, Double> totalSum = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new HashMap<>();
    private final Map<Long, Set<Long>> userActions = new HashMap<>();

    /**
     * Основной метод обновления состояния с учётом нового действия пользователя.
     * Делегирует добавление новых данных и обновление весов в отдельный метод.
     */
    public List<EventSimilarityAvro> updateState(UserActionAvro userAction) {
        long userId = userAction.getUserId();
        long eventId = userAction.getEventId();
        double newWeight = getActionTypeScore(userAction.getActionType());

        log.debug("Updating state for userId={} eventId={} with action weight={}", userId, eventId, newWeight);

        Map<Long, Double> userMap = maxUserAction.get(eventId);
        if (userMap == null) {
            // Новое событие — создаём структуру и добавляем пользователя
            log.debug("New eventId={} detected, initializing structures", eventId);
            userMap = new HashMap<>();
            maxUserAction.put(eventId, userMap);

            totalSum.put(eventId, 0.0);
        }

        Double oldWeight = userMap.get(userId);
        if (oldWeight == null) {
            // Новый пользователь для события
            addUserAction(userId, eventId, 0.0, newWeight);
        } else if (oldWeight < newWeight) {
            // Обновление веса пользователя, если новый вес больше
            addUserAction(userId, eventId, oldWeight, newWeight);
        } else {
            log.debug("Existing weight {} for userId={} eventId={} is >= new weight {}, skipping update",
                    oldWeight, userId, eventId, newWeight);
            return List.of();
        }

        // Формируем список похожих событий для пользователя с пересчитанными коэффициентами
        return calculateSimilarities(userId, eventId);
    }

    /**
     * Добавление/обновление действия пользователя
     * с обновлением всех связанных структур.
     */
    private void addUserAction(long userId, long eventId, double oldWeight, double newWeight) {
        log.debug("Adding/updating user action: userId={}, eventId={}, oldWeight={}, newWeight={}",
                userId, eventId, oldWeight, newWeight);

        maxUserAction.get(eventId).put(userId, newWeight);

        double oldTotal = totalSum.getOrDefault(eventId, 0.0);
        totalSum.put(eventId, oldTotal - oldWeight + newWeight);

        userActions.computeIfAbsent(userId, k -> new HashSet<>()).add(eventId);

        updateMinWeightsSumsForUser(userId, eventId, oldWeight, newWeight);
    }

    /**
     * Обновляет сумму минимальных весов для пары событий с учётом изменения веса пользователя.
     *
     * @param userId    идентификатор пользователя
     * @param eventId   событие, в котором обновился вес
     * @param oldWeight старый вес пользователя для события eventId
     * @param newWeight новый вес пользователя для события eventId
     */
    private void updateMinWeightsSumsForUser(long userId, long eventId, double oldWeight, double newWeight) {
        Set<Long> events = userActions.get(userId);
        if (events == null) {
            log.warn("User {} has no events during updateMinWeightsSumsForUser", userId);
            return;
        }

        for (Long otherEventId : events) {
            if (otherEventId == eventId) continue;

            Map<Long, Double> otherEventUsers = maxUserAction.get(otherEventId);
            if (otherEventUsers == null) {
                log.warn("No users found for eventId {} during updateMinWeightsSumsForUser", otherEventId);
                continue;
            }

            double oldMin = otherEventUsers.containsKey(userId)
                    ? Math.min(oldWeight, otherEventUsers.get(userId))
                    : 0.0;

            double newMin = otherEventUsers.containsKey(userId)
                    ? Math.min(newWeight, otherEventUsers.get(userId))
                    : 0.0;

            double currentSum = get(eventId, otherEventId);
            double updatedSum = currentSum - oldMin + newMin;

            put(eventId, otherEventId, updatedSum);

            log.debug("Updated minWeightsSums for events ({}, {}): {} -> {}", eventId, otherEventId, currentSum, updatedSum);
        }
    }

    private Double getActionTypeScore(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> VIEW;
            case LIKE -> LIKE;
            case REGISTER -> REGISTER;
        };
    }

    private List<EventSimilarityAvro> calculateSimilarities(long userId, long eventId) {
        List<EventSimilarityAvro> events = new ArrayList<>();
        Set<Long> userEvents = userActions.get(userId);
        if (userEvents == null) {
            log.warn("User {} has no events during calculateSimilarities", userId);
            return List.of();
        }

        for (Long otherEvent : userEvents) {
            if (otherEvent == eventId) continue;

            double numerator = get(eventId, otherEvent);
            double denominator = Math.sqrt(totalSum.get(eventId)) * Math.sqrt(totalSum.get(otherEvent));

            if (denominator == 0) {
                log.warn("Denominator zero for events ({}, {}), skipping similarity", eventId, otherEvent);
                continue;
            }

            double score = numerator / denominator;

            long eventA = Math.min(eventId, otherEvent);
            long eventB = Math.max(eventId, otherEvent);

            events.add(EventSimilarityAvro.newBuilder()
                    .setEventA(eventA)
                    .setEventB(eventB)
                    .setScore(score)
                    .setTimestamp(Instant.now())
                    .build());
        }

        events.sort(Comparator
                .comparing(EventSimilarityAvro::getEventA)
                .thenComparing(EventSimilarityAvro::getEventB));

        return events;
    }

    private void put(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        minWeightsSums
                .computeIfAbsent(first, e -> new HashMap<>())
                .put(second, sum);
    }

    private double get(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        return minWeightsSums
                .computeIfAbsent(first, e -> new HashMap<>())
                .getOrDefault(second, 0.0);
    }
}