package com.example.splitwiseclonebackend.controllers

import com.example.splitwiseclonebackend.repository.UserRepository
import com.example.splitwiseclonebackend.security_configs.AuthService
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
    fun register(@Valid @RequestBody authRequest: AuthRegisterRequest) {
        authService.register(email = authRequest.email, password = authRequest.password, username = authRequest.username, phoneNumber = authRequest.phoneNumber)
    }

    @PostMapping("/login")
    fun login(@RequestBody authRequest: AuthLoginRequest): AuthService.LoginResponse {
        return authService.login(email = authRequest.email, password = authRequest.password)
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
