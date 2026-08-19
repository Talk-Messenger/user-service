package com.user.user.update

import com.user.UserApplication
import com.user.exceptions.user.UserNotFoundException
import com.user.exceptions.user.UsernameAlreadyExistsException
import com.user.user.application.use_case.update.UpdateUserCommand
import com.user.user.application.use_case.update.UpdateUserUseCase
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
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID
import kotlin.test.assertEquals

@DisplayName("Обновление профиля | Integrated")
@SpringBootTest(classes = [UserApplication::class])
@TestPropertySource(properties = ["spring.cache.type=NONE"])
class UpdateUserIntegratedTests @Autowired constructor(
    private val useCase: UpdateUserUseCase,
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

        val command = UpdateUserCommand(
            userId = user.id,
            user = user,
            username = "updated",
            avatarUrl = "updated_avatar_url",
            bio = "updated_bio",
        )

        val result = useCase.execute(command)

        assertEquals(user.id, result.id)
        assertEquals("updated", result.username)
        assertEquals("updated_avatar_url", result.avatarUrl)
        assertEquals("updated_bio", result.bio)
    }

    @Test
    @DisplayName("Ник не меняется")
    fun success_same_username() {
        val user = repository.save(
            username = "test",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )

        val command = UpdateUserCommand(
            userId = user.id,
            user = user,
            username = "test",
            avatarUrl = "updated_avatar_url",
            bio = "updated_bio",
        )

        val result = useCase.execute(command)

        assertEquals("test", result.username)
        assertEquals("updated_avatar_url", result.avatarUrl)
    }

    @Test
    @DisplayName("Ник уже занят другим пользователем")
    fun failure_username_already_exists() {
        val user = repository.save(
            username = "test",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )
        repository.save(
            username = "taken",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )

        val command = UpdateUserCommand(
            userId = user.id,
            user = user,
            username = "taken",
            avatarUrl = user.avatarUrl,
            bio = user.bio,
        )

        assertThrows<UsernameAlreadyExistsException> { useCase.execute(command) }
    }

    @Test
    @DisplayName("Пользователь удалён до обновления")
    fun failure_user_not_found() {
        val user = repository.save(
            username = "test",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )
        jpaRepository.deleteAll()

        val command = UpdateUserCommand(
            userId = user.id,
            user = user,
            username = "test",
            avatarUrl = "new_avatar_url",
            bio = "new_bio",
        )

        assertThrows<UserNotFoundException> { useCase.execute(command) }
    }

    @Test
    @DisplayName("Слишком длинный ник роняет запрос на уровне БД")
    fun failure_username_exceeds_db_column_length() {
        val user = repository.save(
            username = "test",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )

        val command = UpdateUserCommand(
            userId = user.id,
            user = user,
            username = "a".repeat(21),
            avatarUrl = user.avatarUrl,
            bio = user.bio,
        )

        assertThrows<Exception> { useCase.execute(command) }
    }

}
