package com.user.contact.create

import com.user.UserApplication
import com.user.contact.application.use_case.create.CreateContactCommand
import com.user.contact.application.use_case.create.CreateContactUseCase
import com.user.contact.domain.repository.ContactRepository
import com.user.contact.infrastructure.persistance.repository.ContactJpaRepository
import com.user.exceptions.contacts.CannotCreateContactYourselfException
import com.user.exceptions.contacts.SuchContactAlreadyExistsException
import com.user.exceptions.user.UserNotFoundException
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

@DisplayName("Создание контакта | Integrated")
@SpringBootTest(classes = [UserApplication::class])
@TestPropertySource(properties = ["spring.cache.type=NONE"])
class CreateContactIntegratedTests @Autowired constructor(
    private val useCase: CreateContactUseCase,
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

    @Test
    @DisplayName("Успех")
    fun success() {
        val user = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")
        val contactUser = userRepository.save(username = "contact", avatarUrl = "avatar", bio = "bio")

        val result = useCase.execute(
            CreateContactCommand(user = user, contactUserId = contactUser.id)
        )

        assertEquals(user.id, result.user.id)
        assertEquals(contactUser.id, result.contactUser.id)
        assertEquals(true, contactRepository.existsByIds(user.id, contactUser.id))
    }

    @Test
    @DisplayName("Нельзя добавить самого себя")
    fun failure_cannot_add_yourself() {
        val user = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")

        assertThrows<CannotCreateContactYourselfException> {
            useCase.execute(CreateContactCommand(user = user, contactUserId = user.id))
        }
    }

    @Test
    @DisplayName("Пользователь не найден")
    fun failure_user_not_found() {
        val user = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")

        assertThrows<UserNotFoundException> {
            useCase.execute(CreateContactCommand(user = user, contactUserId = UUID.randomUUID()))
        }
    }

    @Test
    @DisplayName("Контакт уже существует")
    fun failure_contact_already_exists() {
        val user = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")
        val contactUser = userRepository.save(username = "contact", avatarUrl = "avatar", bio = "bio")
        contactRepository.save(user, contactUser)

        assertThrows<SuchContactAlreadyExistsException> {
            useCase.execute(CreateContactCommand(user = user, contactUserId = contactUser.id))
        }
    }

}
