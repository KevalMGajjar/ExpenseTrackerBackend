package com.example.splitwiseclonebackend.security_configs


import com.example.splitwiseclonebackend.models.RefreshToken
import com.example.splitwiseclonebackend.models.User
import com.example.splitwiseclonebackend.repository.RefreshTokenRepository
import com.example.splitwiseclonebackend.repository.UserRepository
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.Collections
import java.util.UUID


@Service
class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    private val refreshTokenRepository: RefreshTokenRepository,
    @Value("\${google.client.id}") private val googleClientId: String
) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    data class TokenPair(
        val accessToken: String,
        val refreshToken: String
    )

    data class LoginResponse(
        val accessToken: String,
        val refreshToken: String,
        val userId: String,
        val email: String,
        val hashedPassword: String,
        val phoneNumber: String?,
        val profilePicture: String,
        val username: String,
        val defaultCurrencyCode: String,
    )

    fun register(email: String, password: String, username: String, phoneNumber: String): User {
        val user = userRepository.findUserByEmail(email.trim())
        if (user != null) {
            throw ResponseStatusException(HttpStatus.valueOf(409), "User already exists")
        }
        return userRepository.save(
            User(
                email = email.trim(),
                hashedPassword = hashEncoder.encode(password),
                createdDate = Instant.now(),
                username = username,
                phoneNumber = phoneNumber
            )
        )
    }

    fun login(email: String, password: String): LoginResponse {
        val user = userRepository.findUserByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.valueOf(404), "User not found")
        if(!hashEncoder.matches(password, user.hashedPassword)) {
            throw ResponseStatusException(HttpStatus.valueOf(401), "Passwords do not match")
        }

        val newAccessToken = jwtService.generateAccessToken(user.userId.toHexString())
        val newRefreshToken = jwtService.generateRefreshToken(user.userId.toHexString())

        storeRefreshToken(user.userId, newRefreshToken)

        return LoginResponse(
            newAccessToken,
            newRefreshToken,
            user.userId.toHexString(),
            user.email,
            user.hashedPassword,
            user.phoneNumber,
            user.profilePicUrl,
            user.username,
            user.defaultCurrencyCode
        )
    }
    @Transactional
    fun refresh(refreshToken: String): TokenPair {
        if(!jwtService.validateRefreshToken(refreshToken)) {
            throw ResponseStatusException(HttpStatus.valueOf(401), "Invalid refresh token")
        }
        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user = userRepository.findById(userId).orElseThrow{
            throw ResponseStatusException(HttpStatus.valueOf(404), "User not found")
        }

        val hashed = hashToken(refreshToken)
        refreshTokenRepository.findByUserIdAndHashedToken(user.userId, hashed)
            ?: throw ResponseStatusException(HttpStatus.valueOf(404), "Invalid refresh token")


        refreshTokenRepository.deleteByUserIdAndHashedToken(user.userId, hashed)

        val newAccessToken = jwtService.generateAccessToken(userId)
        val newRefreshToken = jwtService.generateRefreshToken(userId)

        storeRefreshToken(user.userId, newRefreshToken)

        return TokenPair(
            newAccessToken,
            newRefreshToken
        )

    }

    private fun storeRefreshToken(userId: ObjectId, rawToken: String) {
        val hashedToken = hashToken(rawToken)
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiryAt = Instant.now().plusMillis(expiryMs)

        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                hashedToken = hashedToken,
                expireAt = expiryAt,
            )
        )

    }

    private fun hashToken(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawToken.toByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }

    fun loginWithGoogle(idTokenString: String): LoginResponse {
        val googleUser = verifyGoogleToken(idTokenString)
            ?: throw IllegalArgumentException("Invalid Google ID Token")

        // Find user by email. If they don't exist, create a new one.
        val user = userRepository.findUserByEmail(googleUser.email)
            ?: createGoogleUser(googleUser)

        val accessToken = jwtService.generateAccessToken(user.userId.toHexString())
        val refreshToken = jwtService.generateRefreshToken(user.userId.toHexString())
        return LoginResponse(
            accessToken,
            refreshToken,
            user.userId.toHexString(),
            user.email,
            user.hashedPassword,
            user.phoneNumber ?: "",
            user.profilePicUrl,
            user.username,
            user.defaultCurrencyCode
        )
    }

    private fun verifyGoogleToken(idTokenString: String): GoogleIdToken.Payload? {
        val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(googleClientId))
            .build()

        return try {
            val idToken: GoogleIdToken = verifier.verify(idTokenString)
            idToken.payload
        } catch (e: Exception) {
            logger.error("Error verifying google token", e)
            null
        }
    }

    private fun createGoogleUser(payload: GoogleIdToken.Payload): User {
        val newUser = User(
            email = payload.email,
            username = payload["name"] as? String ?: "User",
            hashedPassword = hashEncoder.encode(UUID.randomUUID().toString()),
            profilePicUrl = payload["picture"] as? String ?: "",
            defaultCurrencyCode = "Inr",
            createdDate = Instant.now()
        )
        return userRepository.save(newUser)
    }
}