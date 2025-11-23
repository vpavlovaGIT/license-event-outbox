package ru.example;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import ru.example.entity.OutboxEvent;
import ru.example.repository.OutboxEventRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class LicenseKafkaIntegrationTest {

    private static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
                    .withDatabaseName("licenses")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    private OutboxEventRepository outboxRepo;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeAll
    static void startContainers() {
        POSTGRES.start();
        KAFKA.start();
    }

    @AfterAll
    static void stopContainers() {
        KAFKA.stop();
        POSTGRES.stop();
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "test-group");

        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("app.kafka-topic", () -> "software-licenses");
    }

    @Test
    void testKafkaMessageIsSavedToOutbox() throws Exception {

        // GIVEN
        String msg = """
                {"licenseId":"LIC-TST","softwareName":"TestSoft","owner":"Tester","expiresAt":"2030-01-01"}
                """;

        // WHEN: отправляем сообщение в Kafka
        kafkaTemplate.send("software-licenses", msg).get();

        // ждём до прихода сообщения
        Thread.sleep(2000);

        // THEN
        List<OutboxEvent> events = outboxRepo.findAll();

        assertThat(events).isNotEmpty();
        assertThat(events.get(0).getPayload()).contains("LIC-TST");
    }
}