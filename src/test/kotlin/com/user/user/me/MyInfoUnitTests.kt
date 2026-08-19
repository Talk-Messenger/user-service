package com.user.user.me

import com.user.contact.domain.repository.ContactRepository
import com.user.exceptions.user.UserNotFoundException
import com.user.user.application.use_case.me.GetMyInfoCommand
import com.user.user.application.use_case.me.GetMyInfoUseCase
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

@DisplayName("Моя анкета | Unit")
class MyInfoUnitTests {

    private val repository: UserRepository = mock()
    private val contactRepository: ContactRepository = mock()
    private val useCase: GetMyInfoUseCase = GetMyInfoUseCase(repository, contactRepository)

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
        val command = GetMyInfoCommand(mockUser.id)

        whenever(repository.getById(mockUser.id)).thenReturn(mockUser)
        whenever(contactRepository.contactsCount(mockUser.id)).thenReturn(5L)

        val result = useCase.execute(command)

        assertEquals(mockUser.id, result.id)
        assertEquals(mockUser.username, result.username)
        assertEquals(mockUser.avatarUrl, result.avatarUrl)
        assertEquals(mockUser.bio, result.bio)
        assertEquals(5, result.contactsCount)
        assertEquals(mockUser.createdAt, result.createdAt)
        assertEquals(mockUser.updatedAt, result.updatedAt)
    }

    @Test
    @DisplayName("Пользователь не найден")
    fun failure_not_found() {
        val command = GetMyInfoCommand(mockUser.id)

        whenever(repository.getById(mockUser.id)).thenReturn(null)

        assertThrows<UserNotFoundException> { useCase.execute(command) }
    }

}
