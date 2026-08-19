package com.user.user.search

import com.user.user.application.use_case.search.SearchUserCommand
import com.user.user.application.use_case.search.SearchUserUseCase
import com.user.user.domain.model.User
import com.user.user.domain.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("Поиск пользователей | Unit")
class SearchUsersUnitTests {

    private val repository: UserRepository = mock()
    private val useCase: SearchUserUseCase = SearchUserUseCase(repository)

    private val mockUser = User(
        id = UUID.randomUUID(),
        username = "user",
        avatarUrl = "some_avatar_url",
        bio = "some_bio",
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        deletedAt = null,
    )

    @Test
    @DisplayName("Успех")
    fun success() {
        val pageable = PageRequest.of(0, 20)
        val command = SearchUserCommand(query = "user", pageable = pageable)
        val page = PageImpl(listOf(mockUser), pageable, 1)

        whenever(repository.searchByUsername("user", pageable)).thenReturn(page)

        val result = useCase.execute(command)

        assertEquals(1, result.totalElements)
        assertEquals(mockUser.id, result.content.first().id)
    }

    @Test
    @DisplayName("Ничего не найдено")
    fun success_empty_result() {
        val pageable = PageRequest.of(0, 20)
        val command = SearchUserCommand(query = "unknown", pageable = pageable)
        val page = PageImpl<User>(emptyList(), pageable, 0)

        whenever(repository.searchByUsername("unknown", pageable)).thenReturn(page)

        val result = useCase.execute(command)

        assertTrue(result.content.isEmpty())
        assertEquals(0, result.totalElements)
    }

    @Test
    @DisplayName("Передаёт pageable в репозиторий как есть")
    fun success_passes_pageable_through() {
        val pageable = PageRequest.of(2, 5)
        val command = SearchUserCommand(query = "user", pageable = pageable)
        val page = PageImpl(listOf(mockUser), pageable, 11)

        whenever(repository.searchByUsername("user", pageable)).thenReturn(page)

        val result = useCase.execute(command)

        assertEquals(2, result.pageable.pageNumber)
        assertEquals(5, result.pageable.pageSize)
        assertEquals(11, result.totalElements)
    }

}
