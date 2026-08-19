package com.user.user.domain.model

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    @get:JvmName("getUsernameProperty")
    val username: String,
    val avatarUrl: String,
    val bio: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> = listOf()
    override fun getPassword(): String = ""
    override fun getUsername(): String = username
    override fun isEnabled(): Boolean = true
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true

}
