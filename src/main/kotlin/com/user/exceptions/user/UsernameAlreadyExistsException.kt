package com.user.exceptions.user

import com.user.exceptions.basic.BasicException
import org.springframework.http.HttpStatus

data class UsernameAlreadyExistsException (
    override val message: String = "Имя пользователя занято",
    override val status: HttpStatus = HttpStatus.CONFLICT
): BasicException(message, status)