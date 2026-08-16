package com.user.configs

import org.springframework.context.annotation.Bean
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Bean
@Throws(Exception::class)
fun securityWebFilterChain(http: HttpSecurity): SecurityFilterChain? {
    return http
        .authorizeHttpRequests(Customizer {
            auth ->
            auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
        })
        .build()
}