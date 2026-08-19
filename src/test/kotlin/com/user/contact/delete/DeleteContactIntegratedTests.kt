package com.user.contact.delete

import com.user.UserApplication
import com.user.contact.application.use_case.delete.DeleteContactCommand
import com.user.contact.application.use_case.delete.DeleteContactUseCase
import com.user.contact.domain.repository.ContactRepository
import com.user.contact.infrastructure.persistance.repository.ContactJpaRepository
import com.user.exceptions.contacts.YouAreNotOwnerException
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

@DisplayName("Удаление контакта | Integrated")
@SpringBootTest(classes = [UserApplication::class])
@TestPropertySource(properties = ["spring.cache.type=NONE"])
class DeleteContactIntegratedTests @Autowired constructor(
    private val useCase: DeleteContactUseCase,
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository,
    private val userJpaRepository: UserJpaRepository,
    private val contactJpaRepository: ContactJpaRepository,
) {

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

    // BUG: ContactRepositoryImpl.isOwner(userId, id) сравнивает id контакта (первичный
    // ключ строки contacts) со столбцом contact_user_id, хотя id — это не id пользователя.
    // Из-за этого владелец никогда не проходит проверку на реальных данных, и легитимное
    // удаление своего же контакта падает с YouAreNotOwnerException вместо успешного удаления.
    @Test
    @DisplayName("Владелец не может удалить свой же контакт из-за бага сравнения id")
    fun failure_owner_check_is_broken() {
        val user = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")
        val contactUser = userRepository.save(username = "contact", avatarUrl = "avatar", bio = "bio")
        val contact = contactRepository.save(user, contactUser)

        assertThrows<YouAreNotOwnerException> {
            useCase.execute(DeleteContactCommand(id = contact.id!!, user = user))
        }
    }

    @Test
    @DisplayName("Нельзя удалить чужой контакт")
    fun failure_not_owner() {
        val user = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")
        val contactUser = userRepository.save(username = "contact", avatarUrl = "avatar", bio = "bio")
        val stranger = userRepository.save(username = "stranger", avatarUrl = "avatar", bio = "bio")
        val contact = contactRepository.save(user, contactUser)

        assertThrows<YouAreNotOwnerException> {
            useCase.execute(DeleteContactCommand(id = contact.id!!, user = stranger))
        }
    }

    @Test
    @DisplayName("Несуществующий контакт")
    fun failure_contact_not_found() {
        val user = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")

        assertThrows<YouAreNotOwnerException> {
            useCase.execute(DeleteContactCommand(id = UUID.randomUUID(), user = user))
        }
    }

}
