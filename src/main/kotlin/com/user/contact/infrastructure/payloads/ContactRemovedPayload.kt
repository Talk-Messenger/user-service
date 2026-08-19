package com.user.contact.infrastructure.payloads

import java.time.Instant

data class ContactRemovedPayload(
    val userId: String,
    val contactId: String,
    val deletedAt: Instant
)
