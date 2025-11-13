package ru.example.scheduler;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.example.dto.SoftwareLicenseDto;
import ru.example.entity.OutboxEvent;
import ru.example.entity.SoftwareLicenseEntity;
import ru.example.repository.OutboxEventRepository;
import ru.example.repository.SoftwareLicenseRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {
    private static final Logger log = LoggerFactory.getLogger(OutboxScheduler.class);

    private final OutboxEventRepository outboxRepo;
    private final SoftwareLicenseRepository licenseRepo;

    @Value("${outbox.retention-days:7}")
    private int retentionDays;

    private static final Pattern JSON_FIELD_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");

    @Scheduled(fixedDelayString = "5000")
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = outboxRepo.findAllByProcessedFalse();
        log.info("processOutbox started: {} events to process", events.size());
        for (OutboxEvent ev : events) {
            try {
                SoftwareLicenseDto dto = parseJsonManually(ev.getPayload());

                SoftwareLicenseEntity entity = licenseRepo.findByLicenseId(dto.getLicenseId())
                        .orElseGet(SoftwareLicenseEntity::new);
                entity.setLicenseId(dto.getLicenseId());
                entity.setSoftwareName(dto.getSoftwareName());
                entity.setOwner(dto.getOwner());
                entity.setExpiresAt(dto.getExpiresAt());
                licenseRepo.save(entity);
                ev.setProcessed(true);
                ev.setProcessedAt(LocalDateTime.now());
                outboxRepo.save(ev);
                log.info("Successfully processed event ID: {} for license: {}", ev.getId(), dto.getLicenseId());
            } catch (Exception ex) {
                log.error("Failed to process event ID: {} - Payload: {} - Error: {}", ev.getId(), ev.getPayload(), ex.getMessage(), ex);
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * *") // Запускается каждый день в 3:00 ночи
    private SoftwareLicenseDto parseJsonManually(String json) throws Exception {
        SoftwareLicenseDto dto = new SoftwareLicenseDto();

        Matcher matcher = JSON_FIELD_PATTERN.matcher(json);

        while (matcher.find()) {
            String field = matcher.group(1);
            String value = matcher.group(2);

            switch (field) {
                case "licenseId":
                    dto.setLicenseId(value);
                    break;
                case "softwareName":
                    dto.setSoftwareName(value);
                    break;
                case "owner":
                    dto.setOwner(value);
                    break;
                case "expiresAt":
                    // Ручная обработка даты: парсим строку в LocalDate
                    dto.setExpiresAt(LocalDate.parse(value));  // value = "2025-12-31"
                    break;
                default:
                    break;
            }
        }
        
        if (dto.getLicenseId() == null || dto.getExpiresAt() == null) {
            throw new IllegalArgumentException("Invalid JSON: Missing required fields in payload: " + json);
        }

        log.debug("Manual parse success: licenseId={}, expiresAt={}", dto.getLicenseId(), dto.getExpiresAt());
        return dto;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOutbox() {
        LocalDateTime before = LocalDateTime.now().minusDays(retentionDays);
        outboxRepo.deleteProcessedOlderThan(before);
    }
}