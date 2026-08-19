package com.user.user.infrastructure.payloads

import java.time.Instant
import java.util.UUID

data class UserUpdatedPayload(
    val userId: UUID,
    val displayName: String,
    val avatarUrl: String,
    val updatedAt: Instant,
)
