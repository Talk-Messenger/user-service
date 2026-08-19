package com.user.user.path

import com.user.exceptions.user.UserNotFoundException
import com.user.user.application.use_case.public_profile.PublicProfileCommand
import com.user.user.application.use_case.public_profile.PublicProfileUseCase
import com.user.user.domain.model.User
import com.user.user.domain.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

@DisplayName("Поиск по нику | Unit")
class PathVariableUnitTests {

    private val repository: UserRepository = mock()
    private val useCase: PublicProfileUseCase = PublicProfileUseCase(repository)

    private val mockUser = User(
        id = UUID.randomUUID(),
        username = "user",
        avatarUrl = "some_avatar_url",
        bio = "some_bio",
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        deletedAt = null
    )

    @Test
    @DisplayName("Успех")
    fun success() {
        val command = PublicProfileCommand(
            username = "test",
        )

        whenever(repository.getByUsername("test")).thenReturn(mockUser)

        val result = useCase.execute(command)

        assertEquals(mockUser.id, result.id)
        assertEquals(mockUser.username, result.username)
        assertEquals(mockUser.avatarUrl, result.avatarUrl)
        assertEquals(mockUser.bio, result.bio)
        assertEquals(mockUser.createdAt, result.createdAt)
        assertEquals(mockUser.updatedAt, result.updatedAt)
        assertEquals(mockUser.deletedAt, result.deletedAt)
    }

    @Test
    @DisplayName("Пользователь не найден")
    fun failure_not_found() {
        val command = PublicProfileCommand(
            username = "test",
        )

        whenever(repository.getByUsername("test")).thenReturn(null)

        assertThrows<UserNotFoundException> { useCase.execute(command) }
    }

}