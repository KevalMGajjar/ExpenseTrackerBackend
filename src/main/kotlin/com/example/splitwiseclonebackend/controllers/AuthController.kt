package com.example.splitwiseclonebackend.controllers

import com.example.splitwiseclonebackend.repository.UserRepository
import com.example.splitwiseclonebackend.security_configs.AuthService
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import com.example.splitwiseclonebackend.services.UserAlreadyExistsException
import com.example.splitwiseclonebackend.utils.PhoneNumberNormalizer
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService, private val userRepository: UserRepository) {

    data class AuthRegisterRequest(
        @field:Email(message = "Invalid email format")
        val email: String,
        val password: String,
        val username: String,
        val phoneNumber: String
    )

    data class AuthLoginRequest(
        @field:Email(message = "Invalid email format")
        val email: String,
        val password: String
    )

    data class RefreshRequest(
        val refreshToken: String
    )

    data class GoogleLoginRequest(val idToken: String)

    @PostMapping("/register")
    fun register(@Valid @RequestBody authRequest: AuthRegisterRequest): ResponseEntity<Any> {
        return try {
            // The authService will now return the newly created user object
            val newUser = authService.register(
                email = authRequest.email,
                password = authRequest.password,
                username = authRequest.username,
                phoneNumber = PhoneNumberNormalizer.normalize(authRequest.phoneNumber)
            )
            // On success, return a 201 Created status with the user object in the body
            ResponseEntity.status(HttpStatus.CREATED).body(newUser)
        } catch (e: UserAlreadyExistsException) {
            // If the user already exists, return a 409 Conflict with a clear error message
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to e.message))
        } catch (e: Exception) {
            // For any other unexpected errors, return a 500 Internal Server Error
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "An unexpected error occurred."))
        }
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody authRequest: AuthLoginRequest): ResponseEntity<Any> {
        return try {
            // The authService will now return the successful login response or throw an exception
            val loginResponse = authService.login(
                email = authRequest.email,
                password = authRequest.password
            )
            // On success, return a 200 OK status with the login response in the body
            ResponseEntity.ok(loginResponse)
        } catch (e: ResponseStatusException) {
            // If the service throws a specific error (like User Not Found or Incorrect Password),
            // catch it and return the corresponding status code and a clear error message.
            ResponseEntity
                .status(e.statusCode)
                .body(mapOf("error" to e.reason))
        } catch (e: Exception) {
            // For any other unexpected errors, return a 500 Internal Server Error
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "An unexpected server error occurred."))
        }
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody body: RefreshRequest): AuthService.TokenPair {
        return authService.refresh(body.refreshToken)
    }

    @PostMapping("/google")
    fun loginWithGoogle(@RequestBody request: GoogleLoginRequest): AuthService.LoginResponse {
        return authService.loginWithGoogle(request.idToken)
    }
}


