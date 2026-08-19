package com.user.contact.application.use_case.delete

import com.user.contact.domain.repository.ContactRepository
import com.user.exceptions.contacts.YouAreNotOwnerException
import org.springframework.stereotype.Component

@Component
class DeleteContactUseCase(
    private val repository: ContactRepository
) {

    fun execute(command: DeleteContactCommand) {

        // Проверка владения
        if (!repository.isOwner(command.user.id, command.id))
            throw YouAreNotOwnerException()

        // Удаляем контакт
        repository.delete(command.id)

    }

}