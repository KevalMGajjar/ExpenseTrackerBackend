package com.example.splitwiseclonebackend.security_configs

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class HashEncoder {

    private val bCrypt = BCryptPasswordEncoder()

    fun encode(password: String): String = bCrypt.encode(password)

    fun matches(password: String, hashedPassword: String): Boolean = bCrypt.matches(password, hashedPassword)
}