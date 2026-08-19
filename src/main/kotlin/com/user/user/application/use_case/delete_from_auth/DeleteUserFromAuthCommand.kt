package com.user.user.application.use_case.delete_from_auth

import java.time.Instant
import java.util.UUID

data class DeleteUserFromAuthCommand(
    val userId: UUID,
    val deletedAt: Instant?,
)
