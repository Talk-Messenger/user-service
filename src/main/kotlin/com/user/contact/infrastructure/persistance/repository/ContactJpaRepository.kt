package com.user.contact.infrastructure.persistance.repository

import com.user.contact.infrastructure.persistance.entity.ContactJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ContactJpaRepository : JpaRepository<ContactJpaEntity, UUID> {

    fun existsByUserIdAndContactUserId(userId: UUID, contactUserId: UUID): Boolean

    @EntityGraph(attributePaths = ["user", "contact"])
    fun findByIdAndUserId(contactId: UUID, userId: UUID): ContactJpaEntity?

    @EntityGraph(attributePaths = ["user", "contact"])
    fun findByUserId(userId: UUID, pageable: Pageable): Page<ContactJpaEntity>

    fun deleteAllByUserIdOrContactUserId(userId: UUID, userId2: UUID)

    fun countByUserId(userId: UUID): Long

    fun existsByIdAndUserId(id: UUID, userId: UUID): Boolean

    /**
     * Каскад при удалении пользователя: убираем связи в обе стороны — и там,
     * где он владелец, и там, где он значится чужим контактом. Возвращает число удалённых строк.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ContactJpaEntity c where c.userId = :userId or c.contactUserId = :userId")
    fun deleteAllRelatedToUser(@Param("userId") userId: UUID): Int

}