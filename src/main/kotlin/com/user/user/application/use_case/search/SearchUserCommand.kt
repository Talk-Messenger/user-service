package com.user.user.application.use_case.search

import org.springframework.data.domain.Pageable

data class SearchUserCommand(

    val query: String,

    val pageable: Pageable,

)
