package com.user.outboxevent.domain.event

import java.time.Instant
import java.util.*

data class EventEnvelope<T> (

    val id: UUID,

    val eventType: EventType,

    val aggregateId: String?,

    val payload: T,

    val createdAt: Instant,

) {

    companion object {
        fun <T> of(type: EventType, aggregateId: String?, payload: T?): EventEnvelope<T?> {
            return EventEnvelope(
                UUID.randomUUID(),
                type,
                aggregateId,
                payload,
                Instant.now()
            )
        }
    }

}

