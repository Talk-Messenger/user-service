package com.user.configs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import kotlin.io.encoding.Base64

@Configuration
class JwtKeyConfig (
    private val jwtConfig: JwtConfig,
) {

    @Bean
    fun publicKey(): PublicKey {
        return X509EncodedKeySpec(
            Base64.decode(jwtConfig.publicKey.trim())
        ).let {
            KeyFactory
                .getInstance("EC")
                .generatePublic(it)
        }
    }

    @Bean
    fun privateKey(): PrivateKey {
        return PKCS8EncodedKeySpec(
            Base64.decode(jwtConfig.privateKey.trim())
        ).let {
            KeyFactory
                .getInstance("EC")
                .generatePrivate(it)
        }
    }

}