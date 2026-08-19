package com.user.user.infrastructure.persistance.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
data class UserJpaEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(length = 20)
    val username: String,

    val avatarUrl: String,

    val bio: String,

    @CreationTimestamp
    val createdAt: Instant,

    @UpdateTimestamp
    val updatedAt: Instant,

    val deletedAt: Instant? = null,

)
