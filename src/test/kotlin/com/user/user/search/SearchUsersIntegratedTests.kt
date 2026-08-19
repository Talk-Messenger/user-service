package com.user.user.search

import com.user.UserApplication
import com.user.user.application.use_case.search.SearchUserCommand
import com.user.user.application.use_case.search.SearchUserUseCase
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

@DisplayName("Поиск пользователей | Integrated")
@SpringBootTest(classes = [UserApplication::class])
@TestPropertySource(properties = ["spring.cache.type=NONE"])
class SearchUsersIntegratedTests @Autowired constructor(
    private val useCase: SearchUserUseCase,
    private val repository: UserRepository,
    private val jpaRepository: UserJpaRepository,
) {

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
        repository.save(username = "alice", avatarUrl = "avatar", bio = "bio")
        repository.save(username = "bob", avatarUrl = "avatar", bio = "bio")

        val result = useCase.execute(
            SearchUserCommand(query = "ali", pageable = PageRequest.of(0, 20))
        )

        assertEquals(1, result.totalElements)
        assertEquals("alice", result.content.first().username)
    }

    @Test
    @DisplayName("Поиск не зависит от регистра")
    fun success_case_insensitive() {
        repository.save(username = "Alice", avatarUrl = "avatar", bio = "bio")

        val result = useCase.execute(
            SearchUserCommand(query = "ALI", pageable = PageRequest.of(0, 20))
        )

        assertEquals(1, result.totalElements)
        assertEquals("Alice", result.content.first().username)
    }

    @Test
    @DisplayName("Ничего не найдено")
    fun success_no_matches() {
        repository.save(username = "alice", avatarUrl = "avatar", bio = "bio")

        val result = useCase.execute(
            SearchUserCommand(query = "zzz", pageable = PageRequest.of(0, 20))
        )

        assertTrue(result.content.isEmpty())
        assertEquals(0, result.totalElements)
    }

    @Test
    @DisplayName("Пагинация ограничивает размер страницы, total считает все совпадения")
    fun success_pagination() {
        repeat(5) { i -> repository.save(username = "user$i", avatarUrl = "avatar", bio = "bio") }

        val firstPage = useCase.execute(
            SearchUserCommand(query = "user", pageable = PageRequest.of(0, 2))
        )
        val secondPage = useCase.execute(
            SearchUserCommand(query = "user", pageable = PageRequest.of(1, 2))
        )

        assertEquals(2, firstPage.content.size)
        assertEquals(2, secondPage.content.size)
        assertEquals(5, firstPage.totalElements)
        assertEquals(3, firstPage.totalPages)
        assertTrue(firstPage.content.map { it.id } != secondPage.content.map { it.id })
    }

    @Test
    @DisplayName("Пустая страница за пределами диапазона")
    fun success_page_out_of_range() {
        repository.save(username = "alice", avatarUrl = "avatar", bio = "bio")

        val result = useCase.execute(
            SearchUserCommand(query = "ali", pageable = PageRequest.of(5, 20))
        )

        assertTrue(result.content.isEmpty())
        assertEquals(1, result.totalElements)
    }

}
