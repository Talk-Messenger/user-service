package com.user.outboxevent.infrastructure.persistance.repository

import com.user.outboxevent.infrastructure.persistance.entity.OutboxEventJpaEntity
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*


interface OutboxEventJpaRepository : JpaRepository<OutboxEventJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OutboxEventJpaEntity o where o.processed = false order by o.createdAt asc")
    fun findBatchForDispatch(pageable: Pageable?): List<OutboxEventJpaEntity>

    @Modifying
    @Query("update OutboxEventJpaEntity o set o.processed = true where o.id = :id")
    fun markAsProcessed(@Param("id") id: UUID)

}