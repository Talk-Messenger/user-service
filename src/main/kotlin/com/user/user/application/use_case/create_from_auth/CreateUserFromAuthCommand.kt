package com.user.user.application.use_case.create_from_auth

import java.time.Instant
import java.util.UUID

data class CreateUserFromAuthCommand(
    val userId: UUID,
    val username: String?,
    val createdAt: Instant?,
)
