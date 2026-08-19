package com.user.contact.delete

import com.user.contact.application.use_case.delete.DeleteContactCommand
import com.user.contact.application.use_case.delete.DeleteContactUseCase
import com.user.contact.domain.repository.ContactRepository
import com.user.exceptions.contacts.YouAreNotOwnerException
import com.user.user.domain.model.User
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

@DisplayName("Удаление контакта | Unit")
class DeleteContactUnitTests {

    private val repository: ContactRepository = mock()
    private val useCase: DeleteContactUseCase = DeleteContactUseCase(repository)

    private val mockUser = User(
        id = UUID.randomUUID(),
        username = "user",
        avatarUrl = "some_avatar_url",
        bio = "some_bio",
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        deletedAt = null,
    )

    private val contactId = UUID.randomUUID()

    @Test
    @DisplayName("Успех")
    fun success() {
        val command = DeleteContactCommand(id = contactId, user = mockUser)

        whenever(repository.isOwner(mockUser.id, contactId)).thenReturn(true)

        useCase.execute(command)

        verify(repository, times(1)).delete(contactId)
    }

    @Test
    @DisplayName("Нельзя удалить чужой контакт")
    fun failure_not_owner() {
        val command = DeleteContactCommand(id = contactId, user = mockUser)

        whenever(repository.isOwner(mockUser.id, contactId)).thenReturn(false)

        assertThrows<YouAreNotOwnerException> { useCase.execute(command) }
        verify(repository, never()).delete(org.mockito.kotlin.any())
    }

}
