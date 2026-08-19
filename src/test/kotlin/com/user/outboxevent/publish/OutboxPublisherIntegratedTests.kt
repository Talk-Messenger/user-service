package com.user.outboxevent.publish

import com.user.UserApplication
import com.user.contact.domain.repository.ContactRepository
import com.user.contact.infrastructure.persistance.repository.ContactJpaRepository
import com.user.outboxevent.infrastructure.persistance.repository.OutboxEventJpaRepository
import com.user.outboxevent.infrastructure.schedulers.OutboxPublisher
import com.user.user.domain.repository.UserRepository
import com.user.user.infrastructure.persistance.repository.UserJpaRepository
import com.user.utlis.TestContainersConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Duration
import java.util.Properties
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("Публикация outbox poller'ом | Integrated")
@SpringBootTest(classes = [UserApplication::class])
@TestPropertySource(
    properties = [
        "spring.cache.type=NONE",
        // Отключаем фоновый @Scheduled-запуск, чтобы тест управлял poller'ом вручную и детерминированно
        "outbox.publisher.poll-interval-ms=600000",
    ]
)
class OutboxPublisherIntegratedTests @Autowired constructor(
    private val outboxPublisher: OutboxPublisher,
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository,
    private val userJpaRepository: UserJpaRepository,
    private val contactJpaRepository: ContactJpaRepository,
) {

    companion object {
        @Container @ServiceConnection @JvmStatic
        private val postgres: PostgreSQLContainer = TestContainersConfig.POSTGRES

        @Container @ServiceConnection @JvmStatic
        private val redis: GenericContainer<*> = TestContainersConfig.REDIS

        @Container @ServiceConnection @JvmStatic
        private val kafka: KafkaContainer = TestContainersConfig.KAFKA

        // KafkaProducerConfig сам читает spring.kafka.bootstrap-servers через @Value,
        // минуя KafkaConnectionDetails, поэтому @ServiceConnection его не переопределяет —
        // выставляем свойство вручную, чтобы продюсер приложения стучался в контейнер.
        @DynamicPropertySource
        @JvmStatic
        fun kafkaProperties(registry: DynamicPropertyRegistry) {
            if (!kafka.isRunning) kafka.start()
            registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
        }
    }

    private lateinit var consumer: KafkaConsumer<String, String>

    @BeforeEach
    fun beforeEach() {
        contactJpaRepository.deleteAll()
        userJpaRepository.deleteAll()
        outboxEventJpaRepository.deleteAll()

        val props = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, "test-${UUID.randomUUID()}")
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
        }
        consumer = KafkaConsumer(props)
        consumer.subscribe(listOf("contact.added"))
        consumer.poll(Duration.ofMillis(200))
    }

    @AfterEach
    fun afterEach() {
        consumer.close()
    }

    @Test
    @DisplayName("Poller публикует ожидающее событие в Kafka и помечает его processed")
    fun success_poller_publishes_pending_events() {
        val user = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")
        val contactUser = userRepository.save(username = "contact", avatarUrl = "avatar", bio = "bio")
        val contact = contactRepository.save(user, contactUser)

        outboxPublisher.publishPending()

        val records = pollRecords(expectedCount = 1)

        assertEquals(1, records.size)
        assertTrue(records.first().value().contains(contact.id.toString()))

        // Создание двух пользователей само по себе пишет по одному USER_CREATED-событию в outbox,
        // поэтому всего ожидаем 3 события (2 USER_CREATED + 1 CONTACT_ADDED), но все должны быть processed
        val events = outboxEventJpaRepository.findAll()
        assertEquals(3, events.size)
        assertTrue(events.all { it.processed })

        val contactEvent = events.first { it.aggregateId == contact.id.toString() }
        assertEquals("ContactAdded", contactEvent.eventType)
    }

    @Test
    @DisplayName("Повторный запуск poller'а не публикует уже обработанные события")
    fun success_rerun_is_idempotent() {
        val user = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")
        val contactUser = userRepository.save(username = "contact", avatarUrl = "avatar", bio = "bio")
        contactRepository.save(user, contactUser)

        outboxPublisher.publishPending()
        val firstBatch = pollRecords(expectedCount = 1)
        assertEquals(1, firstBatch.size)

        outboxPublisher.publishPending()
        val secondBatch = pollRecords(expectedCount = 0, timeoutSeconds = 3)

        assertEquals(0, secondBatch.size)
        // Создание двух пользователей само по себе пишет по одному USER_CREATED-событию,
        // поэтому всего 3 события (2 USER_CREATED + 1 CONTACT_ADDED) — все обработаны за один прогон
        assertEquals(3, outboxEventJpaRepository.count())
        assertTrue(outboxEventJpaRepository.findAll().all { it.processed })
    }

    private fun pollRecords(
        expectedCount: Int,
        timeoutSeconds: Long = 10,
    ): List<ConsumerRecord<String, String>> {
        val records = mutableListOf<ConsumerRecord<String, String>>()
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
        while (records.size < expectedCount && System.currentTimeMillis() < deadline) {
            val batch = consumer.poll(Duration.ofMillis(500))
            batch.forEach { records.add(it) }
        }
        return records
    }

}
