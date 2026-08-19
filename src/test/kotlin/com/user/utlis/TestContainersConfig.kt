package com.user.utlis

import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

object TestContainersConfig {

    @Container
    @JvmStatic
    @ServiceConnection
    val POSTGRES: PostgreSQLContainer = PostgreSQLContainer(
        DockerImageName.parse("postgres:16.3-alpine")
    ).apply {
        withDatabaseName("admin")
        withUsername("admin")
        withPassword("admin")
        withExposedPorts(5432)
    }

    @Container
    @JvmStatic
    @ServiceConnection
    val REDIS: GenericContainer<*> = GenericContainer(
        DockerImageName.parse("redis:7")
    ).apply {
        withExposedPorts(6379)
    }

    @Container
    @JvmStatic
    @ServiceConnection
    val KAFKA: KafkaContainer = KafkaContainer(
        DockerImageName.parse("apache/kafka-native:latest")
    )

}