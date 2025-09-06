package com.example.splitwiseclonebackend.repository


import com.example.splitwiseclonebackend.models.Expense
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface ExpenseRepository : MongoRepository<Expense, ObjectId> {

    fun findExpenseById(id: ObjectId): Expense?

    @Query("{'participants':  ?0}")
    fun findExpensesByUserId(userId: ObjectId): List<Expense>

    fun deleteAllByCreatedByUserId(createdByUserId: ObjectId)
}