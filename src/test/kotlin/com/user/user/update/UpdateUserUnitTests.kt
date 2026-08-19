package com.user.user.update

import com.user.exceptions.user.UsernameAlreadyExistsException
import com.user.user.application.use_case.update.UpdateUserCommand
import com.user.user.application.use_case.update.UpdateUserUseCase
import com.user.user.domain.model.User
import com.user.user.domain.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

@DisplayName("Обновление профиля | Unit")
class UpdateUserUnitTests {

    private val repository: UserRepository = mock()
    private val useCase: UpdateUserUseCase = UpdateUserUseCase(repository)

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
        val command = UpdateUserCommand(
            userId = mockUser.id,
            user = mockUser,
            username = "new_username",
            avatarUrl = "new_avatar_url",
            bio = "new_bio",
        )

        val updated = mockUser.copy(
            username = "new_username",
            avatarUrl = "new_avatar_url",
            bio = "new_bio",
        )

        whenever(repository.getByUsername("new_username")).thenReturn(null)
        whenever(
            repository.update(mockUser.id, "new_username", "new_avatar_url", "new_bio")
        ).thenReturn(updated)

        val result = useCase.execute(command)

        assertEquals(updated.id, result.id)
        assertEquals(updated.username, result.username)
        assertEquals(updated.avatarUrl, result.avatarUrl)
        assertEquals(updated.bio, result.bio)
    }

    @Test
    @DisplayName("Ник не меняется — проверка уникальности не выполняется")
    fun success_same_username_skips_uniqueness_check() {
        val command = UpdateUserCommand(
            userId = mockUser.id,
            user = mockUser,
            username = mockUser.username,
            avatarUrl = "new_avatar_url",
            bio = "new_bio",
        )

        whenever(
            repository.update(mockUser.id, mockUser.username, "new_avatar_url", "new_bio")
        ).thenReturn(mockUser.copy(avatarUrl = "new_avatar_url", bio = "new_bio"))

        useCase.execute(command)

        verify(repository, never()).getByUsername(any())
    }

    @Test
    @DisplayName("Ник уже занят другим пользователем")
    fun failure_username_already_exists() {
        val command = UpdateUserCommand(
            userId = mockUser.id,
            user = mockUser,
            username = "taken_username",
            avatarUrl = mockUser.avatarUrl,
            bio = mockUser.bio,
        )

        whenever(repository.getByUsername("taken_username")).thenReturn(
            mockUser.copy(id = UUID.randomUUID(), username = "taken_username")
        )

        assertThrows<UsernameAlreadyExistsException> { useCase.execute(command) }
    }

}
