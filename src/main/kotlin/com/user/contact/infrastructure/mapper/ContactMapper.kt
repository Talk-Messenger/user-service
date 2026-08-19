package com.user.contact.infrastructure.mapper

import com.user.contact.domain.model.Contact
import com.user.contact.infrastructure.persistance.entity.ContactJpaEntity
import com.user.contact.infrastructure.web.dto.response.ContactResponse
import com.user.user.infrastructure.mapper.UserMapper
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy

@Mapper(componentModel = "spring", uses = [UserMapper::class], unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ContactMapper {

    @Mapping(source = "user", target = "user")
    @Mapping(source = "contact", target = "contactUser")
    fun toModel(entity: ContactJpaEntity): Contact

    @Mapping(source = "id", target = "contactId")
    fun toResponse(model: Contact): ContactResponse

}