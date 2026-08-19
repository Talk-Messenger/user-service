package com.user.user.infrastructure.web

import com.user.exceptions.dto.ErrorDto
import com.user.exceptions.jwt.TokenUnauthorizedException
import com.user.exceptions.user.CannotUpdateAnotherUserException
import com.user.exceptions.web.SearchParamsInvalidException
import com.user.user.application.use_case.me.GetMyInfoCommand
import com.user.user.application.use_case.me.GetMyInfoUseCase
import com.user.user.application.use_case.public_profile.PublicProfileCommand
import com.user.user.application.use_case.public_profile.PublicProfileUseCase
import com.user.user.application.use_case.search.SearchUserCommand
import com.user.user.application.use_case.search.SearchUserUseCase
import com.user.user.application.use_case.update.UpdateUserCommand
import com.user.user.application.use_case.update.UpdateUserUseCase
import com.user.user.domain.model.User
import com.user.user.infrastructure.mapper.UserMapper
import com.user.user.infrastructure.web.dto.request.UpdateUserProfileRequest
import com.user.user.infrastructure.web.dto.response.UserMeResponse
import com.user.user.infrastructure.web.dto.response.UserPublicProfileResponse
import com.user.user.infrastructure.web.dto.response.UserSearchItemResponse
import com.user.utils.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Профили пользователей и поиск")
class UserController(
    private val mapper: UserMapper,
    private val publicProfileUseCase: PublicProfileUseCase,
    private val getMyInfoUseCase: GetMyInfoUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val searchUserUseCase: SearchUserUseCase,
) {

    @GetMapping("/{username}")
    @SecurityRequirements
    @Operation(
        summary = "Публичный профиль",
        description = "Возвращает публичную информацию о пользователе по его username. Токен не требуется."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Профиль найден"),
        ApiResponse(
            responseCode = "404",
            description = "Пользователь не найден",
            content = [Content(schema = Schema(implementation = ErrorDto::class))]
        ),
    )
    fun getByUsername(
        @Parameter(description = "Имя пользователя", example = "john_doe")
        @PathVariable username: String
    ): ResponseEntity<UserPublicProfileResponse> {
        val result = publicProfileUseCase.execute(
            PublicProfileCommand(username)
        )
        return ResponseEntity.ok(
            mapper.toPublicProfileResponse(result)
        );
    }

    @GetMapping("/me")
    @Operation(
        summary = "Текущий пользователь",
        description = "Возвращает профиль владельца access-токена."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Профиль текущего пользователя"),
        ApiResponse(
            responseCode = "401",
            description = "Токен отсутствует или невалиден",
            content = [Content(schema = Schema(implementation = ErrorDto::class))]
        ),
    )
    fun getMyInfo(
        @Parameter(hidden = true) @AuthenticationPrincipal user: User?
    ): ResponseEntity<UserMeResponse> {
        val result = getMyInfoUseCase.execute(
            GetMyInfoCommand((user ?: throw TokenUnauthorizedException()).id)
        )
        return ResponseEntity.ok(
            UserMeResponse(
                id = result.id,
                username = result.username,
                avatarUrl = result.avatarUrl,
                bio = result.bio,
                contactsCount = result.contactsCount,
                createdAt = result.createdAt,
                updatedAt = result.updatedAt,
            )
        )
    }

    @PutMapping("/{userId}")
    @Operation(
        summary = "Обновить профиль",
        description = "Обновляет профиль пользователя. Менять можно только собственный профиль."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Профиль обновлён"),
        ApiResponse(
            responseCode = "400",
            description = "Запрос невалиден",
            content = [Content(schema = Schema(implementation = ErrorDto::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "Попытка изменить чужой профиль",
            content = [Content(schema = Schema(implementation = ErrorDto::class))]
        ),
        ApiResponse(
            responseCode = "409",
            description = "Username уже занят",
            content = [Content(schema = Schema(implementation = ErrorDto::class))]
        ),
    )
    fun updateUser(
        @Parameter(hidden = true) @AuthenticationPrincipal user: User?,
        @Parameter(description = "Идентификатор пользователя") @PathVariable userId: UUID,
        @Valid @RequestBody request: UpdateUserProfileRequest
    ): ResponseEntity<UserMeResponse> {
        if (user == null || user.id != userId)
            throw CannotUpdateAnotherUserException()
        val result = updateUserUseCase.execute(
            UpdateUserCommand(
                userId = userId,
                user = user,
                username = request.username,
                avatarUrl = request.avatarUrl,
                bio = request.bio,
            )
        )
        return ResponseEntity.ok(
            mapper.toMeResponse(result)
        )
    }

    @GetMapping("/search")
    @SecurityRequirements
    @Operation(
        summary = "Поиск пользователей",
        description = "Поиск по username. Токен не требуется."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Страница результатов поиска"),
        ApiResponse(
            responseCode = "400",
            description = "Некорректные параметры поиска",
            content = [Content(schema = Schema(implementation = ErrorDto::class))]
        ),
    )
    fun search(
        @Parameter(description = "Строка поиска, 1..100 символов", example = "john")
        @RequestParam("query", defaultValue = "") query: String,
        pageable: Pageable
    ): ResponseEntity<PageResponse<UserSearchItemResponse>> {
        if (query.isEmpty() || query.length > 100 || pageable.pageNumber > 100 || pageable.offset < 0)
            throw SearchParamsInvalidException()

        val result = searchUserUseCase.execute(
            SearchUserCommand(
                query = query,
                pageable = pageable,
            )
        )

        return ResponseEntity.ok(
            PageResponse(
                content = result.content.map { mapper.toSearchItemResponse(it) },
                limit = result.pageable.pageSize,
                offset = result.pageable.offset.toInt(),
                total = result.totalElements,
            )
        )
    }

}
