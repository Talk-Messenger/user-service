package com.user.user.application.use_case.me

import java.time.Instant
import java.util.UUID

data class GetMyInfoResult(
    val id: UUID,
    val username: String,
    val avatarUrl: String,
    val bio: String,
    val contactsCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)
