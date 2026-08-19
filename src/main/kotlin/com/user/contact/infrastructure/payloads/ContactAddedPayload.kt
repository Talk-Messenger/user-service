package com.user.contact.infrastructure.payloads

import java.time.Instant
import java.util.UUID

data class ContactAddedPayload(
    val id: UUID,
    val userId: UUID,
    val contactId: UUID,
    val addedAt: Instant
)
