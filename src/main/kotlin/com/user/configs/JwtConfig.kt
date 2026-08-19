package com.user.configs

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
data class JwtConfig(
    @Value($$"${jwt.keys.private-key}")
    val privateKey : String,
    @Value($$"${jwt.keys.public-key}")
    val publicKey : String,
    @Value($$"${jwt.expiration.access-expiration}")
    val accessTokenExpiration : Long,
    @Value($$"${jwt.expiration.refresh-expiration}")
    val refreshTokenExpiration : Long,
    @Value($$"${jwt.claims-names.token-pair-id-claim}")
    val tokenPairIdClaim: String,
    @Value($$"${jwt.claims-names.token-type-claim}")
    val tokenTypeClaim: String,
    @Value($$"${jwt.claims-names.access-token-type}")
    val accessTokenType: String,
    @Value($$"${jwt.claims-names.refresh-token-type}")
    val refreshTokenType: String,
)