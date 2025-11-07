package ru.example.kafka;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.example.entity.OutboxEvent;
import ru.example.repository.OutboxEventRepository;

import java.time.LocalDateTime;

@Component
public class LicenseKafkaListener {

    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public LicenseKafkaListener(OutboxEventRepository outboxRepo) {
        this.outboxRepo = outboxRepo;
    }

    @KafkaListener(topics = "${app.kafka-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String message) {
        OutboxEvent ev = new OutboxEvent();
        ev.setPayload(message);
        ev.setProcessed(false);
        ev.setCreatedAt(LocalDateTime.now());
        outboxRepo.save(ev);
    }

}
