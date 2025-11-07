package ru.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.example.dto.SoftwareLicenseDto;
import ru.example.entity.OutboxEvent;
import ru.example.repository.OutboxEventRepository;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/licenses")
@RequiredArgsConstructor
public class LicenseController {

    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    // REST endpoint принимает JSON и кладёт его в outbox
    @PostMapping
    public ResponseEntity<?> receive(@RequestBody SoftwareLicenseDto dto) {
        try {
            String payload = mapper.writeValueAsString(dto);
            OutboxEvent ev = new OutboxEvent();
            ev.setPayload(payload);
            ev.setProcessed(false);
            ev.setCreatedAt(LocalDateTime.now());
            outboxRepo.save(ev);
            return ResponseEntity.accepted().build();
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

}
