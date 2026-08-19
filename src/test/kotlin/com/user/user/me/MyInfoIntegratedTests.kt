package com.user.user.me

import com.user.UserApplication
import com.user.contact.domain.repository.ContactRepository
import com.user.contact.infrastructure.persistance.entity.ContactJpaEntity
import com.user.contact.infrastructure.persistance.repository.ContactJpaRepository
import com.user.exceptions.user.UserNotFoundException
import com.user.user.application.use_case.me.GetMyInfoCommand
import com.user.user.application.use_case.me.GetMyInfoUseCase
import com.user.user.domain.model.User
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

@DisplayName("Моя анкета | Integrated")
@SpringBootTest(classes = [UserApplication::class])
@TestPropertySource(properties = ["spring.cache.type=NONE"])
class MyInfoIntegratedTests @Autowired constructor(
    private val useCase: GetMyInfoUseCase,
    private val repository: UserRepository,
    private val contactRepository: ContactRepository,
    private val jpaRepository: UserJpaRepository,
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

        val command = GetMyInfoCommand(user.id)

        val result = useCase.execute(command)

        assertEquals(user.id, result.id)
        assertEquals(user.username, result.username)
        assertEquals(user.avatarUrl, result.avatarUrl)
        assertEquals(user.bio, result.bio)
        assertEquals(0, result.contactsCount)
        assertEquals(user.createdAt, result.createdAt)
        assertEquals(user.updatedAt, result.updatedAt)
    }

    @Test
    @DisplayName("Успех с учётом количества контактов")
    fun success_with_contacts_count() {
        val user = repository.save(
            username = "test",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )
        val contact1 = repository.save(
            username = "contact1",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )
        val contact2 = repository.save(
            username = "contact2",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )

        val userEntity = jpaRepository.findByUsernameAndDeletedAtIsNull("test")!!
        val contact1Entity = jpaRepository.findByUsernameAndDeletedAtIsNull("contact1")!!
        val contact2Entity = jpaRepository.findByUsernameAndDeletedAtIsNull("contact2")!!

        contactJpaRepository.save(
            ContactJpaEntity(
                userId = user.id,
                contactUserId = contact1.id,
                user = userEntity,
                contact = contact1Entity,
                createdAt = java.time.Instant.now(),
            )
        )
        contactJpaRepository.save(
            ContactJpaEntity(
                userId = user.id,
                contactUserId = contact2.id,
                user = userEntity,
                contact = contact2Entity,
                createdAt = java.time.Instant.now(),
            )
        )

        val result = useCase.execute(GetMyInfoCommand(user.id))

        assertEquals(2, result.contactsCount)
    }

    @Test
    @DisplayName("Пользователь не найден")
    fun failure_user_not_found() {
        val command = GetMyInfoCommand(UUID.randomUUID())

        assertThrows<UserNotFoundException> { useCase.execute(command) }
    }

    @Test
    @DisplayName("Пользователь удалён")
    fun failure_user_deleted() {
        val user = repository.save(
            username = "test",
            avatarUrl = "some_avatar_url",
            bio = "some_bio",
        )
        val entity = jpaRepository.findByUsernameAndDeletedAtIsNull("test")!!
        jpaRepository.save(entity.copy(deletedAt = java.time.Instant.now()))

        assertThrows<UserNotFoundException> { useCase.execute(GetMyInfoCommand(user.id)) }
    }

}
