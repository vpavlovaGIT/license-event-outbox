package ru.example.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.example.dto.SoftwareLicenseDto;
import ru.example.entity.OutboxEvent;
import ru.example.entity.SoftwareLicenseEntity;
import ru.example.repository.OutboxEventRepository;
import ru.example.repository.SoftwareLicenseRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxEventRepository outboxRepo;
    private final SoftwareLicenseRepository licenseRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${outbox.retention-days:7}")
    private int retentionDays;

    @Scheduled(fixedDelayString = "5000")
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = outboxRepo.findAllByProcessedFalse();
        for (OutboxEvent ev : events) {
            try {
                SoftwareLicenseDto dto = mapper.readValue(ev.getPayload(), SoftwareLicenseDto.class);
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

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * *") // Запускается каждый день в 3:00 ночи
    @Transactional
    public void cleanupOutbox() {
        LocalDateTime before = LocalDateTime.now().minusDays(retentionDays);
        outboxRepo.deleteProcessedOlderThan(before);
    }
}
