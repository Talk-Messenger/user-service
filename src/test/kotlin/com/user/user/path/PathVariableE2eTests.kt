package com.user.user.path

import com.fasterxml.jackson.databind.ObjectMapper
import com.user.UserApplication
import com.user.user.domain.repository.UserRepository
import com.user.user.infrastructure.persistance.entity.UserJpaEntity
import com.user.user.infrastructure.persistance.repository.UserJpaRepository
import com.user.utlis.MapperConfiguration
import com.user.utlis.TestContainersConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Instant

@Import(MapperConfiguration::class)
@DisplayName("Поиск по нику | E2e")
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@SpringBootTest(classes = [UserApplication::class])
class PathVariableE2eTests @Autowired constructor(

    private val mvc: MockMvc,
    private val mapper: ObjectMapper,
    private val repository: UserRepository,
    private val jpaRepository: UserJpaRepository,

) {

    private final val path = "/api/v1/users"

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

        mvc.perform(get("$path/test"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(user.id.toString()))
            .andExpect(jsonPath("$.avatarUrl").value(user.avatarUrl))
            .andExpect(jsonPath("$.bio").value(user.bio))
            .andExpect(jsonPath("$.createdAt").exists())
    }

    @Test
    @DisplayName("Пользователь не найден")
    fun failure_user_not_found() {
        mvc.perform(get("$path/unknown"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Пользователь не найден"))
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
                deletedAt = Instant.now(),
            )
        )

        mvc.perform(get("$path/test"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Пользователь не найден"))
    }

    @Test
    @DisplayName("Регистр ника учитывается")
    fun failure_case_sensitive() {
        repository.save(
            username = "test",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )

        mvc.perform(get("$path/TEST"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Пользователь не найден"))
    }

    @Test
    @DisplayName("Пустой ник в пути невалиден")
    fun failure_blank_username() {
        mvc.perform(get("$path/"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("Длинный ник не приводит к ошибке сервера")
    fun failure_long_username_does_not_500() {
        mvc.perform(get("$path/${"a".repeat(256)}"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Пользователь не найден"))
    }

    @Test
    @DisplayName("Небезопасные символы в нике отклоняются файрволом")
    fun failure_unsafe_characters_rejected() {
        mvc.perform(get("$path/test%20user"))
            .andExpect(status().isBadRequest)
    }

}
