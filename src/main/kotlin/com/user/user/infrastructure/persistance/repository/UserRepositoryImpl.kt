package com.user.user.infrastructure.persistance.repository

import com.user.exceptions.user.UserNotFoundException
import com.user.outboxevent.domain.event.EventType
import com.user.outboxevent.domain.writer.OutboxEventWriter
import com.user.outboxevent.infrastructure.persistance.repository.OutboxEventJpaRepository
import com.user.user.domain.model.User
import com.user.user.domain.repository.UserRepository
import com.user.user.infrastructure.mapper.UserMapper
import com.user.user.infrastructure.payloads.UserCreatedPayload
import com.user.user.infrastructure.payloads.UserDeletedPayload
import com.user.user.infrastructure.payloads.UserUpdatedPayload
import com.user.user.infrastructure.persistance.entity.UserJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
class UserRepositoryImpl(
    val repository: UserJpaRepository,
    val outboxEventWriter: OutboxEventWriter,
    val mapper: UserMapper
) : UserRepository {

    @Transactional
    override fun save(
        username: String,
        avatarUrl: String,
        bio: String,
    ): User {
        val newUser = mapper.toModel(
            repository.save(
                UserJpaEntity(
                    username = username,
                    avatarUrl = avatarUrl,
                    bio = bio,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                    deletedAt = null
                )
            )
        )
        outboxEventWriter.write(
            eventType = EventType.USER_CREATED,
            aggregateId = newUser.id.toString(),
            payload = UserCreatedPayload(
                userId = newUser.id,
                username = newUser.username,
                createdAt = newUser.createdAt
            )
        )
        return newUser
    }

    @Transactional
    override fun update(
        id: UUID,
        username: String,
        avatarUrl: String,
        bio: String
    ): User {
        val entity = repository.findById(id).orElseThrow { UserNotFoundException() }
        val updatedUser = mapper.toModel(
            repository.saveAndFlush(
                entity.copy(username = username, avatarUrl = avatarUrl, bio = bio)
            )
        )
        outboxEventWriter.write(
            eventType = EventType.USER_UPDATED,
            aggregateId = updatedUser.id.toString(),
            payload = UserUpdatedPayload(
                userId = updatedUser.id,
                displayName = updatedUser.username,
                avatarUrl = updatedUser.avatarUrl,
                updatedAt = Instant.now(),
            )
        )
        return updatedUser
    }

    override fun getByUsername(username: String): User? {
        val entity = repository.findByUsernameAndDeletedAtIsNull(username)
        if (entity != null) return mapper.toModel(entity)
        return null
    }

    override fun existsByUsernameAndId(username: String, id: UUID): Boolean
        = repository.existsByUsernameAndIdNot(username, id)

    override fun searchByUsername(
        query: String,
        pageable: Pageable
    ): Page<User>
        = repository.searchByUsernameContainingIgnoreCase(
            query, pageable
        ).map { mapper.toModel(it) }

    override fun getById(id: UUID): User?
        = repository.findByIdAndDeletedAtIsNull(id)?.let { mapper.toModel(it) }

    override fun existsByIdNotDeleted(id: UUID): Boolean
        = repository.existsByIdAndDeletedAtIsNull(  id)

    @Transactional
    override fun createWithId(
        id: UUID,
        username: String,
        avatarUrl: String,
        bio: String,
        createdAt: Instant,
    ): User? {
        val inserted = repository.insertIfAbsent(id, username, avatarUrl, bio, createdAt)
        // 0 строк — профиль уже был создан ранее: событие дублирующее, outbox не трогаем
        if (inserted == 0) return null

        val newUser = mapper.toModel(
            repository.findById(id).orElseThrow { UserNotFoundException() }
        )
        outboxEventWriter.write(
            eventType = EventType.USER_CREATED,
            aggregateId = newUser.id.toString(),
            payload = UserCreatedPayload(
                userId = newUser.id,
                username = newUser.username,
                createdAt = newUser.createdAt
            )
        )
        return newUser
    }

    @Transactional
    override fun softDelete(id: UUID, deletedAt: Instant): Boolean {
        val updated = repository.softDeleteById(id, deletedAt)
        // 0 строк — пользователя нет либо он уже удалён: повторное событие игнорируем
        if (updated == 0) return false

        outboxEventWriter.write(
            eventType = EventType.USER_DELETED,
            aggregateId = id.toString(),
            payload = UserDeletedPayload(
                userId = id.toString(),
                deletedAt = deletedAt,
            )
        )
        return true
    }

    override fun isUsernameTaken(username: String): Boolean
        = repository.existsByUsername(username)

}