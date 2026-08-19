package com.user.contact.infrastructure.web

import com.user.contact.application.use_case.create.CreateContactCommand
import com.user.contact.application.use_case.create.CreateContactUseCase
import com.user.contact.application.use_case.delete.DeleteContactCommand
import com.user.contact.application.use_case.delete.DeleteContactUseCase
import com.user.contact.application.use_case.get.GetContactsCommand
import com.user.contact.application.use_case.get.GetContactsUseCase
import com.user.contact.infrastructure.mapper.ContactMapper
import com.user.contact.infrastructure.web.dto.request.CreateContactRequest
import com.user.contact.infrastructure.web.dto.response.ContactResponse
import com.user.exceptions.dto.ErrorDto
import com.user.exceptions.jwt.TokenUnauthorizedException
import com.user.user.domain.model.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@Validated
@RestController
@RequestMapping("/api/v1/contacts")
@Tag(name = "Contacts", description = "Управление контактами текущего пользователя")
class ContactController(
    private val mapper: ContactMapper,
    private val getContactsUseCase: GetContactsUseCase,
    private val createContactUseCase: CreateContactUseCase,
    private val deleteContactUseCase: DeleteContactUseCase,
) {

    @GetMapping
    @Operation(
        summary = "Список контактов",
        description = "Возвращает постранично контакты текущего пользователя."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Страница контактов"),
        ApiResponse(
            responseCode = "401",
            description = "Токен отсутствует или невалиден",
            content = [Content(schema = Schema(implementation = ErrorDto::class))]
        ),
    )
    fun getContacts(
        pageable: Pageable,
        @Parameter(hidden = true) @AuthenticationPrincipal user: User?,
    ): ResponseEntity<Page<ContactResponse>> {
        val result = getContactsUseCase.execute(
            GetContactsCommand(
                user = user ?: throw TokenUnauthorizedException(),
                pageable = pageable,
            )
        )
        return ResponseEntity.ok(result.map { mapper.toResponse(it) })
    }

    @PostMapping
    @Operation(
        summary = "Добавить контакт",
        description = "Создаёт контакт между текущим пользователем и указанным. " +
            "В заголовке Location возвращается ссылка на созданный контакт."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Контакт создан", content = [Content()]),
        ApiResponse(
            responseCode = "400",
            description = "Запрос невалиден или контакт с самим собой",
            content = [Content(schema = Schema(implementation = ErrorDto::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Токен отсутствует или невалиден",
            content = [Content(schema = Schema(implementation = ErrorDto::class))]
        ),
        ApiResponse(
            responseCode = "409",
            description = "Контакт уже существует",
            content = [Content(schema = Schema(implementation = ErrorDto::class))]
        ),
    )
    fun create(
        @Validated @RequestBody request: CreateContactRequest,
        @Parameter(hidden = true) @AuthenticationPrincipal user: User?,
    ): ResponseEntity<ContactResponse> {
        if (user == null) throw TokenUnauthorizedException()
        val result = createContactUseCase.execute(
            CreateContactCommand(
                user = user,
                contactUserId = request.contactUserId
            )
        )
        return ResponseEntity.created(URI.create("/api/v1/contacts/${result.id}")).build()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить контакт")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Контакт удалён", content = [Content()]),
        ApiResponse(
            responseCode = "401",
            description = "Токен отсутствует или невалиден",
            content = [Content(schema = Schema(implementation = ErrorDto::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Контакт не найден",
            content = [Content(schema = Schema(implementation = ErrorDto::class))]
        ),
    )
    fun delete(
        @Parameter(description = "Идентификатор контакта") @PathVariable id: UUID,
        @Parameter(hidden = true) @AuthenticationPrincipal user: User?,
    ): ResponseEntity<Void> {
        deleteContactUseCase.execute(
            DeleteContactCommand(
                id = id,
                user = user ?: throw TokenUnauthorizedException(),
            )
        )
        return ResponseEntity.noContent().build()
    }

}
