package com.user.user.infrastructure.payloads

import java.time.Instant

data class UserDeletedPayload(
    val userId: String,
    val deletedAt: Instant
)
