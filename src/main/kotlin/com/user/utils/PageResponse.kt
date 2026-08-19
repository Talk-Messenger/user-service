package com.user.utils

data class PageResponse<T>(
    val content: List<T>,
    val limit: Int,
    val offset: Int,
    val total: Long,
)
