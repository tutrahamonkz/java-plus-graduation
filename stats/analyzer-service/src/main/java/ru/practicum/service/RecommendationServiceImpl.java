package ru.practicum.service;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.grpc.stats.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.RecommendedEventProto;
import ru.practicum.grpc.stats.SimilarEventsRequestProto;
import ru.practicum.grpc.stats.UserPredictionsRequestProto;
import ru.practicum.mapping.UserMapper;
import ru.practicum.model.EventAction;
import ru.practicum.model.UserAction;
import ru.practicum.model.UserActionType;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final double VIEW = 0.4;
    private static final double REGISTER = 0.8;
    private static final double LIKE = 1.0;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {

        long userId = request.getUserId();
        int limit = request.getMaxResults();

        log.info("Started getRecommendationsForUser for userId={}, limit={}", userId, limit);

        List<UserAction> userActions = userRepository.getRecentUserInteraction(userId, limit);

        if (userActions.isEmpty()) {
            log.info("No recent user actions for userId={}, completing response", userId);
            responseObserver.onCompleted();
            return;
        }

        Set<Long> userEventIds = extractUserEventIds(userActions);
        List<EventAction> similarEvents = getSimilarEventsForUserActions(userId, userActions);

        List<EventAction> candidateEvents = filterOutUserEvents(similarEvents, userEventIds);

        if (candidateEvents.isEmpty()) {
            log.info("No candidate events found for userId={}, completing response", userId);
            responseObserver.onCompleted();
            return;
        }

        candidateEvents.stream()
                .limit(limit)
                .forEach(eventAction -> {
                    RecommendedEventProto recommendedEvent = RecommendedEventProto.newBuilder()
                            .setEventId(eventAction.getId().getEventB())
                            .setScore(eventAction.getScore())
                            .build();
                    responseObserver.onNext(recommendedEvent);
                });

        responseObserver.onCompleted();

        log.info("Completed getRecommendationsForUser for userId={}", userId);
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {

        long userId = request.getUserId();
        long eventId = request.getEventId();

        log.info("Started getSimilarEvents for userId={}, eventId={}", userId, eventId);

        List<EventAction> events = eventRepository.findSimilarEvents(userId, eventId);

        List<RecommendedEventProto> recommendedEvents = UserMapper.mapEventActionToRecommendedEventProto(events);

        recommendedEvents.forEach(responseObserver::onNext);

        responseObserver.onCompleted();

        log.info("Completed getSimilarEvents for userId={}, eventId={}", userId, eventId);
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {

        List<Long> requestEvents = request.getEventIdList();

        log.info("Started getInteractionsCount for events={}", requestEvents);

        List<UserAction> userActions = userRepository.getByEventIds(requestEvents);

        if (userActions.isEmpty()) {
            log.info("No user actions found for events={}, completing response", requestEvents);
            responseObserver.onCompleted();
            return;
        }

        Map<Long, Map<Long, Double>> maxWeights = calculateMaxWeights(userActions);

        List<RecommendedEventProto> result = buildRecommendedEventProtos(maxWeights);

        result.forEach(responseObserver::onNext);

        responseObserver.onCompleted();

        log.info("Completed getInteractionsCount for events={}", requestEvents);
    }

    private Set<Long> extractUserEventIds(List<UserAction> userActions) {
        return userActions.stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());
    }

    private List<EventAction> getSimilarEventsForUserActions(long userId, List<UserAction> userActions) {
        List<EventAction> similarEvents = new ArrayList<>();
        for (UserAction userAction : userActions) {
            similarEvents.addAll(eventRepository.findSimilarEvents(userId, userAction.getEventId()));
        }
        return similarEvents;
    }

    private List<EventAction> filterOutUserEvents(List<EventAction> events, Set<Long> userEventIds) {
        return events.stream()
                .filter(eventAction -> !userEventIds.contains(eventAction.getId().getEventB()))
                .toList();
    }

    private Map<Long, Map<Long, Double>> calculateMaxWeights(List<UserAction> userActions) {
        Map<Long, Map<Long, Double>> maxWeights = new HashMap<>();

        for (UserAction ua : userActions) {
            long eventId = ua.getEventId();
            long userId = ua.getUserId();
            double weight = toWeight(ua.getActionType());

            Map<Long, Double> userMap = maxWeights.computeIfAbsent(eventId, e -> new HashMap<>());
            userMap.merge(userId, weight, Double::max);
        }
        return maxWeights;
    }

    private List<RecommendedEventProto> buildRecommendedEventProtos(Map<Long, Map<Long, Double>> maxWeights) {
        List<RecommendedEventProto> result = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : maxWeights.entrySet()) {
            long eventId = entry.getKey();
            double scoreSum = entry.getValue().values().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();

            RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                    .setEventId(eventId)
                    .setScore(scoreSum)
                    .build();

            result.add(proto);
        }
        return result;
    }

    private double toWeight(UserActionType type) {
        return switch (type) {
            case VIEW -> VIEW;
            case REGISTER -> REGISTER;
            case LIKE -> LIKE;
        };
    }
}