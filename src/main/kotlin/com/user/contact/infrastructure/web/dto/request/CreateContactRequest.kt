package com.user.contact.infrastructure.web.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Schema(description = "Данные для создания контакта")
data class CreateContactRequest (

    @field:Schema(description = "Идентификатор пользователя, добавляемого в контакты")
    @NotNull
    val contactUserId: UUID

)
