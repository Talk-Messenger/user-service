package com.user.utils

import com.user.configs.JwtConfig
import com.user.exceptions.jwt.TokenInvalidException
import com.user.exceptions.jwt.TokenUnauthorizedException
import com.user.user.domain.model.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.security.PrivateKey
import java.security.PublicKey
import java.time.Clock
import java.time.Instant
import java.util.Date
import java.util.UUID
import java.util.function.Function

@Component
class JwtController (
    private val publicKey: PublicKey,
    private val privateKey: PrivateKey,
    private val jwtConfig: JwtConfig,
    private val clock: Clock
)  {

    private val logger = KotlinLogging.logger {}

    fun isTokenValid(token: String, user: User): Boolean {
        try {
            val claims = validateAndGetClaims(token)

            val tokenId = claims.subject
            if (user.id.toString() != tokenId) {
                return false
            }

            val tokenType = claims.get(jwtConfig.tokenTypeClaim, String::class.java)

            return jwtConfig.accessTokenType == tokenType && !claims.expiration.before(Date())
        } catch (e: Exception) {
            logger.error("Token validation failed: {}", e.message)
            return false
        }
    }

    fun extractTokenPairId(token: String, ignoreExpiration: Boolean): String? {
        return try {
            extractClaim(
                token=token,
                ignoreExpiration=ignoreExpiration,
                claimsResolver = { claims ->
                    claims?.get(jwtConfig.tokenPairIdClaim) as? String
                }
            )
        } catch (_: Exception) {
            null
        }
    }

    fun extractId(token: String, ignoreExpiration: Boolean): UUID {
        try {
            return UUID.fromString(validateAndGetClaims(token, ignoreExpiration).subject)
        } catch (_: Exception) {
            throw TokenInvalidException()
        }
    }

    fun isRefreshTokenValid(token: String, user: User): Boolean {
        val now = Instant.now(clock)
        try {
            val claims = validateAndGetClaims(token)

            val tokenType = claims.get(jwtConfig.tokenTypeClaim, String::class.java)
            if (jwtConfig.refreshTokenType != tokenType) {
                return false
            }

            val tokenId = claims.subject
            // ToDelete
            logger.info("Expiration: ${claims.expiration}\nNow: ${Date(now.toEpochMilli())}")
            return !(user.id.toString() != tokenId || claims.expiration.before(Date(now.toEpochMilli())))
        } catch (e: java.lang.Exception) {
            logger.error("Refresh token validation failed: {}", e.message)
            return false
        }
    }

    fun isAccessTokenExpired(token: String): Boolean {
        val now = Instant.now(clock)
        try {
            val claims: Claims = Jwts
                .parser()
                .verifyWith(publicKey)
                .clock { Date(now.toEpochMilli()) }
                .build()
                .parseSignedClaims(token)
                .payload
            return claims.expiration.before(Date(now.toEpochMilli()))
        } catch (_: ExpiredJwtException) {
            return true
        } catch (e: JwtException) {
            logger.error("Invalid token during expiration check: {}", e.message)
            return true
        }
    }

    private fun <T> extractClaim(token: String?, claimsResolver: Function<Claims?, T?>, ignoreExpiration: Boolean = false): T? {
        val claims: Claims = validateAndGetClaims(token, ignoreExpiration)
        return claimsResolver.apply(claims)
    }

    private fun validateAndGetClaims(token: String?, ignoreExpiration: Boolean = false): Claims {
        try {
            return Jwts
                .parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: ExpiredJwtException) {
            if (ignoreExpiration) return e.claims
            throw TokenUnauthorizedException("Токен устарел")
        } catch (e: JwtException) {
            logger.error("Failed to validate token: ${e.message}")
            throw TokenUnauthorizedException("Токен невалиден")
        }
    }

}