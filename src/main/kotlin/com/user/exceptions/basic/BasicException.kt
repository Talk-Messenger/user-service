package com.user.exceptions.basic

import org.springframework.http.HttpStatus

open class BasicException(
    override val message: String,
    open val status: HttpStatus
): RuntimeException(message)