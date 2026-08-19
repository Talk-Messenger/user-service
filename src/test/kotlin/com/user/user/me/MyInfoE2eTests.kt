package com.user.user.me

import com.fasterxml.jackson.databind.ObjectMapper
import com.user.UserApplication
import com.user.user.domain.repository.UserRepository
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.postgresql.PostgreSQLContainer

@Import(MapperConfiguration::class)
@DisplayName("Моя анкета | E2e")
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@SpringBootTest(classes = [UserApplication::class])
class MyInfoE2eTests @Autowired constructor(

    private val mvc: MockMvc,
    private val mapper: ObjectMapper,
    private val repository: UserRepository,
    private val jpaRepository: UserJpaRepository,

) {

    private final val path = "/api/v1/users/me"

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

        mvc.perform(get(path).with(user(account)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(account.id.toString()))
            .andExpect(jsonPath("$.username").value(account.username))
            .andExpect(jsonPath("$.avatarUrl").value(account.avatarUrl))
            .andExpect(jsonPath("$.bio").value(account.bio))
            .andExpect(jsonPath("$.contactsCount").value(0))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists())
    }

    @Test
    @DisplayName("Без токена запрос не проходит")
    fun failure_unauthenticated() {
        mvc.perform(get(path))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("Пользователь удалён после выдачи токена")
    fun failure_user_deleted_after_token_issued() {
        val account = repository.save(
            username = "test",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )
        jpaRepository.deleteAll()

        mvc.perform(get(path).with(user(account)))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Пользователь не найден"))
    }

}
