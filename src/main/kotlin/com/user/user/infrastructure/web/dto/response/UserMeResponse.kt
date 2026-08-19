package com.user.user.infrastructure.web.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "Профиль текущего пользователя")
data class UserMeResponse(
    @field:Schema(description = "Идентификатор пользователя")
    val id: UUID,

    @field:Schema(description = "Имя пользователя", example = "john_doe")
    val username: String,

    @field:Schema(description = "Ссылка на аватар")
    val avatarUrl: String,

    @field:Schema(description = "Описание профиля")
    val bio: String,

    @field:Schema(description = "Количество контактов", example = "12")
    val contactsCount: Int,

    @field:Schema(description = "Дата создания профиля")
    val createdAt: Instant,

    @field:Schema(description = "Дата последнего обновления профиля")
    val updatedAt: Instant,
)
