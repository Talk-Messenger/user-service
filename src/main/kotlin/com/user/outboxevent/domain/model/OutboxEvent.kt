package com.user.outboxevent.domain.model

import java.time.Instant
import java.util.UUID

data class OutboxEvent(

    val id: UUID,
    val aggregateId: String,
    val eventType: String,
    val payload: String,
    val createdAt: Instant,
    val processed: Boolean,

)
