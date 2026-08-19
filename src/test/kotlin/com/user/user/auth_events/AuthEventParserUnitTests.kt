package com.user.user.auth_events

import com.user.user.infrastructure.messaging.AuthEventParser
import com.user.user.infrastructure.messaging.exception.InvalidEventException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("Разбор событий AuthService | Unit")
class AuthEventParserUnitTests {

    private val parser = AuthEventParser(ObjectMapper())

    @Test
    @DisplayName("Успех — полный payload AuthUserCreated")
    fun success_parses_full_created_payload() {
        val id = UUID.randomUUID()

        val event = parser.parseCreated(
            """{"userId":"$id","username":"alice","createdAt":"2026-01-01T10:00:00Z"}"""
        )

        assertEquals(id, event.userId)
        assertEquals("alice", event.username)
        assertEquals(Instant.parse("2026-01-01T10:00:00Z"), event.createdAt)
    }

    @Test
    @DisplayName("Успех — payload в конверте и синонимы полей")
    fun success_parses_envelope_and_field_aliases() {
        val id = UUID.randomUUID()

        val enveloped = parser.parseCreated(
            """{"eventType":"AuthUserCreated","payload":{"user_id":"$id","login":"bob"}}"""
        )
        assertEquals(id, enveloped.userId)
        assertEquals("bob", enveloped.username)

        val byId = parser.parseDeleted("""{"id":"$id"}""")
        assertEquals(id, byId.userId)
    }

    @Test
    @DisplayName("Успех — необязательные поля отсутствуют")
    fun success_optional_fields_are_nullable() {
        val id = UUID.randomUUID()

        val created = parser.parseCreated("""{"userId":"$id"}""")

        assertNull(created.username)
        assertNull(created.createdAt)
    }

    @Test
    @DisplayName("Успех — некорректная дата не ломает разбор")
    fun success_broken_timestamp_is_ignored() {
        val id = UUID.randomUUID()

        val deleted = parser.parseDeleted("""{"userId":"$id","deletedAt":"вчера"}""")

        assertEquals(id, deleted.userId)
        assertNull(deleted.deletedAt)
    }

    @Test
    @DisplayName("Ошибка — битый JSON")
    fun failure_broken_json() {
        assertThrows<InvalidEventException> { parser.parseCreated("{ не json") }
    }

    @Test
    @DisplayName("Ошибка — пустое тело")
    fun failure_empty_body() {
        assertThrows<InvalidEventException> { parser.parseCreated(null) }
        assertThrows<InvalidEventException> { parser.parseDeleted("   ") }
    }

    @Test
    @DisplayName("Ошибка — отсутствует userId")
    fun failure_missing_user_id() {
        assertThrows<InvalidEventException> { parser.parseCreated("""{"username":"alice"}""") }
    }

    @Test
    @DisplayName("Ошибка — userId не UUID")
    fun failure_user_id_is_not_uuid() {
        assertThrows<InvalidEventException> { parser.parseDeleted("""{"userId":"12345"}""") }
    }

    @Test
    @DisplayName("Ошибка — событие не объект")
    fun failure_not_an_object() {
        assertThrows<InvalidEventException> { parser.parseCreated("[1,2,3]") }
    }

}
