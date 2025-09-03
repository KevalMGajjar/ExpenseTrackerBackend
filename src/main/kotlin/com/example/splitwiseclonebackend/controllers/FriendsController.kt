package com.example.splitwiseclonebackend.controllers

import com.example.splitwiseclonebackend.models.Friend
import com.example.splitwiseclonebackend.models.Split
import com.example.splitwiseclonebackend.repository.FriendsRepository
import com.example.splitwiseclonebackend.repository.UserRepository
import com.example.splitwiseclonebackend.services.BalanceService
import com.example.splitwiseclonebackend.services.SplitDto
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/friends")
class FriendsController(
    private val friendsRepository: FriendsRepository,
    private val userRepository: UserRepository,
    private val balanceService: BalanceService
) {

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

    data class DeleteFriendRequest(val currentUserId: String, val friendId: String)

    data class UpdateFriendBalanceRequest(
        val splits: List<SplitDto>
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
        val friends: List<GetAllFriendResponse>
    )

    data class UpdateFriendResponse(
        val id: String,
        val friendId: String,
        val profilePic: String,
        val username: String,
        val phoneNumber: String?,
        val currentUserId: String,
        val email: String?,
        val balanceWithUser: Double?
    )

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
                    balanceWithUser = it.balanceWithUser,
                    currentUserId = getAllFriendsRequest.currentUserId,
                    friendId = it.friendId.toHexString()
                )
            }
    }

    @DeleteMapping("/delete")
    fun deleteFriend(@Valid @RequestBody deleteFriendRequest: DeleteFriendRequest) {

        val friendToDeleteForCurrentUser = friendsRepository.findByFriendIdAndCurrentUserId(ObjectId(deleteFriendRequest.friendId), ObjectId(deleteFriendRequest.currentUserId))
        val friendToDeleteForFriend = friendsRepository.findByFriendIdAndCurrentUserId(ObjectId(deleteFriendRequest.currentUserId), ObjectId(deleteFriendRequest.friendId))

        friendsRepository.delete(friendToDeleteForCurrentUser)
        friendsRepository.delete(friendToDeleteForFriend)
    }

    @PostMapping("/updateBalance")
    fun updateBalanceWithFriend(@Valid @RequestBody updateFriendRequest: UpdateFriendBalanceRequest){

        balanceService.processSplits(splits = updateFriendRequest.splits)

    }

    @PostMapping("/add")
    fun addFriends(@Valid @RequestBody addFriendsRequest: AddFriendsRequest): AddFriendResponse {

        val currentUserId = ObjectId(addFriendsRequest.currentUserId)

        val potentialFriendUsers = userRepository.findByPhoneNumberIn(addFriendsRequest.phoneNumberList)

        val usersToAdd = potentialFriendUsers.filter { potentialFriend ->

            val isNotCurrentUser = potentialFriend.userId != ObjectId(addFriendsRequest.currentUserId)

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
                    currentUserId = ObjectId(addFriendsRequest.currentUserId),
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

        val friendsAddedForCurrentUser = savedFriends.filter { it.currentUserId == currentUserId }

        val foundNumbers = potentialFriendUsers.mapNotNull { it.phoneNumber }.toSet()
        val phoneNumbersNotFound = addFriendsRequest.phoneNumberList.filter { it !in foundNumbers }

        if (phoneNumbersNotFound.isNotEmpty()) {
            // TODO: Send invitations to these numbers
            println("Numbers not found on platform: $phoneNumbersNotFound")
        }

        if (potentialFriendUsers.isNotEmpty() && usersToAdd.isEmpty()) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Users are already in your friends list or you tried to add yourself. $usersToAdd")
        }

        if (potentialFriendUsers.isEmpty()) {
            throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "No users found with the provided phone numbers.")
        }

        return AddFriendResponse(
            friends = friendsAddedForCurrentUser.map { friend ->
                GetAllFriendResponse(
                    id = friend.id.toHexString(),
                    profilePic = friend.profilePic,
                    username = friend.username,
                    phoneNumber = friend.phoneNumber,
                    email = friend.email,
                    balanceWithUser = friend.balanceWithUser,
                    currentUserId = addFriendsRequest.currentUserId,
                    friendId = friend.friendId.toHexString()
                )
            }
        )
    }

}