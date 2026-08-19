package com.user.utlis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class MapperConfiguration {

    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper().registerModule(JavaTimeModule())

}