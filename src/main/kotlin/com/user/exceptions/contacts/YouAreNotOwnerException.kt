package com.user.exceptions.contacts

import com.user.exceptions.basic.BasicException
import org.springframework.http.HttpStatus

data class YouAreNotOwnerException(
    override val message: String = "Можно удалять только свои контакты",
    override val status: HttpStatus = HttpStatus.FORBIDDEN,
): BasicException(message, status)
