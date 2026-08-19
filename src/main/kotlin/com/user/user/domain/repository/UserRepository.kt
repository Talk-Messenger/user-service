package com.user.user.domain.repository

import com.user.user.domain.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.Instant
import java.util.UUID

interface UserRepository {

    fun save(
        username: String,
        avatarUrl: String,
        bio: String,
    ): User

    fun update(
        id: UUID,
        username: String,
        avatarUrl: String,
        bio: String,
    ): User

    fun getByUsername(username: String): User?

    fun existsByUsernameAndId(username: String, id: UUID): Boolean

    fun searchByUsername(query: String, pageable: Pageable): Page<User>

    fun getById(id: UUID): User?

    fun existsByIdNotDeleted(id: UUID): Boolean

    /**
     * Создаёт профиль с идентификатором, полученным из события AuthService.
     * Возвращает null, если профиль с таким id уже существует (событие уже обработано).
     */
    fun createWithId(
        id: UUID,
        username: String,
        avatarUrl: String,
        bio: String,
        createdAt: Instant,
    ): User?

    /**
     * Помечает пользователя удалённым. Возвращает false, если пользователя нет
     * или он уже был удалён ранее (повторная доставка события).
     */
    fun softDelete(id: UUID, deletedAt: Instant): Boolean

    fun isUsernameTaken(username: String): Boolean

}