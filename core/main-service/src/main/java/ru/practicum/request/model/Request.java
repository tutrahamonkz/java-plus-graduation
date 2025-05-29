package ru.practicum.request.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.event.model.Event;

import java.time.LocalDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "requests")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Column(name = "requester_id", nullable = false)
    private Long requester;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
}