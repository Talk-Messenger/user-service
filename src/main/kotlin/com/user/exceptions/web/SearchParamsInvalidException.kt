package com.user.exceptions.web

import com.user.exceptions.basic.BasicException
import org.springframework.http.HttpStatus

data class SearchParamsInvalidException (
    override val message: String = "Невалидные параметры запроса",
    override val status: HttpStatus = HttpStatus.BAD_REQUEST
): BasicException(message, status)