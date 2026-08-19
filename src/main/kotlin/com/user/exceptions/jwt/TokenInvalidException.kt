package com.user.exceptions.jwt

import com.user.exceptions.basic.BasicException
import org.springframework.http.HttpStatus

data class TokenInvalidException(
    override val message: String = "Токен авторизации невалиден",
    override val status: HttpStatus = HttpStatus.UNAUTHORIZED
): BasicException(message, status)
