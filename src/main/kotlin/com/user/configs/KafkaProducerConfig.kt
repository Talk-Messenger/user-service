package com.user.configs

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory


@Configuration
class KafkaProducerConfig {

    @Bean
    fun producerFactory(
        @Value("\${spring.kafka.bootstrap-servers}")
        bootstrapServers: String
    ): ProducerFactory<String, String> {
        val props: Map<String, Any> = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true
        )
        return DefaultKafkaProducerFactory(props)
    }

    @Bean
    fun kafkaTemplate(pf: ProducerFactory<String, String>): KafkaTemplate<String, String> {
        return KafkaTemplate(pf)
    }

}