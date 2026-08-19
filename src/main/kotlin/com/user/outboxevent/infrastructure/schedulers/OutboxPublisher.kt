package com.user.outboxevent.infrastructure.schedulers

import com.user.outboxevent.domain.event.EventType
import com.user.outboxevent.infrastructure.persistance.entity.OutboxEventJpaEntity
import com.user.outboxevent.infrastructure.persistance.repository.OutboxEventJpaRepository
import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


@Component
class OutboxPublisher(
    val repository: OutboxEventJpaRepository,
    val kafkaTemplate: KafkaTemplate<String, String>,
) {

    private val logger = KotlinLogging.logger {}
    private final val batchSize = 100

    @Scheduled(fixedDelayString = $$"${outbox.publisher.poll-interval-ms:1000}")
    @Transactional
    fun publishPending() {
        val batch = repository.findBatchForDispatch(PageRequest.of(0, batchSize))
        if (batch.isEmpty()) return
        for (entity in batch) {
            try {
                publishOne(entity)
                repository.markAsProcessed(entity.id)
            } catch (e: Exception) {
                logger.error("Failed to publish outbox event {} ({}): {}",
                    entity.id, entity.eventType, e.message, e);
            }
        }
    }

    @Throws(Exception::class)
    private fun publishOne(entity: OutboxEventJpaEntity) {
        val eventType: EventType = EventType.fromCode(entity.eventType)

        val message = MessageBuilder
            .withPayload(entity.payload)
            .setHeader(KafkaHeaders.TOPIC, eventType.topic)
            .setHeader(KafkaHeaders.KEY, entity.aggregateId)
            .setHeader("eventType", entity.eventType)
            .setHeader("eventId", entity.id)
            .build()

        // send() синхронно ждём через .get(), чтобы гарантированно знать успех до markProcessed
        kafkaTemplate.send(message).get()
    }

}