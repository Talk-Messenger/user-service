package com.user.contact.delete

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID

@Import(MapperConfiguration::class)
@DisplayName("Удаление контакта | E2e")
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@SpringBootTest(classes = [UserApplication::class])
class DeleteContactE2eTests @Autowired constructor(

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

    // BUG: см. DeleteContactIntegratedTests — ContactRepositoryImpl.isOwner сравнивает id
    // контакта со столбцом contact_user_id, поэтому владелец никогда не проходит проверку,
    // и удаление собственного контакта всегда отдаёт 403 вместо 204.
    @Test
    @DisplayName("Владелец не может удалить свой же контакт из-за бага проверки владения")
    fun failure_owner_check_is_broken() {
        val account = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")
        val contactUser = userRepository.save(username = "contact", avatarUrl = "avatar", bio = "bio")
        val contact = contactRepository.save(account, contactUser)

        mvc.perform(delete("$path/${contact.id}").with(user(account)))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Можно удалять только свои контакты"))
    }

    @Test
    @DisplayName("Без токена запрос не проходит")
    fun failure_unauthenticated() {
        mvc.perform(delete("$path/${UUID.randomUUID()}"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("Нельзя удалить чужой контакт")
    fun failure_not_owner() {
        val account = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")
        val contactUser = userRepository.save(username = "contact", avatarUrl = "avatar", bio = "bio")
        val stranger = userRepository.save(username = "stranger", avatarUrl = "avatar", bio = "bio")
        val contact = contactRepository.save(account, contactUser)

        mvc.perform(delete("$path/${contact.id}").with(user(stranger)))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Можно удалять только свои контакты"))
    }

    @Test
    @DisplayName("Несуществующий контакт")
    fun failure_contact_not_found() {
        val account = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")

        mvc.perform(delete("$path/${UUID.randomUUID()}").with(user(account)))
            .andExpect(status().isForbidden)
    }

}
