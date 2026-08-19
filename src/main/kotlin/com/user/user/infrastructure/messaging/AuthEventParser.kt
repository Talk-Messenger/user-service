package com.user.user.infrastructure.messaging

import com.user.user.infrastructure.messaging.event.AuthUserCreatedEvent
import com.user.user.infrastructure.messaging.event.AuthUserDeletedEvent
import com.user.user.infrastructure.messaging.exception.InvalidEventException
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

@Component
class AuthEventParser(
    private val objectMapper: ObjectMapper,
) {

    fun parseCreated(raw: String?): AuthUserCreatedEvent {
        val node = payloadNode(raw)
        return AuthUserCreatedEvent(
            userId = requireUserId(node),
            username = text(node, "username", "login", "displayName"),
            createdAt = instant(node, "createdAt", "created_at", "occurredAt"),
        )
    }

    fun parseDeleted(raw: String?): AuthUserDeletedEvent {
        val node = payloadNode(raw)
        return AuthUserDeletedEvent(
            userId = requireUserId(node),
            deletedAt = instant(node, "deletedAt", "deleted_at", "occurredAt"),
        )
    }

    private fun payloadNode(raw: String?): JsonNode {
        if (raw.isNullOrBlank()) throw InvalidEventException("Пустое тело события")
        val root = try {
            objectMapper.readTree(raw)
        } catch (e: Exception) {
            throw InvalidEventException("Невалидный JSON в событии", e)
        }
        if (!root.isObject) throw InvalidEventException("Событие не является JSON-объектом")

        val payload = root.get("payload")
        return if (payload != null && payload.isObject) payload else root
    }

    private fun requireUserId(node: JsonNode): UUID {
        val raw = text(node, "userId", "user_id", "id")
            ?: throw InvalidEventException("В событии отсутствует userId")
        return try {
            UUID.fromString(raw)
        } catch (e: IllegalArgumentException) {
            throw InvalidEventException("userId не является UUID: $raw", e)
        }
    }

    private fun text(node: JsonNode, vararg names: String): String? {
        for (name in names) {
            val value = node.get(name)
            if (value != null && !value.isNull) {
                val text = value.asString()
                if (text.isNotBlank()) return text
            }
        }
        return null
    }

    private fun instant(node: JsonNode, vararg names: String): Instant? {
        val raw = text(node, *names) ?: return null
        return try {
            Instant.parse(raw)
        } catch (e: Exception) {
            // Время — не критичное поле: подставим now() на уровне use case
            null
        }
    }

}
