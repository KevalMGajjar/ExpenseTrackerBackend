package com.example.splitwiseclonebackend.controllers

import com.example.splitwiseclonebackend.models.Friend
import com.example.splitwiseclonebackend.repository.FriendsRepository
import com.example.splitwiseclonebackend.repository.UserRepository
import com.example.splitwiseclonebackend.services.BalanceService
import com.example.splitwiseclonebackend.services.SplitDto
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/friends")
class FriendsController(
    private val friendsRepository: FriendsRepository,
    private val userRepository: UserRepository,
    private val balanceService: BalanceService
) {

    // --- Data Classes for API requests/responses ---

    data class GetAllFriendsRequest(val currentUserId: String)

    data class GetAllFriendResponse(
        val id: String?,
        val profilePic: String,
        val username: String,
        val phoneNumber: String?,
        val email: String?,
        val balanceWithUser: Double?,
        val currentUserId: String,
        val friendId: String
    )

    data class UpdateFriendBalanceRequest(
        val splits: List<SplitDto>
    )

    data class SettleUpRequest(
        val payerId: String,
        val receiverId: String,
        val amount: Double
    )

    data class AddFriendsRequest(
        val phoneNumberList: List<String>,
        val currentUserId: String,
        val currentUserEmail: String,
        val currentUserProfilePic: String,
        val currentUserPhoneNumber: String,
        val currentUserUsername: String
    )

    data class AddFriendResponse(
        val friends: List<GetAllFriendResponse>,
        val notFoundPhoneNumbers: List<String>
    )

    // --- Controller Endpoints ---

    @PostMapping("/getAll")
    fun getAllFriends(@Valid @RequestBody getAllFriendsRequest: GetAllFriendsRequest): List<GetAllFriendResponse> {
        val allFriends = friendsRepository.findAllByCurrentUserId(ObjectId(getAllFriendsRequest.currentUserId))
        return allFriends.map {
            GetAllFriendResponse(
                id = it.id.toHexString(),
                profilePic = it.profilePic,
                username = it.username,
                phoneNumber = it.phoneNumber,
                email = it.email,
                balanceWithUser = it.balanceWithUser!!.toDouble(),
                currentUserId = getAllFriendsRequest.currentUserId,
                friendId = it.friendId.toHexString()
            )
        }
    }

    @DeleteMapping("/delete")
    fun deleteFriend(
        // Use query parameters for DELETE, not a request body
        @RequestParam currentUserId: String,
        @RequestParam friendId: String
    ): ResponseEntity<Unit> {
        // This query now correctly returns a nullable Friend?
        val friendToDeleteForCurrentUser = friendsRepository.findByFriendIdAndCurrentUserId(ObjectId(friendId), ObjectId(currentUserId))
        val friendToDeleteForFriend = friendsRepository.findByFriendIdAndCurrentUserId(ObjectId(currentUserId), ObjectId(friendId))

        // FIX: Use safe calls (`?.let`) to only delete if the objects are not null.
        // This prevents the NullPointerException and makes the logic robust.
        friendToDeleteForCurrentUser?.let { friendsRepository.delete(it) }
        friendToDeleteForFriend?.let { friendsRepository.delete(it) }

        return ResponseEntity.ok().build()
    }

    @PostMapping("/updateBalance")
    fun updateBalanceWithFriend(@Valid @RequestBody updateFriendRequest: UpdateFriendBalanceRequest) {
        balanceService.processSplits(splits = updateFriendRequest.splits)
    }

    @PostMapping("/add")
    fun addFriends(@Valid @RequestBody addFriendsRequest: AddFriendsRequest): AddFriendResponse {
        val currentUserId = ObjectId(addFriendsRequest.currentUserId)
        val potentialFriendUsers = userRepository.findByPhoneNumberIn(addFriendsRequest.phoneNumberList)
        val foundNumbers = potentialFriendUsers.mapNotNull { it.phoneNumber }.toSet()

        val phoneNumbersNotFound = addFriendsRequest.phoneNumberList.filter { it !in foundNumbers }

        val usersToAdd = potentialFriendUsers.filter { potentialFriend ->
            val isNotCurrentUser = potentialFriend.userId != currentUserId
            val isNotAlreadyFriend = !friendsRepository.existsByFriendIdAndCurrentUserId(
                currentUserId = currentUserId,
                friendId = potentialFriend.userId
            )
            isNotCurrentUser && isNotAlreadyFriend
        }

        val savedFriends: List<Friend>
        if (usersToAdd.isNotEmpty()) {
            val friendLinksToCreate = usersToAdd.flatMap { friendUser ->
                val linkForCurrentUser = Friend(
                    currentUserId = currentUserId,
                    username = friendUser.username,
                    email = friendUser.email,
                    phoneNumber = friendUser.phoneNumber,
                    profilePic = friendUser.profilePicUrl,
                    friendId = friendUser.userId
                )
                val linkForNewFriend = Friend(
                    currentUserId = friendUser.userId,
                    username = addFriendsRequest.currentUserUsername,
                    email = addFriendsRequest.currentUserEmail,
                    phoneNumber = addFriendsRequest.currentUserPhoneNumber,
                    profilePic = addFriendsRequest.currentUserProfilePic,
                    friendId = currentUserId
                )
                listOf(linkForCurrentUser, linkForNewFriend)
            }
            savedFriends = friendsRepository.saveAll(friendLinksToCreate)
        } else {
            savedFriends = emptyList()
        }

        if (potentialFriendUsers.isNotEmpty() && usersToAdd.isEmpty() && phoneNumbersNotFound.isEmpty()) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Users are already in your friends list.")
        }

        val friendsAddedForCurrentUser = savedFriends.filter { it.currentUserId == currentUserId }

        return AddFriendResponse(
            friends = friendsAddedForCurrentUser.map { friend ->
                GetAllFriendResponse(
                    id = friend.id.toHexString(),
                    profilePic = friend.profilePic,
                    username = friend.username,
                    phoneNumber = friend.phoneNumber,
                    email = friend.email,
                    balanceWithUser = friend.balanceWithUser!!.toDouble(),
                    currentUserId = addFriendsRequest.currentUserId,
                    friendId = friend.friendId.toHexString()
                )
            },
            notFoundPhoneNumbers = phoneNumbersNotFound
        )
    }

    @PostMapping("/settle")
    fun settleUp(@Valid @RequestBody request: SettleUpRequest): ResponseEntity<Unit> {
        // This endpoint now receives the specific payer and receiver from the app
        // and calls the service to perform the balance update.
        balanceService.settleUp(
            payerId = ObjectId(request.payerId),
            receiverId = ObjectId(request.receiverId),
            amount = request.amount
        )
        return ResponseEntity.ok().build()
    }
}