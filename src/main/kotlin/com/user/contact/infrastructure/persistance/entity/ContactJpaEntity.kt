package com.user.contact.infrastructure.persistance.entity

import com.user.user.infrastructure.persistance.entity.UserJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "contacts")
data class ContactJpaEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user_id")
    val userId: UUID,

    @Column(name = "contact_user_id")
    val contactUserId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    val user: UserJpaEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_user_id", insertable = false, updatable = false)
    val contact: UserJpaEntity? = null,

    @CreationTimestamp
    val createdAt: Instant,

)
