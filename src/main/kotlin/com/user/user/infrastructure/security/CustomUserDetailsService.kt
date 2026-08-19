package com.user.user.infrastructure.security

import com.user.exceptions.jwt.TokenUnauthorizedException
import com.user.user.domain.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CustomUserDetailsService(
    private val repository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        try {
            val user = repository.getById(
                UUID.fromString(username)
            )
            if (user != null) return user
            else throw TokenUnauthorizedException("Токен невалиден")
        } catch (_: IllegalArgumentException) {
            throw TokenUnauthorizedException("Токен невалиден")
        }
    }

}