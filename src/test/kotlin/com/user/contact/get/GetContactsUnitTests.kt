package com.user.contact.get

import com.user.contact.application.use_case.get.GetContactsCommand
import com.user.contact.application.use_case.get.GetContactsUseCase
import com.user.contact.domain.model.Contact
import com.user.contact.domain.repository.ContactRepository
import com.user.user.domain.model.User
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

@DisplayName("Получение контактов | Unit")
class GetContactsUnitTests {

    private val repository: ContactRepository = mock()
    private val useCase: GetContactsUseCase = GetContactsUseCase(repository)

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
        val pageable = PageRequest.of(0, 20)
        val contact = Contact(
            id = UUID.randomUUID(),
            user = mockUser,
            contactUser = mockContactUser,
            createdAt = Instant.now(),
        )
        val page = PageImpl(listOf(contact), pageable, 1)

        whenever(repository.getById(mockUser.id, pageable)).thenReturn(page)

        val result = useCase.execute(GetContactsCommand(user = mockUser, pageable = pageable))

        assertEquals(1, result.totalElements)
        assertEquals(contact.id, result.content.first().id)
        assertEquals(mockContactUser.id, result.content.first().contactUser.id)
    }

    @Test
    @DisplayName("Ничего не найдено")
    fun success_empty_result() {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl<Contact>(emptyList(), pageable, 0)

        whenever(repository.getById(mockUser.id, pageable)).thenReturn(page)

        val result = useCase.execute(GetContactsCommand(user = mockUser, pageable = pageable))

        assertTrue(result.content.isEmpty())
        assertEquals(0, result.totalElements)
    }

    @Test
    @DisplayName("Передаёт pageable в репозиторий как есть")
    fun success_passes_pageable_through() {
        val pageable = PageRequest.of(2, 5)
        val page = PageImpl<Contact>(emptyList(), pageable, 11)

        whenever(repository.getById(mockUser.id, pageable)).thenReturn(page)

        val result = useCase.execute(GetContactsCommand(user = mockUser, pageable = pageable))

        assertEquals(2, result.pageable.pageNumber)
        assertEquals(5, result.pageable.pageSize)
        assertEquals(11, result.totalElements)
    }

}
