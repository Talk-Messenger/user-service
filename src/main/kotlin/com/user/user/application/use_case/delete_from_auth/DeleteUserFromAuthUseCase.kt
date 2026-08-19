package com.user.user.application.use_case.delete_from_auth

import com.user.contact.domain.repository.ContactRepository
import com.user.user.domain.repository.UserRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class DeleteUserFromAuthUseCase(
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository,
    private val clock: Clock,
) {

    private val logger = KotlinLogging.logger {}

    @Transactional
    fun execute(command: DeleteUserFromAuthCommand) {
        val deletedAt = command.deletedAt ?: Instant.now(clock)

        val removedContacts = contactRepository.deleteAllRelatedToUser(command.userId)

        val deleted = userRepository.softDelete(command.userId, deletedAt)
        if (!deleted) {
            logger.info(
                "AuthUserDeleted userId={} — пользователь отсутствует или уже удалён, " +
                    "событие UserDeleted не публикуем (удалено контактов: {})",
                command.userId, removedContacts
            )
            return
        }

        logger.info(
            "Профиль userId={} помечен удалённым, каскадно удалено контактов: {}",
            command.userId, removedContacts
        )
    }

}
