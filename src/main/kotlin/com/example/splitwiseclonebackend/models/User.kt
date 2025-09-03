package com.example.splitwiseclonebackend.models

import org.bson.types.ObjectId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.Instant


@Document(collection = "Users")
data class User(
    @Id val userId: ObjectId = ObjectId(),
    @Field("email")@Indexed(unique = true) val email: String,
    @Field("username")@Indexed(unique = true) var username: String,
    @Field("hashed_password") var hashedPassword: String,
    @Field("phone_number") @Indexed(unique = true, sparse = true) var phoneNumber: String? = null,
    @Field("profile_pic_url") var profilePicUrl: String = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Default_pfp.svg/1024px-Default_pfp.svg.png",
    @Field("default_currency_code") var defaultCurrencyCode: String = "Inr",
    @CreatedDate @Field("created_at") val createdDate: Instant,
    @LastModifiedDate @Field("last_modified_at") val lastModifiedDate: Instant? = null,
    )
