package com.user.configs

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.util.backoff.FixedBackOff
import com.user.user.infrastructure.messaging.exception.InvalidEventException
import mu.KotlinLogging
import org.apache.kafka.common.TopicPartition

/**
 * Консьюмеры читают сырую строку (StringDeserializer) — разбор JSON выполняется вручную
 * внутри слушателя. Так ошибка формата становится обычным исключением приложения,
 * которое можно осознанно отправить в dead-letter топик, а не падением на уровне сети.
 */
@Configuration
class KafkaConsumerConfig {

    private val logger = KotlinLogging.logger {}

    @Bean
    fun consumerFactory(
        @Value("\${spring.kafka.bootstrap-servers}")
        bootstrapServers: String,
        @Value("\${spring.kafka.consumer.group-id}")
        groupId: String,
        @Value("\${spring.kafka.consumer.auto-offset-reset:earliest}")
        autoOffsetReset: String,
    ): ConsumerFactory<String, String> {
        val props: Map<String, Any> = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to autoOffsetReset,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            // Оффсет коммитим сами после успешной обработки записи — «at least once»,
            // дубликаты гасятся идемпотентностью обработчиков.
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 50,
        )
        return DefaultKafkaConsumerFactory(props)
    }

    /**
     * Невалидные события (битый JSON, отсутствующий userId) ретраить бессмысленно —
     * они сразу уходят в <topic>.DLT. Транзиентные сбои (БД недоступна) ретраятся.
     */
    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, String>,
        kafkaTemplate: KafkaTemplate<String, String>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.setConsumerFactory(consumerFactory)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        factory.setCommonErrorHandler(errorHandler(kafkaTemplate))
        return factory
    }

    private fun errorHandler(kafkaTemplate: KafkaTemplate<String, String>): DefaultErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate) { record, exception ->
            logger.error(
                "Событие topic={} partition={} offset={} отправлено в DLT: {}",
                record.topic(), record.partition(), record.offset(), exception.message, exception
            )
            // partition = -1: брокер сам выберет партицию в DLT-топике
            TopicPartition("${record.topic()}.DLT", -1)
        }
        // 3 попытки с паузой 1с, затем DLT
        val handler = DefaultErrorHandler(recoverer, FixedBackOff(1000L, 2L))
        handler.addNotRetryableExceptions(InvalidEventException::class.java)
        return handler
    }

}
