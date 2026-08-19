package com.user.user.search

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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.postgresql.PostgreSQLContainer

@Import(MapperConfiguration::class)
@DisplayName("Поиск пользователей | E2e")
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@SpringBootTest(classes = [UserApplication::class])
class SearchUsersE2eTests @Autowired constructor(

    private val mvc: MockMvc,
    private val mapper: ObjectMapper,
    private val repository: UserRepository,
    private val jpaRepository: UserJpaRepository,

) {

    private final val path = "/api/v1/users/search"

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
        val user = repository.save(username = "alice", avatarUrl = "some_avatar_url", bio = "some_bio")
        repository.save(username = "bob", avatarUrl = "some_avatar_url", bio = "some_bio")

        mvc.perform(get(path).queryParam("query", "ali"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].id").value(user.id.toString()))
            .andExpect(jsonPath("$.content[0].username").value("alice"))
            .andExpect(jsonPath("$.content[0].avatarUrl").value(user.avatarUrl))
            .andExpect(jsonPath("$.limit").value(20))
            .andExpect(jsonPath("$.offset").value(0))
            .andExpect(jsonPath("$.total").value(1))
    }

    @Test
    @DisplayName("Ничего не найдено")
    fun success_no_matches() {
        repository.save(username = "alice", avatarUrl = "some_avatar_url", bio = "some_bio")

        mvc.perform(get(path).queryParam("query", "zzz"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.total").value(0))
    }

    @Test
    @DisplayName("Пагинация через page/size")
    fun success_pagination() {
        repeat(5) { i -> repository.save(username = "user$i", avatarUrl = "some_avatar_url", bio = "some_bio") }

        mvc.perform(
            get(path)
                .queryParam("query", "user")
                .queryParam("page", "1")
                .queryParam("size", "2")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.limit").value(2))
            .andExpect(jsonPath("$.offset").value(2))
            .andExpect(jsonPath("$.total").value(5))
    }

    @Test
    @DisplayName("Пустой query отклоняется")
    fun failure_blank_query_rejected() {
        mvc.perform(get(path).queryParam("query", ""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Невалидные параметры запроса"))
    }

    @Test
    @DisplayName("Query параметр отсутствует")
    fun failure_missing_query_rejected() {
        mvc.perform(get(path))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Невалидные параметры запроса"))
    }

    @Test
    @DisplayName("Слишком длинный query отклоняется")
    fun failure_too_long_query_rejected() {
        mvc.perform(get(path).queryParam("query", "a".repeat(101)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Невалидные параметры запроса"))
    }

    @Test
    @DisplayName("Слишком большой номер страницы отклоняется")
    fun failure_page_number_too_large_rejected() {
        mvc.perform(
            get(path)
                .queryParam("query", "user")
                .queryParam("page", "101")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Невалидные параметры запроса"))
    }

}
