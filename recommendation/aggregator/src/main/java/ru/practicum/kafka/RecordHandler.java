package ru.practicum.kafka;

public class RecordHandler {
    /*private static final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {

        SensorsSnapshotAvro snapshot;
        if(snapshots.containsKey(event.getHubId())) {
            snapshot = snapshots.get(event.getHubId());
        } else {
            snapshot = SensorsSnapshotAvro.newBuilder()
                    .setHubId(event.getHubId())
                    .setTimestamp(event.getTimestamp())
                    .setSensorsState(new HashMap<>())
                    .build();
            snapshots.put(event.getHubId(), snapshot);
        }

        Map<String, SensorStateAvro> states = snapshot.getSensorsState();
        if(!states.isEmpty() && states.containsKey(event.getId())) {
            SensorStateAvro oldState = states.get(event.getId());
            if(oldState.getTimestamp().isAfter(event.getTimestamp()) ||
                    oldState.getData().equals(event.getPayload())) {
                return Optional.empty();
            }
        }

        SensorStateAvro state = SensorStateAvro.newBuilder()
                .setData(event.getPayload())
                .setTimestamp(event.getTimestamp())
                .build();
        snapshot.getSensorsState().put(event.getId(), state);
        snapshot.setTimestamp(event.getTimestamp());
        return Optional.of(snapshot);
    }*/
}
