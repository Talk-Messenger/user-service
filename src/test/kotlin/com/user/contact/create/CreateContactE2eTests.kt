package com.user.contact.create

import com.fasterxml.jackson.databind.ObjectMapper
import com.user.UserApplication
import com.user.contact.domain.repository.ContactRepository
import com.user.contact.infrastructure.persistance.repository.ContactJpaRepository
import com.user.contact.infrastructure.web.dto.request.CreateContactRequest
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
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID

@Import(MapperConfiguration::class)
@DisplayName("Создание контакта | E2e")
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@SpringBootTest(classes = [UserApplication::class])
class CreateContactE2eTests @Autowired constructor(

    private val mvc: MockMvc,
    private val mapper: ObjectMapper,
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository,
    private val userJpaRepository: UserJpaRepository,
    private val contactJpaRepository: ContactJpaRepository,

) {

    private final val path = "/api/v1/contacts"

    companion object {
        @Container @ServiceConnection @JvmStatic
        private val postgres: PostgreSQLContainer = TestContainersConfig.POSTGRES

        @Container @ServiceConnection @JvmStatic
        private val redis: GenericContainer<*> = TestContainersConfig.REDIS
    }

    @BeforeEach
    fun beforeEach() {
        contactJpaRepository.deleteAll()
        userJpaRepository.deleteAll()
    }

    @Test
    @DisplayName("Успех")
    fun success() {
        val account = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")
        val contactUser = userRepository.save(username = "contact", avatarUrl = "avatar", bio = "bio")

        val request = CreateContactRequest(contactUserId = contactUser.id)

        mvc.perform(
            post(path)
                .with(user(account))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))

        assert(contactRepository.existsByIds(account.id, contactUser.id))
    }

    @Test
    @DisplayName("Без токена запрос не проходит")
    fun failure_unauthenticated() {
        val request = CreateContactRequest(contactUserId = UUID.randomUUID())

        mvc.perform(
            post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("Нельзя добавить самого себя")
    fun failure_cannot_add_yourself() {
        val account = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")

        val request = CreateContactRequest(contactUserId = account.id)

        mvc.perform(
            post(path)
                .with(user(account))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Невозможно создать контакт с самим собой"))
    }

    @Test
    @DisplayName("Пользователь не найден")
    fun failure_user_not_found() {
        val account = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")

        val request = CreateContactRequest(contactUserId = UUID.randomUUID())

        mvc.perform(
            post(path)
                .with(user(account))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Пользователь не найден"))
    }

    @Test
    @DisplayName("Контакт уже существует")
    fun failure_contact_already_exists() {
        val account = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")
        val contactUser = userRepository.save(username = "contact", avatarUrl = "avatar", bio = "bio")
        contactRepository.save(account, contactUser)

        val request = CreateContactRequest(contactUserId = contactUser.id)

        mvc.perform(
            post(path)
                .with(user(account))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Такой контакт уже существует"))
    }

    @Test
    @DisplayName("Отсутствует contactUserId")
    fun failure_missing_contact_user_id() {
        val account = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")

        mvc.perform(
            post(path)
                .with(user(account))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Невалидное тело запроса")
    fun failure_malformed_body() {
        val account = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")

        mvc.perform(
            post(path)
                .with(user(account))
                .contentType(MediaType.APPLICATION_JSON)
                .content("not a json")
        )
            .andExpect(status().isBadRequest)
    }

}
