package com.user.contact.application.use_case.delete

import com.user.user.domain.model.User
import java.util.UUID

data class DeleteContactCommand(

    val id: UUID,
    val user: User,

    )
