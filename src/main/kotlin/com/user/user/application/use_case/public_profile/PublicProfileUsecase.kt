package com.user.user.application.use_case.public_profile

import com.user.exceptions.user.UserNotFoundException
import com.user.user.domain.model.User
import com.user.user.domain.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class PublicProfileUseCase(
    private val repository: UserRepository,
) {

    fun execute(command: PublicProfileCommand): User
        = repository.getByUsername(command.username) ?: throw UserNotFoundException()

}