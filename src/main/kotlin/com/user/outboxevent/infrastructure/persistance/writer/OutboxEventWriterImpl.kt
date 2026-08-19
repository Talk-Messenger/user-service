package com.user.outboxevent.infrastructure.persistance.writer

import com.user.outboxevent.domain.event.EventEnvelope
import com.user.outboxevent.domain.event.EventType
import com.user.outboxevent.domain.writer.OutboxEventWriter
import com.user.outboxevent.infrastructure.persistance.entity.OutboxEventJpaEntity
import com.user.outboxevent.infrastructure.persistance.repository.OutboxEventJpaRepository
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Instant


@Component
class OutboxEventWriterImpl(
    private val repository: OutboxEventJpaRepository,
    private val objectMapper: ObjectMapper,
): OutboxEventWriter {

    override fun <T> write(eventType: EventType, aggregateId: String, payload: T) {
        val envelope: EventEnvelope<T?> = EventEnvelope.of(eventType, aggregateId, payload)
        try {
            val json: String = objectMapper.writeValueAsString(envelope)
            val entity = OutboxEventJpaEntity(
                envelope.id,
                aggregateId,
                eventType.code,
                payload = json,
                createdAt = Instant.now(),
                processed = false
            )
            repository.save<OutboxEventJpaEntity>(entity)
        } catch (e: Exception) {
            throw IllegalStateException("Невозможно отправить событие: $eventType", e)
        }
    }

}