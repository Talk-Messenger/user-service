package com.user.contact.application.use_case.create

import com.user.user.domain.model.User
import java.util.UUID

data class CreateContactCommand(

    val user: User,

    val contactUserId: UUID

)
