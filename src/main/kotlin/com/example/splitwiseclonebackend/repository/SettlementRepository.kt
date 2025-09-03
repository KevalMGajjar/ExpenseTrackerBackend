package com.example.splitwiseclonebackend.repository

import com.example.splitwiseclonebackend.models.Settlement
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface SettlementRepository : MongoRepository<Settlement, ObjectId> {
}