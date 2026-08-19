package com.user.user.infrastructure.messaging.event

import java.time.Instant
import java.util.UUID

data class AuthUserCreatedEvent(
    val userId: UUID,
    val username: String? = null,
    val createdAt: Instant? = null,
)
