package com.user.user.infrastructure.web.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "Публичный профиль пользователя")
data class UserPublicProfileResponse(
    @field:Schema(description = "Идентификатор пользователя")
    val id: UUID,

    @field:Schema(description = "Ссылка на аватар")
    val avatarUrl: String,

    @field:Schema(description = "Описание профиля")
    val bio: String,

    @field:Schema(description = "Дата создания профиля")
    val createdAt: Instant,
)
