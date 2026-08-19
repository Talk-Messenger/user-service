package com.user.outboxevent.write

import com.user.UserApplication
import com.user.outboxevent.infrastructure.persistance.repository.OutboxEventJpaRepository
import com.user.user.domain.repository.UserRepository
import com.user.user.infrastructure.persistance.repository.UserJpaRepository
import com.user.utlis.TestContainersConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.postgresql.PostgreSQLContainer
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("Запись в outbox | Integrated")
@SpringBootTest(classes = [UserApplication::class])
@TestPropertySource(
    properties = [
        "spring.cache.type=NONE",
        "outbox.publisher.poll-interval-ms=600000",
    ]
)
class OutboxEventWriterIntegratedTests @Autowired constructor(
    private val userRepository: UserRepository,
    private val userJpaRepository: UserJpaRepository,
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
    private val transactionManager: PlatformTransactionManager,
) {

    companion object {
        @Container @ServiceConnection @JvmStatic
        private val postgres: PostgreSQLContainer = TestContainersConfig.POSTGRES

        @Container @ServiceConnection @JvmStatic
        private val redis: GenericContainer<*> = TestContainersConfig.REDIS
    }

    @BeforeEach
    fun beforeEach() {
        outboxEventJpaRepository.deleteAll()
        userJpaRepository.deleteAll()
    }

    @Test
    @DisplayName("Успех — событие пишется в outbox вместе с бизнес-данными")
    fun success_writes_event_alongside_business_data() {
        val user = userRepository.save(username = "test", avatarUrl = "avatar", bio = "bio")

        val events = outboxEventJpaRepository.findAll()

        assertEquals(1, events.size)
        val event = events.first()
        assertEquals(user.id.toString(), event.aggregateId)
        assertEquals("UserCreated", event.eventType)
        assertEquals(false, event.processed)
        assertTrue(event.payload.contains(user.id.toString()))
        assertTrue(event.payload.contains(user.username))
    }

    @Test
    @DisplayName("Откат транзакции откатывает и запись в outbox")
    fun failure_rollback_also_rolls_back_outbox_write() {
        val template = TransactionTemplate(transactionManager)

        assertThrows<RuntimeException> {
            template.execute<Unit> {
                userRepository.save(username = "test", avatarUrl = "avatar", bio = "bio")
                throw RuntimeException("boom")
            }
        }

        assertEquals(0, userJpaRepository.count())
        assertEquals(0, outboxEventJpaRepository.count())
    }

}
