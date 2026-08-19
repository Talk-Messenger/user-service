package com.user.contact.get

import com.user.UserApplication
import com.user.contact.application.use_case.get.GetContactsCommand
import com.user.contact.application.use_case.get.GetContactsUseCase
import com.user.contact.domain.repository.ContactRepository
import com.user.contact.infrastructure.mapper.ContactMapper
import com.user.contact.infrastructure.persistance.repository.ContactJpaRepository
import com.user.user.domain.repository.UserRepository
import com.user.user.infrastructure.persistance.repository.UserJpaRepository
import com.user.utlis.TestContainersConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.postgresql.PostgreSQLContainer
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("Получение контактов | Integrated")
@SpringBootTest(classes = [UserApplication::class])
@TestPropertySource(properties = ["spring.cache.type=NONE"])
class GetContactsIntegratedTests @Autowired constructor(
    private val useCase: GetContactsUseCase,
    private val mapper: ContactMapper,
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
    @DisplayName("И user, и contactUser мапятся в UserSearchItemResponse")
    fun success_both_users_are_mapped_to_search_item_response() {
        val user = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")
        val contactUser = userRepository.save(username = "contact", avatarUrl = "contact_avatar", bio = "bio")
        contactRepository.save(user, contactUser)

        val page = useCase.execute(
            GetContactsCommand(user = user, pageable = PageRequest.of(0, 20))
        )

        assertEquals(1, page.totalElements)

        val response = mapper.toResponse(page.content.first())

        assertEquals(user.id, response.user.id)
        assertEquals(user.username, response.user.username)
        assertEquals(user.avatarUrl, response.user.avatarUrl)

        assertEquals(contactUser.id, response.contactUser.id)
        assertEquals(contactUser.username, response.contactUser.username)
        assertEquals(contactUser.avatarUrl, response.contactUser.avatarUrl)
    }

    @Test
    @DisplayName("Ничего не найдено")
    fun success_no_contacts() {
        val user = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")

        val page = useCase.execute(
            GetContactsCommand(user = user, pageable = PageRequest.of(0, 20))
        )

        assertTrue(page.content.isEmpty())
        assertEquals(0, page.totalElements)
    }

    @Test
    @DisplayName("Видны только свои контакты")
    fun success_only_own_contacts_visible() {
        val user = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")
        val contactUser = userRepository.save(username = "contact", avatarUrl = "avatar", bio = "bio")
        val stranger = userRepository.save(username = "stranger", avatarUrl = "avatar", bio = "bio")
        val strangerContactUser = userRepository.save(username = "stranger_contact", avatarUrl = "avatar", bio = "bio")
        contactRepository.save(user, contactUser)
        contactRepository.save(stranger, strangerContactUser)

        val page = useCase.execute(
            GetContactsCommand(user = user, pageable = PageRequest.of(0, 20))
        )

        assertEquals(1, page.totalElements)
        assertEquals(contactUser.id, page.content.first().contactUser.id)
    }

    @Test
    @DisplayName("Пагинация ограничивает размер страницы, total считает все контакты")
    fun success_pagination() {
        val user = userRepository.save(username = "user", avatarUrl = "avatar", bio = "bio")
        repeat(5) { i ->
            val contactUser = userRepository.save(username = "contact$i", avatarUrl = "avatar", bio = "bio")
            contactRepository.save(user, contactUser)
        }

        val firstPage = useCase.execute(GetContactsCommand(user = user, pageable = PageRequest.of(0, 2)))
        val secondPage = useCase.execute(GetContactsCommand(user = user, pageable = PageRequest.of(1, 2)))

        assertEquals(2, firstPage.content.size)
        assertEquals(2, secondPage.content.size)
        assertEquals(5, firstPage.totalElements)
        assertEquals(3, firstPage.totalPages)
        assertTrue(firstPage.content.map { it.id } != secondPage.content.map { it.id })
    }

}
