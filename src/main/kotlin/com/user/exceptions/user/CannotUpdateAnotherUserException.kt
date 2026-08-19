package com.user.exceptions.user

import com.user.exceptions.basic.BasicException
import org.springframework.http.HttpStatus

data class CannotUpdateAnotherUserException(
    override val message: String = "Вы можете обновить только данные о себе",
    override val status: HttpStatus = HttpStatus.FORBIDDEN
): BasicException(message, status)
