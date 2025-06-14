package ru.practicum.client;

import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.grpc.stats.ActionTypeProto;
import ru.practicum.grpc.stats.UserActionControllerGrpc;
import ru.practicum.grpc.stats.UserActionProto;

import java.time.Instant;

@Component
public class CollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void collectUserAction(Long userId, Long eventId, ActionTypeProto actionType) {
        Instant instant = Instant.now();
        client.collectUserAction(UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(instant.getEpochSecond())
                        .setNanos(instant.getNano())
                        .build())
                .build());
    }
}
