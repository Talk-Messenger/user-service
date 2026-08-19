package com.user.user.infrastructure.persistance.repository

import com.user.user.infrastructure.persistance.entity.UserJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.Optional
import java.util.UUID

interface UserJpaRepository : JpaRepository<UserJpaEntity, UUID> {

    fun findByUsernameAndDeletedAtIsNull(username: String): UserJpaEntity?

    fun existsByUsernameAndIdNot(username: String, id: UUID): Boolean

    fun searchByUsernameContainingIgnoreCase(query: String, page: Pageable): Page<UserJpaEntity>

    fun findByIdAndDeletedAtIsNull(id: UUID): UserJpaEntity?

    fun existsByIdAndDeletedAtIsNull(id: UUID): Boolean

    fun existsByUsername(username: String): Boolean

    /**
     * Идемпотентная вставка профиля с заранее известным id (id приходит из события AuthService).
     * ON CONFLICT DO NOTHING перекладывает проверку дубликата на БД: гонки двух консьюмеров
     * разрешаются атомарно, повторная доставка события возвращает 0 обновлённых строк.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            INSERT INTO users (id, username, avatar_url, bio, created_at, updated_at, deleted_at)
            VALUES (:id, :username, :avatarUrl, :bio, :createdAt, :createdAt, NULL)
            ON CONFLICT (id) DO NOTHING
        """,
        nativeQuery = true
    )
    fun insertIfAbsent(
        @Param("id") id: UUID,
        @Param("username") username: String,
        @Param("avatarUrl") avatarUrl: String,
        @Param("bio") bio: String,
        @Param("createdAt") createdAt: Instant,
    ): Int

    /**
     * Возвращает 1 только при первом удалении: условие deleted_at IS NULL делает
     * повторную доставку события безопасным no-op.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserJpaEntity u set u.deletedAt = :now, u.updatedAt = :now where u.id = :id and u.deletedAt is null")
    fun softDeleteById(@Param("id") id: UUID, @Param("now") now: Instant): Int

}