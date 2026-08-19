package com.user.contact.create

import com.user.contact.application.use_case.create.CreateContactCommand
import com.user.contact.application.use_case.create.CreateContactUseCase
import com.user.contact.domain.model.Contact
import com.user.contact.domain.repository.ContactRepository
import com.user.exceptions.contacts.CannotCreateContactYourselfException
import com.user.exceptions.contacts.SuchContactAlreadyExistsException
import com.user.exceptions.user.UserNotFoundException
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

@DisplayName("Создание контакта | Unit")
class CreateContactUnitTests {

    private val repository: ContactRepository = mock()
    private val userRepository: UserRepository = mock()
    private val useCase: CreateContactUseCase = CreateContactUseCase(repository, userRepository)

    private val mockUser = User(
        id = UUID.randomUUID(),
        username = "user",
        avatarUrl = "some_avatar_url",
        bio = "some_bio",
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        deletedAt = null,
    )

    private val mockContactUser = User(
        id = UUID.randomUUID(),
        username = "contact",
        avatarUrl = "contact_avatar_url",
        bio = "contact_bio",
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        deletedAt = null,
    )

    @Test
    @DisplayName("Успех")
    fun success() {
        val command = CreateContactCommand(user = mockUser, contactUserId = mockContactUser.id)
        val contact = Contact(
            id = UUID.randomUUID(),
            user = mockUser,
            contactUser = mockContactUser,
            createdAt = Instant.now(),
        )

        whenever(userRepository.getById(mockContactUser.id)).thenReturn(mockContactUser)
        whenever(repository.existsByIds(mockUser.id, mockContactUser.id)).thenReturn(false)
        whenever(repository.save(mockUser, mockContactUser)).thenReturn(contact)

        val result = useCase.execute(command)

        assertEquals(contact.id, result.id)
        assertEquals(mockUser.id, result.user.id)
        assertEquals(mockContactUser.id, result.contactUser.id)
    }

    @Test
    @DisplayName("Нельзя добавить самого себя")
    fun failure_cannot_add_yourself() {
        val command = CreateContactCommand(user = mockUser, contactUserId = mockUser.id)

        assertThrows<CannotCreateContactYourselfException> { useCase.execute(command) }
        verify(userRepository, never()).getById(any())
    }

    @Test
    @DisplayName("Пользователь не найден")
    fun failure_user_not_found() {
        val command = CreateContactCommand(user = mockUser, contactUserId = mockContactUser.id)

        whenever(userRepository.getById(mockContactUser.id)).thenReturn(null)

        assertThrows<UserNotFoundException> { useCase.execute(command) }
    }

    @Test
    @DisplayName("Контакт уже существует")
    fun failure_contact_already_exists() {
        val command = CreateContactCommand(user = mockUser, contactUserId = mockContactUser.id)

        whenever(userRepository.getById(mockContactUser.id)).thenReturn(mockContactUser)
        whenever(repository.existsByIds(mockUser.id, mockContactUser.id)).thenReturn(true)

        assertThrows<SuchContactAlreadyExistsException> { useCase.execute(command) }
        verify(repository, never()).save(any(), any())
    }

}
