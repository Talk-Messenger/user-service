package com.user.exceptions.user

import com.user.exceptions.basic.BasicException
import org.springframework.http.HttpStatus

data class UserNotFoundException(
    override val message: String = "Пользователь не найден",
    override val status: HttpStatus = HttpStatus.NOT_FOUND,
): BasicException(message, status)
