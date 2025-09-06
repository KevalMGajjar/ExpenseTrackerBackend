package com.example.splitwiseclonebackend.models

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import java.math.BigDecimal

@Document(collection = "friends")
data class Friend(
    @Id val id : ObjectId = ObjectId(),
    val profilePic: String,
    val username: String,
    val phoneNumber: String?,
    val email: String?,
    @Field(targetType = FieldType.DECIMAL128)
    var balanceWithUser: BigDecimal? = BigDecimal.ZERO,
    val friendId: ObjectId,
    val currentUserId: ObjectId,
)
