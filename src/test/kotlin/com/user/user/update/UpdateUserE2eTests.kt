package com.user.user.update

import com.fasterxml.jackson.databind.ObjectMapper
import com.user.UserApplication
import com.user.user.domain.repository.UserRepository
import com.user.user.infrastructure.persistance.repository.UserJpaRepository
import com.user.user.infrastructure.web.dto.request.UpdateUserProfileRequest
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
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID

@Import(MapperConfiguration::class)
@DisplayName("Обновление профиля | E2e")
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@SpringBootTest(classes = [UserApplication::class])
class UpdateUserE2eTests @Autowired constructor(

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
        val account = repository.save(
            username = "test",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )

        val request = UpdateUserProfileRequest(
            username = "updated",
            avatarUrl = "updated_avatar_url",
            bio = "updated_bio",
        )

        mvc.perform(
            put("$path/${account.id}")
                .with(user(account))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(account.id.toString()))
            .andExpect(jsonPath("$.username").value("updated"))
            .andExpect(jsonPath("$.avatarUrl").value("updated_avatar_url"))
            .andExpect(jsonPath("$.bio").value("updated_bio"))
    }

    @Test
    @DisplayName("Без токена запрос не проходит")
    fun failure_unauthenticated() {
        val request = UpdateUserProfileRequest(
            username = "updated",
            avatarUrl = "updated_avatar_url",
            bio = "updated_bio",
        )

        mvc.perform(
            put("$path/${UUID.randomUUID()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Вы можете обновить только данные о себе"))
    }

    @Test
    @DisplayName("Нельзя обновить чужой профиль")
    fun failure_cannot_update_another_user() {
        val account = repository.save(
            username = "test",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )
        val another = repository.save(
            username = "another",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )

        val request = UpdateUserProfileRequest(
            username = "hacked",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )

        mvc.perform(
            put("$path/${another.id}")
                .with(user(account))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Вы можете обновить только данные о себе"))
    }

    @Test
    @DisplayName("Ник уже занят другим пользователем")
    fun failure_username_already_exists() {
        val account = repository.save(
            username = "test",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )
        repository.save(
            username = "taken",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )

        val request = UpdateUserProfileRequest(
            username = "taken",
            avatarUrl = account.avatarUrl,
            bio = account.bio,
        )

        mvc.perform(
            put("$path/${account.id}")
                .with(user(account))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Имя пользователя занято"))
    }

    @Test
    @DisplayName("Пустой ник отклоняется валидацией")
    fun failure_blank_username_rejected() {
        val account = repository.save(
            username = "test",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )

        val request = UpdateUserProfileRequest(
            username = "",
            avatarUrl = account.avatarUrl,
            bio = account.bio,
        )

        mvc.perform(
            put("$path/${account.id}")
                .with(user(account))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Слишком длинный ник отклоняется валидацией")
    fun failure_too_long_username_rejected() {
        val account = repository.save(
            username = "test",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )

        val request = UpdateUserProfileRequest(
            username = "a".repeat(21),
            avatarUrl = account.avatarUrl,
            bio = account.bio,
        )

        mvc.perform(
            put("$path/${account.id}")
                .with(user(account))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Ник с недопустимыми символами отклоняется валидацией")
    fun failure_invalid_username_pattern_rejected() {
        val account = repository.save(
            username = "test",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )

        val request = UpdateUserProfileRequest(
            username = "invalid username!",
            avatarUrl = account.avatarUrl,
            bio = account.bio,
        )

        mvc.perform(
            put("$path/${account.id}")
                .with(user(account))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Невалидное тело запроса")
    fun failure_malformed_body() {
        val account = repository.save(
            username = "test",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )

        mvc.perform(
            put("$path/${account.id}")
                .with(user(account))
                .contentType(MediaType.APPLICATION_JSON)
                .content("not a json")
        )
            .andExpect(status().isBadRequest)
    }

}
