package com.user.exceptions.contacts

import com.user.exceptions.basic.BasicException
import org.springframework.http.HttpStatus

data class SuchContactAlreadyExistsException(
    override val message: String = "Такой контакт уже существует",
    override val status: HttpStatus = HttpStatus.CONFLICT
): BasicException(message, status)
