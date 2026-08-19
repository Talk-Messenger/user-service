package com.user.user.application.use_case.search

import com.user.user.domain.model.User
import com.user.user.domain.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class SearchUserUseCase(
    private val repository: UserRepository
) {

    fun execute(command: SearchUserCommand): Page<User>
        = repository.searchByUsername(
            command.query,
            command.pageable
        )

}