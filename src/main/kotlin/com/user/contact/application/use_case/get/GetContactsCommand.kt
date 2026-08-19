package com.user.contact.application.use_case.get

import com.user.user.domain.model.User
import org.springframework.data.domain.Pageable

data class GetContactsCommand (
    val user: User,
    val pageable: Pageable,
)