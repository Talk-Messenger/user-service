package com.user.user.infrastructure.mapper

import com.user.user.domain.model.User
import com.user.user.infrastructure.persistance.entity.UserJpaEntity
import com.user.user.infrastructure.web.dto.response.UserMeResponse
import com.user.user.infrastructure.web.dto.response.UserPublicProfileResponse
import com.user.user.infrastructure.web.dto.response.UserSearchItemResponse
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface UserMapper {

    fun toModel(entity: UserJpaEntity): User

    fun toMeResponse(model: User): UserMeResponse

    fun toPublicProfileResponse(model: User): UserPublicProfileResponse

    fun toSearchItemResponse(model: User): UserSearchItemResponse

}