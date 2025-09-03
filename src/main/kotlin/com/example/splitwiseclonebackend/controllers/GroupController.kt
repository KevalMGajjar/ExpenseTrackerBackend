package com.example.splitwiseclonebackend.controllers

import com.example.splitwiseclonebackend.models.Group
import com.example.splitwiseclonebackend.models.Member
import com.example.splitwiseclonebackend.repository.GroupsRepository
import com.example.splitwiseclonebackend.repository.UserRepository
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@RestController
@RequestMapping("/group")
class GroupController(private val groupsRepository: GroupsRepository, private val userRepository: UserRepository) {

    data class GroupRequest(
        val groupName: String,
        val profilePicture: String,
        val groupCreatedByUserId: String,
        val type: String
    )

    data class GroupUpdateRequest(
        val groupId: String,
        val groupName: String,
        val profilePicture: String,
    )

    data class AddMembersRequest(
        val groupId: String,
        val members: List<Member>
    )

    data class GetAllGroupsRequest(
        val userId: String,
    )

    data class GetAllGroupsResponse(
        val groupName: String,
        val profilePic: String,
        val createdByUserId: String,
        val type: String,
        val members: List<MemberResponse>?,
        val archived: Boolean,
        val groupId: String
    )

    data class MemberResponse(
        val userId: String,
        val role: String,
        val username: String,
        val email: String,
        val profilePicture: String
    )

    data class GroupAddResponse(
        val groupId: String
    )

    data class DeleteGroupRequest(
        val groupId: String,
        val currentUserId: String
    )

    data class DeleteGroupMembers(
        val groupId: String,
        val membersIds: List<String>
    )

    @PostMapping("/add")
    fun save(@Valid @RequestBody groupRequest: GroupRequest): GroupAddResponse {

        val currentUser = userRepository.findUserByUserId(ObjectId(groupRequest.groupCreatedByUserId))

        if (currentUser != null) {

            val initialMember =
                Member(
                    userId = currentUser.userId,
                    role = "admin",
                    username = currentUser.username,
                    email = currentUser.email,
                    profilePicture = currentUser.profilePicUrl
                )

            val members: MutableList<Member> = ArrayList()
            members.add(initialMember)

            val group = Group(
                groupName = groupRequest.groupName,
                profilePicture = groupRequest.profilePicture,
                groupCreatedByUserId = ObjectId(groupRequest.groupCreatedByUserId),
                members = members,
                createdAt = Instant.now(),
                type = groupRequest.type
            )

            groupsRepository.save<Group>(group)
            return GroupAddResponse(groupId = group.groupId.toHexString())
        }else{
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group Admin not found")
        }
    }

    @DeleteMapping("/delete")
    fun delete(@Valid @RequestBody deleteRequest: DeleteGroupRequest) {

        val group = groupsRepository.findGroupByGroupId(ObjectId(deleteRequest.groupId))

        if (group?.groupCreatedByUserId == ObjectId(deleteRequest.currentUserId)) {
            groupsRepository.deleteGroupByGroupId(ObjectId(deleteRequest.currentUserId))
        } else {
            throw ResponseStatusException(HttpStatus.CONFLICT, "User is not group admin")
        }

    }

    @PostMapping("/update")
    fun update(@Valid @RequestBody groupUpdateRequest: GroupUpdateRequest) {
        val groupToBeUpdated = groupsRepository.findGroupByGroupId(ObjectId(groupUpdateRequest.groupId))

        if (groupToBeUpdated != null) {
            groupUpdateRequest.groupName.let { groupToBeUpdated.groupName = it }
            groupUpdateRequest.profilePicture.let { groupToBeUpdated.profilePicture = it }
            groupsRepository.save(groupToBeUpdated)
        }
    }

    @PostMapping("/addMembers")
    fun addMembers(@Valid @RequestBody addMembersRequest: AddMembersRequest) {

        val group = groupsRepository.findGroupByGroupId(ObjectId(addMembersRequest.groupId))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")

        val existingMemberIds = group.members?.map { it.userId }?.toSet() ?: emptySet()

        val userAlreadyExists = addMembersRequest.members.any { it.userId in existingMemberIds }
        if (userAlreadyExists) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }

        val newMembers = addMembersRequest.members.map { memberRequest ->
            Member(
                userId = memberRequest.userId,
                role = memberRequest.role,
                username = memberRequest.username,
                email = memberRequest.email,
                profilePicture = memberRequest.profilePicture
            )
        }

        val updatedMemberList = group.members?.toMutableList() ?: mutableListOf()
        updatedMemberList.addAll(newMembers)
        group.members = updatedMemberList
        group.updatedAt = Instant.now()

        groupsRepository.save(group)
    }

    @PostMapping("/deleteMembers")
    fun deleteMembers(@Valid @RequestBody deleteGroupMembers: DeleteGroupMembers) {

        val group = groupsRepository.findGroupByGroupId(ObjectId(deleteGroupMembers.groupId))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")

        val existingMembersIds = group.members?.map { it.userId }?.toSet() ?: emptySet()
        val membersToDelete = deleteGroupMembers.membersIds.map { memberId ->
            println("$memberId ${deleteGroupMembers.groupId} deleted")
            val group = groupsRepository.findGroupByGroupIdAndUserId(ObjectId(deleteGroupMembers.groupId), ObjectId(memberId))
            group?.members?.find { it.userId == ObjectId(memberId) }
        }

        membersToDelete.forEach { member ->
            if (member?.userId !in existingMembersIds) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found")
            }
        }

        if (membersToDelete.isNotEmpty()) {
            val updatedMembersList = group.members?.toMutableList() ?: mutableListOf()
            updatedMembersList.removeAll(membersToDelete)
            group.members = updatedMembersList
            group.updatedAt = Instant.now()

            groupsRepository.save(group)
        }

    }

    @PostMapping("/getAll")
    fun getAllGroups(@Valid @RequestBody getAllGroupsRequest: GetAllGroupsRequest): List<GetAllGroupsResponse> {
        val groupList = groupsRepository.findAllByMemberUserId(ObjectId(getAllGroupsRequest.userId))
        return groupList.map {
            GetAllGroupsResponse(
                groupName = it.groupName,
                createdByUserId = it.groupCreatedByUserId.toHexString(),
                profilePic = it.profilePicture,
                type = it.type,
                archived = it.isArchived,
                members = it.members?.map { member ->
                    MemberResponse(
                        member.userId.toHexString(),
                        member.role,
                        member.username,
                        member.email,
                        member.profilePicture
                    )
                },
                groupId = it.groupId.toHexString()
            )
        }
    }
}