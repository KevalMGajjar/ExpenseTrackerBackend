package com.example.splitwiseclonebackend.models

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "friends")
data class Friend(
    @Id val id : ObjectId = ObjectId(),
    val profilePic: String,
    val username: String,
    val phoneNumber: String?,
    val email: String?,
    var balanceWithUser: Double? = 0.0,
    val friendId: ObjectId,
    val currentUserId: ObjectId,
)
