package com.example.splitwiseclonebackend.models

import org.bson.types.ObjectId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.Instant

@Document(collection = "Group")
data class Group(
    @Id val groupId: ObjectId = ObjectId(),
    @Field("group_name") var groupName: String,
    @Field("group_profile_picture") var profilePicture: String,
    @Field("group_created_by_user_id") val groupCreatedByUserId: ObjectId,
    @Field("is_archived") val isArchived: Boolean = false,
    @CreatedDate @Field("created_at") val createdAt: Instant?,
    @LastModifiedDate @Field("updated_at") var updatedAt: Instant? = null,
    @Field("is_simplified_expenses_enabled") val isSimplifiedExpensesEnabled: Boolean? = false,
    @Field("type") val type: String,
    var members: List<Member>? = emptyList()
)

data class Member(
    @Field("user_id")
    val userId: ObjectId,
    val role: String,
    val username: String,
    val email: String,
    val profilePicture: String
)
