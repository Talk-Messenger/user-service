package com.user.user.infrastructure.messaging.event

import java.time.Instant
import java.util.UUID

data class AuthUserDeletedEvent(
    val userId: UUID,
    val deletedAt: Instant? = null,
)
