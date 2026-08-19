package com.user.user.application.use_case.create_from_auth

import com.user.user.domain.repository.UserRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class CreateUserFromAuthUseCase(
    private val userRepository: UserRepository,
    private val clock: Clock,
) {

    private val logger = KotlinLogging.logger {}

    @Transactional
    fun execute(command: CreateUserFromAuthCommand) {
        val createdAt = command.createdAt ?: Instant.now(clock)
        val username = resolveUsername(command.username, command.userId)

        val created = userRepository.createWithId(
            id = command.userId,
            username = username,
            avatarUrl = "",
            bio = "",
            createdAt = createdAt,
        )

        if (created == null) {
            logger.info("AuthUserCreated userId={} уже обработан ранее — пропускаем", command.userId)
            return
        }
        logger.info("Профиль userId={} создан по событию AuthUserCreated", created.id)
    }

    private fun resolveUsername(candidate: String?, userId: UUID): String {
        val fallback = fallbackUsername(userId)
        val normalized = candidate?.trim()?.takeIf { it.isNotEmpty() } ?: return fallback
        val trimmed = normalized.take(USERNAME_MAX_LENGTH)
        return if (userRepository.isUsernameTaken(trimmed)) fallback else trimmed
    }

    private fun fallbackUsername(userId: UUID): String =
        "user_" + userId.toString().replace("-", "").take(FALLBACK_SUFFIX_LENGTH)

    private companion object {
        // соответствует @Column(length = 20) в UserJpaEntity
        const val USERNAME_MAX_LENGTH = 20
        const val FALLBACK_SUFFIX_LENGTH = 12
    }

}
