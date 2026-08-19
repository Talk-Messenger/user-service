package com.user.contact.application.use_case.create

import com.user.contact.domain.model.Contact
import com.user.contact.domain.repository.ContactRepository
import com.user.exceptions.contacts.CannotCreateContactYourselfException
import com.user.exceptions.contacts.SuchContactAlreadyExistsException
import com.user.exceptions.user.UserNotFoundException
import com.user.user.domain.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class CreateContactUseCase(
    private val repository: ContactRepository,
    private val userRepository: UserRepository
) {

    fun execute(command: CreateContactCommand): Contact {

        // Проверка на контакт с самим собой
        if (command.contactUserId == command.user.id)
            throw CannotCreateContactYourselfException()

        // Проверка существования пользователя
        val contactUser = userRepository.getById(command.contactUserId)
            ?: throw UserNotFoundException()

        // Проверка на существование такого контакта
        if (repository.existsByIds(command.user.id, command.contactUserId))
            throw SuchContactAlreadyExistsException()

        // Создаём контакт и возвращаем его
        return repository.save(
            user = command.user,
            contactUser = contactUser,
        )

    }

}