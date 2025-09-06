package com.example.splitwiseclonebackend.repository

import com.example.splitwiseclonebackend.models.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : MongoRepository<User, String> {

    fun findUserByEmail(email: String): User?

    fun findUserByUserId(id: ObjectId): User?

    fun findUserByPhoneNumber(phoneNumber: String): User?

    fun findByPhoneNumberIn(phoneNumbers: List<String>): List<User>

    fun existsByEmail(email: String): Boolean

    fun existsByUsername(username: String): Boolean

}