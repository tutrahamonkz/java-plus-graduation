package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.EventAction;
import ru.practicum.model.EventActionId;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<EventAction, Long> {
    Optional<EventAction> findById(EventActionId id);

    @Query(value = """
            select ea
            from EventAction ea
            left join UserAction ua on ua.eventId = ea.id.eventA and ua.userId = :userId
            where ua.userId is null and ea.id.eventA = :eventId
            order by ea.score desc
            """)
    List<EventAction> findSimilarEvents(@Param("userId") Long userId, @Param("eventId") Long eventId);
}