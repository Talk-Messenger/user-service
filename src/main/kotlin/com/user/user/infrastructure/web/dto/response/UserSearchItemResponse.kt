package com.user.user.infrastructure.web.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Пользователь в результатах поиска")
data class UserSearchItemResponse(

    @field:Schema(description = "Идентификатор пользователя")
    val id: UUID,

    @field:Schema(description = "Имя пользователя", example = "john_doe")
    val username: String,

    @field:Schema(description = "Ссылка на аватар")
    val avatarUrl: String,

)
