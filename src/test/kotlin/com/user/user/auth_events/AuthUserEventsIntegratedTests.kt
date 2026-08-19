package com.user.user.auth_events

import com.user.UserApplication
import com.user.contact.domain.repository.ContactRepository
import com.user.contact.infrastructure.persistance.repository.ContactJpaRepository
import com.user.outboxevent.infrastructure.persistance.entity.OutboxEventJpaEntity
import com.user.outboxevent.infrastructure.persistance.repository.OutboxEventJpaRepository
import com.user.user.domain.model.User
import com.user.user.domain.repository.UserRepository
import com.user.user.infrastructure.persistance.repository.UserJpaRepository
import com.user.utlis.TestContainersConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.AfterAll
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
import java.time.Instant
import java.util.Properties
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val AUTH_USER_CREATED_TOPIC = "test.auth.user.created"
private const val AUTH_USER_DELETED_TOPIC = "test.auth.user.deleted"
private const val AWAIT_TIMEOUT_MS = 20_000L
private const val SETTLE_MS = 3_000L

@DisplayName("Подписка на события AuthService | Integrated")
@SpringBootTest(classes = [UserApplication::class])
@TestPropertySource(
    properties = [
        "spring.cache.type=NONE",
        // Publisher отключаем: опубликованные события проверяем прямо в таблице outbox_events
        "outbox.publisher.poll-interval-ms=600000",
        "kafka.topics.auth-user-created=$AUTH_USER_CREATED_TOPIC",
        "kafka.topics.auth-user-deleted=$AUTH_USER_DELETED_TOPIC",
    ]
)
class AuthUserEventsIntegratedTests @Autowired constructor(
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository,
    private val userJpaRepository: UserJpaRepository,
    private val contactJpaRepository: ContactJpaRepository,
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
) {

    companion object {
        @Container @ServiceConnection @JvmStatic
        private val postgres: PostgreSQLContainer = TestContainersConfig.POSTGRES

        @Container @ServiceConnection @JvmStatic
        private val redis: GenericContainer<*> = TestContainersConfig.REDIS

        @Container @ServiceConnection @JvmStatic
        private val kafka: KafkaContainer = TestContainersConfig.KAFKA

        // KafkaProducerConfig/KafkaConsumerConfig читают spring.kafka.bootstrap-servers через @Value,
        // минуя KafkaConnectionDetails, поэтому свойство выставляем вручную.
        @DynamicPropertySource
        @JvmStatic
        fun kafkaProperties(registry: DynamicPropertyRegistry) {
            if (!kafka.isRunning) kafka.start()
            registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
        }

        private var producerRef: KafkaProducer<String, String>? = null

        private fun producer(): KafkaProducer<String, String> {
            producerRef?.let { return it }
            val props = Properties().apply {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            }
            return KafkaProducer<String, String>(props).also { producerRef = it }
        }

        @AfterAll
        @JvmStatic
        fun closeProducer() {
            producerRef?.close()
            producerRef = null
        }
    }

    @BeforeEach
    fun beforeEach() {
        contactJpaRepository.deleteAll()
        userJpaRepository.deleteAll()
        outboxEventJpaRepository.deleteAll()
    }

    @Test
    @DisplayName("AuthUserCreated создаёт профиль и пишет событие UserCreated в outbox")
    fun success_auth_user_created_creates_profile_and_publishes_event() {
        val userId = UUID.randomUUID()

        send(
            AUTH_USER_CREATED_TOPIC, userId,
            """{"userId":"$userId","username":"alice","createdAt":"2026-01-01T10:00:00Z"}"""
        )

        val entity = await("профиль создан") { userJpaRepository.findByIdAndDeletedAtIsNull(userId) }
        assertEquals("alice", entity.username)
        assertNull(entity.deletedAt)

        val event = await("UserCreated в outbox") { outboxEvent("UserCreated", userId) }
        assertTrue(event.payload.contains(userId.toString()))
        assertTrue(event.payload.contains("alice"))
    }

    @Test
    @DisplayName("Повторная доставка AuthUserCreated не создаёт дубликат и не падает")
    fun success_repeated_auth_user_created_is_idempotent() {
        val userId = UUID.randomUUID()
        val payload = """{"userId":"$userId","username":"bob","createdAt":"2026-01-01T10:00:00Z"}"""

        send(AUTH_USER_CREATED_TOPIC, userId, payload)
        await("профиль создан") { userJpaRepository.findByIdAndDeletedAtIsNull(userId) }
        await("UserCreated в outbox") { outboxEvent("UserCreated", userId) }

        // Повтор того же события: конфликт по PK гасится без ошибки и без ретрая
        send(AUTH_USER_CREATED_TOPIC, userId, payload)
        send(AUTH_USER_CREATED_TOPIC, userId, payload)
        Thread.sleep(SETTLE_MS)

        assertEquals(1, userJpaRepository.count())
        assertEquals(1, countOutboxEvents("UserCreated", userId))
    }

    @Test
    @DisplayName("AuthUserDeleted делает soft delete, каскадно удаляет контакты и пишет UserDeleted")
    fun success_auth_user_deleted_soft_deletes_and_cascades() {
        val target = createUser("victim")
        val friend = createUser("friend")
        val stranger = createUser("stranger")

        // связи в обе стороны: где пользователь владелец и где он значится чужим контактом
        contactRepository.save(target, friend)
        contactRepository.save(stranger, target)
        assertEquals(2, contactJpaRepository.count())

        send(
            AUTH_USER_DELETED_TOPIC, target.id,
            """{"userId":"${target.id}","deletedAt":"2026-02-02T12:00:00Z"}"""
        )

        val deleted = await("профиль помечен удалённым") {
            userJpaRepository.findById(target.id).orElse(null)?.takeIf { it.deletedAt != null }
        }
        assertEquals(Instant.parse("2026-02-02T12:00:00Z"), deleted.deletedAt)

        await("контакты удалены") { contactJpaRepository.count().takeIf { it == 0L } }

        // остальные пользователи не затронуты
        assertEquals(3, userJpaRepository.count())
        assertNotNull(userJpaRepository.findByIdAndDeletedAtIsNull(friend.id))

        val event = await("UserDeleted в outbox") { outboxEvent("UserDeleted", target.id) }
        assertTrue(event.payload.contains(target.id.toString()))
    }

    @Test
    @DisplayName("Повторная доставка AuthUserDeleted идемпотентна")
    fun success_repeated_auth_user_deleted_is_idempotent() {
        val target = createUser("victim")
        val friend = createUser("friend")
        contactRepository.save(target, friend)

        val payload = """{"userId":"${target.id}","deletedAt":"2026-02-02T12:00:00Z"}"""

        send(AUTH_USER_DELETED_TOPIC, target.id, payload)
        val deleted = await("профиль помечен удалённым") {
            userJpaRepository.findById(target.id).orElse(null)?.takeIf { it.deletedAt != null }
        }
        val firstDeletedAt = deleted.deletedAt

        send(AUTH_USER_DELETED_TOPIC, target.id, payload)
        send(AUTH_USER_DELETED_TOPIC, target.id, payload)
        Thread.sleep(SETTLE_MS)

        // deleted_at не переписывается, второе событие UserDeleted не публикуется
        assertEquals(
            firstDeletedAt,
            userJpaRepository.findById(target.id).orElseThrow().deletedAt
        )
        assertEquals(1, countOutboxEvents("UserDeleted", target.id))
        assertEquals(0, contactJpaRepository.count())
    }

    @Test
    @DisplayName("AuthUserDeleted для неизвестного пользователя не ломает консьюмер")
    fun success_auth_user_deleted_for_unknown_user_is_noop() {
        val unknownId = UUID.randomUUID()
        send(AUTH_USER_DELETED_TOPIC, unknownId, """{"userId":"$unknownId"}""")
        Thread.sleep(SETTLE_MS)

        assertEquals(0, userJpaRepository.count())
        assertEquals(0, countOutboxEvents("UserDeleted", unknownId))

        // Консьюмер продолжает работать: следующее корректное событие обрабатывается
        val userId = UUID.randomUUID()
        send(AUTH_USER_CREATED_TOPIC, userId, """{"userId":"$userId","username":"after"}""")
        await("профиль создан после no-op события") { userJpaRepository.findByIdAndDeletedAtIsNull(userId) }
    }

    @Test
    @DisplayName("Невалидное событие уходит в dead-letter топик и не блокирует обработку")
    fun failure_invalid_event_goes_to_dlt() {
        val dltConsumer = dltConsumer("$AUTH_USER_CREATED_TOPIC.DLT")
        try {
            send(AUTH_USER_CREATED_TOPIC, null, "{ это не json")

            val records = mutableListOf<String>()
            val deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS
            while (records.isEmpty() && System.currentTimeMillis() < deadline) {
                dltConsumer.poll(Duration.ofMillis(500)).forEach { records.add(it.value()) }
            }

            assertEquals(1, records.size, "невалидное событие должно попасть в DLT")
            assertTrue(records.first().contains("это не json"))

            // Следующее валидное событие обрабатывается как обычно
            val userId = UUID.randomUUID()
            send(AUTH_USER_CREATED_TOPIC, userId, """{"userId":"$userId","username":"valid"}""")
            await("профиль создан после невалидного события") {
                userJpaRepository.findByIdAndDeletedAtIsNull(userId)
            }
        } finally {
            dltConsumer.close()
        }
    }

    @Test
    @DisplayName("Событие в конверте outbox-формата (payload внутри) тоже обрабатывается")
    fun success_envelope_shaped_event_is_supported() {
        val userId = UUID.randomUUID()
        send(
            AUTH_USER_CREATED_TOPIC, userId,
            """
            {
              "id":"${UUID.randomUUID()}",
              "eventType":"AuthUserCreated",
              "aggregateId":"$userId",
              "payload":{"userId":"$userId","username":"enveloped"},
              "createdAt":"2026-01-01T10:00:00Z"
            }
            """.trimIndent()
        )

        val entity = await("профиль создан из конверта") { userJpaRepository.findByIdAndDeletedAtIsNull(userId) }
        assertEquals("enveloped", entity.username)
    }

    // --- helpers -------------------------------------------------------------

    private fun createUser(username: String): User =
        userRepository.createWithId(
            id = UUID.randomUUID(),
            username = username,
            avatarUrl = "",
            bio = "",
            createdAt = Instant.now(),
        )!!

    private fun send(topic: String, key: UUID?, value: String) {
        producer().send(ProducerRecord(topic, key?.toString(), value)).get()
    }

    private fun outboxEvent(eventType: String, aggregateId: UUID): OutboxEventJpaEntity? =
        outboxEventJpaRepository.findAll()
            .firstOrNull { it.eventType == eventType && it.aggregateId == aggregateId.toString() }

    private fun countOutboxEvents(eventType: String, aggregateId: UUID): Int =
        outboxEventJpaRepository.findAll()
            .count { it.eventType == eventType && it.aggregateId == aggregateId.toString() }

    /** Ждём, пока асинхронный консьюмер доведёт состояние до ожидаемого. */
    private fun <T : Any> await(what: String, supplier: () -> T?): T {
        val deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            supplier()?.let { return it }
            Thread.sleep(200)
        }
        throw AssertionError("Не дождались: $what")
    }

    private fun dltConsumer(topic: String): KafkaConsumer<String, String> {
        val props = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-test-${UUID.randomUUID()}")
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
        }
        val consumer = KafkaConsumer<String, String>(props)
        consumer.subscribe(listOf(topic))
        consumer.poll(Duration.ofMillis(200))
        return consumer
    }

}
