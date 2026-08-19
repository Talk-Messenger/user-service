package com.user.user.application.use_case.update

import com.user.exceptions.user.UsernameAlreadyExistsException
import com.user.user.domain.model.User
import com.user.user.domain.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class UpdateUserUseCase(
    private val repository: UserRepository
) {

    fun execute(command: UpdateUserCommand): User {
        // Проверка уникальности username
        if (command.user.username != command.username && repository.getByUsername(command.username) != null)
            throw UsernameAlreadyExistsException()

        // Обновляем пользователя и получаем обновлённый вариант обратно
        return repository.update(
            command.user.id,
            command.username,
            command.avatarUrl,
            command.bio,
        )

    }

}