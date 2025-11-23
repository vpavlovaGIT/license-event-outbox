package ru.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.example.entity.OutboxEvent;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findAllByProcessedFalse();

    @Transactional
    @Modifying
    @Query("delete from OutboxEvent e where e.processed = true and e.processedAt < :before")
    void deleteProcessedOlderThan(@Param("before") LocalDateTime before);
}
