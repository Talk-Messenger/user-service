package com.user.outboxevent.domain.writer

import com.user.outboxevent.domain.event.EventType

interface OutboxEventWriter {
    fun <T> write(eventType: EventType, aggregateId: String, payload: T)
}