package com.example.splitwiseclonebackend.security_configs

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.Base64
import java.util.Date


@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
) {

    private val secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))

    private val accessTokenValidityMs = 15L * 60L * 1000L
    val refreshTokenValidityMs = 180L * 24 * 60 * 60 * 1000L

    fun generateToken(userId: String, type: String, expiry: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + expiry)
        return Jwts.builder()
            .subject(userId)
            .claim("type", type)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }

    fun validateAccessToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims["type"] as? String ?: return false
        return tokenType.equals("Access", ignoreCase = true)
    }

    fun validateRefreshToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims["type"] as? String ?: return false
        return tokenType.equals("Refresh", ignoreCase = true)
    }

    fun getUserIdFromToken(token: String): String {
        val claims = parseAllClaims(token) ?: throw ResponseStatusException(HttpStatus.valueOf(401), "No such user found")
        return claims.subject
    }

    private fun parseAllClaims(token: String): Claims? {
        return try {
            val rawToken = if(token.startsWith("Bearer ")) {
                token.removePrefix("Bearer ")
            } else token
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(rawToken)
                .payload
        } catch (e: Exception) {
            null
        }
    }

    fun  generateAccessToken(userId: String): String{
        return generateToken(userId, "Access", accessTokenValidityMs)
    }

    fun  generateRefreshToken(userId: String): String{
        return generateToken(userId, "Refresh", refreshTokenValidityMs)
    }
}