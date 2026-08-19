package com.user.user.application.use_case.update

import com.user.user.domain.model.User
import java.util.UUID

data class UpdateUserCommand (

    val userId: UUID,

    val user: User,

    val username: String,
    val avatarUrl: String,
    val bio: String,

    )