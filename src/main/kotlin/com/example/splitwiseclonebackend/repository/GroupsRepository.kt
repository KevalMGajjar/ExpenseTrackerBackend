package com.example.splitwiseclonebackend.repository

import com.example.splitwiseclonebackend.models.Group
import com.example.splitwiseclonebackend.models.Member
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface GroupsRepository : MongoRepository<Group, ObjectId> {

    fun findGroupByGroupId(id: ObjectId): Group?

    fun deleteGroupByGroupId(groupId: ObjectId)

    @Query("{ 'members.user_id': ?0 }")
    fun findAllByMemberUserId(userId: ObjectId): List<Group>

    @Query("{ '_id': ?0, 'members.user_id': ?1 }")
    fun findGroupByGroupIdAndUserId(groupId: ObjectId, userId: ObjectId): Group?

}