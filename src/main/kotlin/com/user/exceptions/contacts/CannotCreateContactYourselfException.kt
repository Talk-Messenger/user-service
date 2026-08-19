package com.user.exceptions.contacts

import com.user.exceptions.basic.BasicException
import org.springframework.http.HttpStatus

data class CannotCreateContactYourselfException (
    override val message: String = "Невозможно создать контакт с самим собой",
    override val status: HttpStatus = HttpStatus.BAD_REQUEST
): BasicException(message, status)