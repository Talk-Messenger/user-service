package com.user.contact.application.use_case.get

import com.user.contact.domain.model.Contact
import com.user.contact.domain.repository.ContactRepository
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class GetContactsUseCase(
    private val repository: ContactRepository
) {

    fun execute(command: GetContactsCommand): Page<Contact> {

        // Получаем список контактов
        return repository.getById(
            id = command.user.id,
            pageable = command.pageable
        )

    }

}