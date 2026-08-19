package com.user.user.infrastructure.payloads

import java.time.Instant
import java.util.UUID

data class UserCreatedPayload(
    val userId: UUID,
    val username: String,
    val createdAt: Instant,
)
