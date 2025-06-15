package ru.practicum.service;

import io.grpc.stub.StreamObserver;
import ru.practicum.grpc.stats.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.RecommendedEventProto;
import ru.practicum.grpc.stats.SimilarEventsRequestProto;
import ru.practicum.grpc.stats.UserPredictionsRequestProto;

public interface RecommendationService {

    void getRecommendationsForUser(UserPredictionsRequestProto request,
                                   StreamObserver<RecommendedEventProto> responseObserver);

    void getSimilarEvents(SimilarEventsRequestProto request,
                          StreamObserver<RecommendedEventProto> responseObserver);

    void getInteractionsCount(InteractionsCountRequestProto request,
                              StreamObserver<RecommendedEventProto> responseObserver);
}
