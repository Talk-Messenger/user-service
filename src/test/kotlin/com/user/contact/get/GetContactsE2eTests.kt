package com.user.contact.get

import com.user.UserApplication
import com.user.contact.domain.repository.ContactRepository
import com.user.contact.infrastructure.persistance.repository.ContactJpaRepository
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
@DisplayName("Получение контактов | E2e")
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@SpringBootTest(classes = [UserApplication::class])
class GetContactsE2eTests @Autowired constructor(

    private val mvc: MockMvc,
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
        val contactUser = userRepository.save(username = "contact", avatarUrl = "contact_avatar", bio = "bio")
        contactRepository.save(account, contactUser)

        mvc.perform(get(path).with(user(account)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].user.id").value(account.id.toString()))
            .andExpect(jsonPath("$.content[0].user.username").value(account.username))
            .andExpect(jsonPath("$.content[0].user.avatarUrl").value(account.avatarUrl))
            .andExpect(jsonPath("$.content[0].contactUser.id").value(contactUser.id.toString()))
            .andExpect(jsonPath("$.content[0].contactUser.username").value(contactUser.username))
            .andExpect(jsonPath("$.content[0].contactUser.avatarUrl").value(contactUser.avatarUrl))
    }

    @Test
    @DisplayName("Без токена запрос не проходит")
    fun failure_unauthenticated() {
        mvc.perform(get(path))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("Ничего не найдено")
    fun success_no_contacts() {
        val account = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")

        mvc.perform(get(path).with(user(account)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(0))
    }

    @Test
    @DisplayName("Видны только свои контакты")
    fun success_only_own_contacts_visible() {
        val account = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")
        val contactUser = userRepository.save(username = "contact", avatarUrl = "avatar", bio = "bio")
        val stranger = userRepository.save(username = "stranger", avatarUrl = "avatar", bio = "bio")
        val strangerContactUser = userRepository.save(username = "stranger_contact", avatarUrl = "avatar", bio = "bio")
        contactRepository.save(account, contactUser)
        contactRepository.save(stranger, strangerContactUser)

        mvc.perform(get(path).with(user(account)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].contactUser.id").value(contactUser.id.toString()))
    }

}
