package com.user.outboxevent.domain.event

enum class EventType(
    val code: String,
    val topic: String,
) {
    USER_CREATED("UserCreated", "user.created"),
    USER_UPDATED("UserUpdated", "user.updated"),
    USER_DELETED("UserDeleted", "user.deleted"),
    CONTACT_ADDED("ContactAdded", "contact.added"),
    CONTACT_REMOVED("ContactRemoved", "contact.removed");

    companion object {
        fun fromCode(code: String): EventType {
            for (t in values()) {
                if (t.code == code) return t
            }
            throw IllegalArgumentException("Неизвестный тип события: " + code)
        }
    }

}