package com.user.contact.infrastructure.web.dto.response

import com.user.user.infrastructure.web.dto.response.UserSearchItemResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "Контакт между двумя пользователями")
data class ContactResponse(

    @field:Schema(description = "Идентификатор контакта")
    val contactId: UUID,

    @field:Schema(description = "Владелец контакта")
    val user: UserSearchItemResponse,

    @field:Schema(description = "Пользователь, добавленный в контакты")
    val contactUser: UserSearchItemResponse,

    @field:Schema(description = "Дата создания контакта")
    val createdAt: Instant,

)
