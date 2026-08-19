package com.user.contact.domain.repository

import com.user.contact.domain.model.Contact
import com.user.user.domain.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface ContactRepository {

    fun save(
        user: User,
        contactUser: User,
    ): Contact

    fun existsByIds(userId: UUID, contactUserId: UUID): Boolean

    fun getByIdAndUserId(id: UUID, userId: UUID): Contact?

    fun getById(id: UUID, pageable: Pageable): Page<Contact>

    fun deleteByIds(id1: UUID, id2: UUID)

    fun contactsCount(userId: UUID): Long

    fun isOwner(userId: UUID, id: UUID): Boolean

    fun delete(id: UUID)

    /** Каскадное удаление всех связей пользователя (в обе стороны). Возвращает число удалённых строк. */
    fun deleteAllRelatedToUser(userId: UUID): Int

}