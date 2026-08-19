package com.user.exceptions.jwt

import com.user.exceptions.basic.BasicException
import org.springframework.http.HttpStatus

data class TokenUnauthorizedException(
    override val message: String = "Токен авторизации не авторизован",
    override val status: HttpStatus = HttpStatus.UNAUTHORIZED
): BasicException(message, status)
