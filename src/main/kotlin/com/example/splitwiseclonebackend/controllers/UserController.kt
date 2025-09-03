package com.example.splitwiseclonebackend.controllers

import com.example.splitwiseclonebackend.models.User
import com.example.splitwiseclonebackend.repository.UserRepository
import com.example.splitwiseclonebackend.security_configs.HashEncoder
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user")
class UserController(
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder
) {

    data class UpdateUserRequest(
        val id : String,
        val newProfilePicUrl: String?,
        val newUsername: String?,
        val newDefaultCurrencyCode: String?,
        val newPassword: String?,
        val oldPassword: String?,
        val newPhoneNumber: String?,
    )

    data class FindUserResponse(
        val userId: String,
        val email: String,
        val phoneNumber: String?,
        val profilePic: String,
        val username: String,
    ) {
        companion object {
            fun from(user: User): FindUserResponse {
                return FindUserResponse(
                    userId = user.userId.toString(),
                    email = user.email,
                    phoneNumber = user.phoneNumber,
                    profilePic = user.profilePicUrl,
                    username = user.username,
                )
            }
        }
    }

    @PostMapping("/update")
    fun updateUser(@Valid @RequestBody updateUserRequest: UpdateUserRequest) {
        val user = userRepository.findUserByUserId(ObjectId(updateUserRequest.id))

        if (user != null) {
            updateUserRequest.newProfilePicUrl?.let { user.profilePicUrl = it}
            updateUserRequest.newUsername?.let { user.username = it }
            updateUserRequest.newDefaultCurrencyCode?.let { user.defaultCurrencyCode = it }
            updateUserRequest.newPhoneNumber?.let { user.phoneNumber = it }
            if (updateUserRequest.oldPassword != null && updateUserRequest.newPassword != null) {

                if(hashEncoder.matches(updateUserRequest.oldPassword, user.hashedPassword)) {
                    updateUserRequest.newPassword.let { user.hashedPassword = hashEncoder.encode(it) }
                }

            }
            userRepository.save(user)
        }
    }

}