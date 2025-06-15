package ru.practicum.mapping;

import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.stats.RecommendedEventProto;
import ru.practicum.model.EventAction;
import ru.practicum.model.UserAction;
import ru.practicum.model.UserActionType;

import java.util.ArrayList;
import java.util.List;

public class UserMapper {

    public static UserAction map(UserActionAvro userActionAvro) {
        UserAction userAction = new UserAction();
        userAction.setUserId(userActionAvro.getUserId());
        userAction.setEventId(userActionAvro.getEventId());
        userAction.setActionType(UserActionType.valueOf(userActionAvro.getActionType().name()));
        userAction.setTimestamp(userActionAvro.getTimestamp());
        return userAction;
    }

    public static List<RecommendedEventProto> mapEventActionToRecommendedEventProto(List<EventAction> events) {
        List<RecommendedEventProto> result = new ArrayList<>();
        for (EventAction event : events) {
            result.add(RecommendedEventProto.newBuilder()
                    .setEventId(event.getId().getEventB())
                    .setScore(event.getScore())
                    .build());
        }
        return result;
    }
}
