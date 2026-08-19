package com.user.user.application.use_case.me

import com.user.contact.domain.repository.ContactRepository
import com.user.exceptions.user.UserNotFoundException
import com.user.user.domain.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class GetMyInfoUseCase(
    private val repository: UserRepository,
    private val contactRepository: ContactRepository
) {

    fun execute(command: GetMyInfoCommand): GetMyInfoResult {
        val user = repository.getById(command.id) ?: throw UserNotFoundException()
        val contactsAmount = contactRepository.contactsCount(user.id)
        return GetMyInfoResult(
            id = user.id,
            username = user.username,
            avatarUrl = user.avatarUrl,
            bio = user.bio,
            contactsCount = contactsAmount.toInt(),
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
        )
    }

}