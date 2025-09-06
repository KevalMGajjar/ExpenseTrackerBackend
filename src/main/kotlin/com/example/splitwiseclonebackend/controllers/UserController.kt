package com.example.splitwiseclonebackend.controllers

import com.example.splitwiseclonebackend.models.User
import com.example.splitwiseclonebackend.repository.ExpenseRepository
import com.example.splitwiseclonebackend.repository.FriendsRepository
import com.example.splitwiseclonebackend.repository.GroupsRepository
import com.example.splitwiseclonebackend.repository.UserRepository
import com.example.splitwiseclonebackend.security_configs.HashEncoder
import com.example.splitwiseclonebackend.utils.PhoneNumberNormalizer
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/user")
class UserController(
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    private val friendsRepository: FriendsRepository,
    private val groupRepository: GroupsRepository,
    private val expenseRepository: ExpenseRepository
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

    // In your UserController.kt file on the server

    @PostMapping("/update")
    fun updateUser(@Valid @RequestBody updateUserRequest: UpdateUserRequest): ResponseEntity<User> { // FIX 1: Return the User object
        val user = userRepository.findUserByUserId(ObjectId(updateUserRequest.id))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        // --- Update user fields individually ---
        updateUserRequest.newProfilePicUrl?.let { user.profilePicUrl = it }
        updateUserRequest.newUsername?.let { user.username = it }
        // Note: Currency code updates are disallowed as per your request.
        updateUserRequest.newPhoneNumber?.let { user.phoneNumber = PhoneNumberNormalizer.normalize(it) }

        // FIX 2: Corrected password update logic.
        // This now correctly handles updating the password ONLY if both old and new passwords are provided.
        // It no longer prevents other fields from being updated independently.
        if (!updateUserRequest.oldPassword.isNullOrBlank() && !updateUserRequest.newPassword.isNullOrBlank()) {
            if (hashEncoder.matches(updateUserRequest.oldPassword, user.hashedPassword)) {
                user.hashedPassword = hashEncoder.encode(updateUserRequest.newPassword)
            } else {
                // It's good practice to throw an error if the old password is incorrect
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect old password")
            }
        }

        // Save the updated user
        val updatedUser = userRepository.save(user)

        // FIX 3: Always return the fully updated user object in the response body.
        // This is what solves the `EOFException` crash in the Android app.
        return ResponseEntity.ok(updatedUser)
    }

    @DeleteMapping("/delete/{userId}")
    fun deleteUserAccount(@PathVariable userId: String): ResponseEntity<Unit> {
        val userObjectId = ObjectId(userId)

        // 1. Delete all friendships involving the user
        friendsRepository.deleteAllByCurrentUserIdOrFriendId(userObjectId, userObjectId)

        // 2. Remove the user from all groups they are a member of
        val groupsToUpdate = groupRepository.findAllByMembers_UserId(userObjectId)
        groupsToUpdate.forEach { group ->
            val updatedMembers = group.members?.filter { it.userId != userObjectId }
            group.members = updatedMembers
            groupRepository.save(group)
        }

        // 3. Delete all expenses created by the user
        expenseRepository.deleteAllByCreatedByUserId(userObjectId)

        // TODO: A more advanced implementation would also re-calculate splits in expenses where the user was a participant.

        // 4. Finally, delete the user themselves
        userRepository.deleteById(userObjectId.toHexString())

        // Return a successful response
        return ResponseEntity.ok().build()
    }


}