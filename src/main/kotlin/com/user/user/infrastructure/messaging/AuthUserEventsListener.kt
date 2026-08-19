package com.user.user.infrastructure.messaging

import com.user.user.application.use_case.create_from_auth.CreateUserFromAuthCommand
import com.user.user.application.use_case.create_from_auth.CreateUserFromAuthUseCase
import com.user.user.application.use_case.delete_from_auth.DeleteUserFromAuthCommand
import com.user.user.application.use_case.delete_from_auth.DeleteUserFromAuthUseCase
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class AuthUserEventsListener(
    private val parser: AuthEventParser,
    private val createUserFromAuthUseCase: CreateUserFromAuthUseCase,
    private val deleteUserFromAuthUseCase: DeleteUserFromAuthUseCase,
) {

    private val logger = KotlinLogging.logger {}

    @KafkaListener(
        topics = [$$"${kafka.topics.auth-user-created:auth.user.created}"],
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun onAuthUserCreated(
        @Payload(required = false) raw: String?,
        @Header(KafkaHeaders.RECEIVED_KEY, required = false) key: String?,
    ) {
        logger.debug("Получено AuthUserCreated key={}", key)
        val event = parser.parseCreated(raw)
        createUserFromAuthUseCase.execute(
            CreateUserFromAuthCommand(
                userId = event.userId,
                username = event.username,
                createdAt = event.createdAt,
            )
        )
    }

    @KafkaListener(
        topics = [$$"${kafka.topics.auth-user-deleted:auth.user.deleted}"],
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun onAuthUserDeleted(
        @Payload(required = false) raw: String?,
        @Header(KafkaHeaders.RECEIVED_KEY, required = false) key: String?,
    ) {
        logger.debug("Получено AuthUserDeleted key={}", key)
        val event = parser.parseDeleted(raw)
        deleteUserFromAuthUseCase.execute(
            DeleteUserFromAuthCommand(
                userId = event.userId,
                deletedAt = event.deletedAt,
            )
        )
    }

}
