package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.UserAction;

import java.util.List;

public interface UserRepository extends JpaRepository<UserAction, Long> {

    @Query(value = """
            select *
            from user_actions
            where user_id = :userId
            order by created desc
            limit :limit
            """, nativeQuery = true)
    List<UserAction> getRecentUserInteraction(@Param("userId") Long userId, @Param("limit") int limit);

    @Query("select ua from UserAction ua where ua.eventId in :eventIds")
    List<UserAction> getByEventIds(@Param("eventIds") List<Long> eventIds);
}