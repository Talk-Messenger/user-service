package com.user.contact.domain.model

import com.user.user.domain.model.User
import java.time.Instant
import java.util.UUID

data class Contact(

    val id: UUID?,

    val user: User,

    val contactUser: User,

    val createdAt: Instant,

)
