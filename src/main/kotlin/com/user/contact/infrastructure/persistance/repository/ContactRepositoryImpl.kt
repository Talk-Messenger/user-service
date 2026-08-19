package com.user.contact.infrastructure.persistance.repository

import com.user.contact.domain.model.Contact
import com.user.contact.domain.repository.ContactRepository
import com.user.contact.infrastructure.mapper.ContactMapper
import com.user.contact.infrastructure.payloads.ContactAddedPayload
import com.user.contact.infrastructure.payloads.ContactRemovedPayload
import com.user.contact.infrastructure.persistance.entity.ContactJpaEntity
import com.user.outboxevent.domain.event.EventType
import com.user.outboxevent.domain.writer.OutboxEventWriter
import com.user.user.domain.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Repository
class ContactRepositoryImpl(
    val mapper: ContactMapper,
    val repository: ContactJpaRepository,
    val outboxEventWriter: OutboxEventWriter
) : ContactRepository {

    @Transactional
    override fun save(user: User, contactUser: User): Contact {
        val saved = repository.save(
            ContactJpaEntity(
                userId = user.id,
                contactUserId = contactUser.id,
                createdAt = Instant.now(),
            )
        )
        outboxEventWriter.write(
            eventType = EventType.CONTACT_ADDED,
            aggregateId = saved.id.toString(),
            payload = ContactAddedPayload(
                id = saved.id!!,
                userId = user.id,
                contactId = contactUser.id,
                addedAt = saved.createdAt,
            )
        )
        return Contact(
            id = saved.id,
            user = user,
            contactUser = contactUser,
            createdAt = saved.createdAt,
        )
    }

    override fun existsByIds(userId: UUID, contactUserId: UUID): Boolean
        = repository.existsByUserIdAndContactUserId(userId, contactUserId)

    override fun getByIdAndUserId(id: UUID, userId: UUID): Contact? {
        val entity = repository.findByIdAndUserId(id, userId)
        return if (entity != null) mapper.toModel(entity) else null
    }

    override fun getById(id: UUID, pageable: Pageable): Page<Contact>
        = repository.findByUserId(id, pageable)
            .map { mapper.toModel(it) }

    @Transactional
    override fun deleteByIds(id1: UUID, id2: UUID) {
        repository.deleteAllByUserIdOrContactUserId(id1, id2)
        outboxEventWriter.write(
            eventType = EventType.CONTACT_REMOVED,
            aggregateId = id1.toString(),
            payload = ContactRemovedPayload(
                userId = id1.toString(),
                contactId = id2.toString(),
                deletedAt = Instant.now()
            )
        )
    }

    override fun contactsCount(userId: UUID): Long
        = repository.countByUserId(userId)

    override fun isOwner(userId: UUID, id: UUID): Boolean
        = repository.existsByIdAndUserId(userId, id)

    override fun delete(id: UUID)
        = repository.deleteById(id)

    @Transactional
    override fun deleteAllRelatedToUser(userId: UUID): Int
        = repository.deleteAllRelatedToUser(userId)

}