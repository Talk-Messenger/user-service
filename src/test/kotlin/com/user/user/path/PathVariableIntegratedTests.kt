package com.user.user.path

import com.user.UserApplication
import com.user.exceptions.user.UserNotFoundException
import com.user.user.application.use_case.public_profile.PublicProfileCommand
import com.user.user.application.use_case.public_profile.PublicProfileUseCase
import com.user.user.domain.repository.UserRepository
import com.user.user.infrastructure.persistance.entity.UserJpaEntity
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
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

@DisplayName("Поиск по нику | Integrated")
@SpringBootTest(classes = [UserApplication::class])
@TestPropertySource(properties = ["spring.cache.type=NONE"])
class PathVariableIntegratedTests @Autowired constructor (
    private val publicProfileUseCase: PublicProfileUseCase,
    private val repository: UserRepository,
    private val jpaRepository: UserJpaRepository,
) {

    companion object {
        @Container @ServiceConnection @JvmStatic
        private val postgres: PostgreSQLContainer = TestContainersConfig.POSTGRES

        @Container @ServiceConnection @JvmStatic
        private val redis: GenericContainer<*> = TestContainersConfig.REDIS
    }

    @BeforeEach
    fun beforeEach() {
        jpaRepository.deleteAll()
    }

    @Test
    @DisplayName("Успех")
    fun success() {
        val user = repository.save(
            username = "test",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )

        val command = PublicProfileCommand(
            username = "test",
        )

        val result = publicProfileUseCase.execute(command)

        assertEquals(user.id, result.id)
        assertEquals(user.username, result.username)
        assertEquals(user.avatarUrl, result.avatarUrl)
        assertEquals(user.bio, result.bio)
        assertEquals(user.createdAt, result.createdAt)
        assertEquals(user.updatedAt, result.updatedAt)
        assertEquals(user.deletedAt, result.deletedAt)
    }

    @Test
    @DisplayName("Пользователь не найден")
    fun failure_user_not_found() {
        val command = PublicProfileCommand(
            username = "test2",
        )

        assertThrows<UserNotFoundException> { publicProfileUseCase.execute(command) }
    }

    @Test
    @DisplayName("Пользователь удалён")
    fun failure_user_deleted() {
        jpaRepository.save(
            UserJpaEntity(
                username = "test",
                avatarUrl = "some_avatar_url",
                bio = "some_bio",
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                deletedAt = Instant.now()
            )
        )

        val command = PublicProfileCommand(
            username = "test",
        )

        assertThrows<UserNotFoundException> { publicProfileUseCase.execute(command) }
    }

}