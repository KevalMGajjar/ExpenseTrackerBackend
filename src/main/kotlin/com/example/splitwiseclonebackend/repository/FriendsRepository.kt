package com.example.splitwiseclonebackend.repository

import com.example.splitwiseclonebackend.models.Friend
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface FriendsRepository : MongoRepository<Friend, ObjectId> {

    fun findAllByCurrentUserId(userId: ObjectId): List<Friend>

    fun findByFriendIdAndCurrentUserId(friendId: ObjectId, currentUserId: ObjectId): Friend?

    fun existsByFriendIdAndCurrentUserId(friendId: ObjectId, currentUserId: ObjectId): Boolean

    fun deleteAllByCurrentUserIdOrFriendId(currentUserId: ObjectId, friendId: ObjectId)

}