package com.example.splitwiseclonebackend.repository

import com.example.splitwiseclonebackend.models.RefreshToken
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface RefreshTokenRepository : MongoRepository<RefreshToken, ObjectId> {

    fun findByUserIdAndHashedToken(userId: ObjectId, hashedToken: String): RefreshToken?

    fun deleteByUserIdAndHashedToken(userId: ObjectId, token: String)

}