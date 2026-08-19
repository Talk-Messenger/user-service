package com.user.user.infrastructure.web.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "Данные для обновления профиля")
data class UpdateUserProfileRequest(

    @field:Schema(
        description = "Имя пользователя: латиница, цифры и подчёркивание",
        example = "john_doe",
        minLength = 3,
        maxLength = 20,
    )
    @NotBlank
    @Size(min = 3, max = 20)
    @Pattern(regexp = "[a-zA-Z0-9_]+\$")
    val username: String,

    @field:Schema(description = "Ссылка на аватар", maxLength = 2048)
    @Size(max = 2048)
    val avatarUrl: String,

    @field:Schema(description = "Описание профиля", maxLength = 500)
    @Size(max = 500)
    val bio: String,

)
