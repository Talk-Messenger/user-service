package com.user.exceptions.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus

@Schema(description = "Стандартный ответ об ошибке")
data class ErrorDto(
    @field:Schema(description = "Описание ошибки", example = "Пользователь не найден")
    val message: String,

    @field:Schema(description = "HTTP-статус ошибки", example = "NOT_FOUND")
    val status: HttpStatus,
)
