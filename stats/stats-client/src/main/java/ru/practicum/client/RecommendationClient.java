package ru.practicum.client;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.grpc.stats.*;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class RecommendationClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;


    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResult) {
        UserPredictionsRequestProto proto = UserPredictionsRequestProto
                .newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResult)
                .build();
        Iterator<RecommendedEventProto> iterator = client.getRecommendationsForUser(proto);
        return asStream(iterator);
    }

    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        InteractionsCountRequestProto proto = InteractionsCountRequestProto
                .newBuilder()
                .addAllEventId(eventIds)
                .build();
        Iterator<RecommendedEventProto> iterator = client.getInteractionsCount(proto);
        return asStream(iterator);
    }

    public Stream<RecommendedEventProto> getSimilarEvents(long userId, long eventId, int maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto
                .newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setMaxResults(maxResults)
                .build();
        Iterator<RecommendedEventProto> iterator = client.getSimilarEvents(request);
        return asStream(iterator);
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}
