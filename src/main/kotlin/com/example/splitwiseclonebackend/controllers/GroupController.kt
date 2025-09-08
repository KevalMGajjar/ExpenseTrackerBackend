package com.example.splitwiseclonebackend.controllers

import com.example.splitwiseclonebackend.models.Group
import com.example.splitwiseclonebackend.models.Member
import com.example.splitwiseclonebackend.repository.GroupsRepository
import com.example.splitwiseclonebackend.repository.UserRepository
import com.example.splitwiseclonebackend.repository.ExpenseRepository
import com.example.splitwiseclonebackend.services.DebtSimplificationService
import com.example.splitwiseclonebackend.services.Transaction
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@RestController
@RequestMapping("/group")
class GroupController(private val groupsRepository: GroupsRepository, private val userRepository: UserRepository, private val expenseRepository: ExpenseRepository, // Add this
                      private val debtSimplificationService: DebtSimplificationService ) {

    data class GroupRequest(
        val groupName: String,
        val profilePicture: String,
        val groupCreatedByUserId: String,
        val type: String
    )

    data class GroupUpdateRequest(
        val groupId: String,
        val groupName: String
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

    data class SimplifiedDebtResponse(
        val fromUser: String,
        val toUser: String,
        val amount: Double,
        val fromUsername: String,
        val toUsername: String
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

    @DeleteMapping("/deleteMembers/{groupId}")
    fun deleteMembers(
        @PathVariable groupId: String,
        @RequestParam memberIds: List<String> // Spring Boot automatically maps multiple params like ?memberIds=...&memberIds=...
    ): ResponseEntity<Unit> {
        val group = groupsRepository.findGroupByGroupId(ObjectId(groupId))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")

        val membersToRemove = memberIds.map { ObjectId(it) }.toSet()

        // This is a much more efficient way to remove members
        val updatedMembers = group.members?.filter { member ->
            member.userId !in membersToRemove
        }

        group.members = updatedMembers
        group.updatedAt = Instant.now()
        groupsRepository.save(group)

        // Return a successful response
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/delete/{groupId}")
    fun deleteGroup(
        @PathVariable groupId: String,
        @RequestParam currentUserId: String
    ): ResponseEntity<Unit> {
        val group = groupsRepository.findGroupByGroupId(ObjectId(groupId))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")

        // Check if the user making the request is the admin of the group
        if (group.groupCreatedByUserId == ObjectId(currentUserId)) {
            // FIX: The original code had a bug where it tried to delete by the user's ID.
            // This now correctly deletes the group by its own ID.
            groupsRepository.deleteById(group.groupId)
            // TODO: You might need to call a service here to reverse all expense balances related to this group.
            // e.g., balanceService.reverseAllSplitsForGroup(group.id)
            return ResponseEntity.ok().build()
        } else {
            // If the user is not the admin, they are not allowed to delete the group.
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "User is not the group admin and cannot delete it.")
        }
    }

    @PostMapping("/update")
    fun update(@Valid @RequestBody groupUpdateRequest: GroupUpdateRequest) {
        val groupToBeUpdated = groupsRepository.findGroupByGroupId(ObjectId(groupUpdateRequest.groupId))

        if (groupToBeUpdated != null) {
            groupUpdateRequest.groupName.let { groupToBeUpdated.groupName = it }
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

    @GetMapping("/{groupId}/simplify")
    fun getSimplifiedDebts(@PathVariable groupId: String): List<SimplifiedDebtResponse> {
        val groupObjectId = ObjectId(groupId)
        val groupExpenses = expenseRepository.findAll().filter { it.groupId == groupObjectId }

        if (groupExpenses.isEmpty()) {
            return emptyList()
        }

        val simplifiedTransactions = debtSimplificationService.simplifyDebts(groupExpenses)

        return simplifiedTransactions.map { transaction ->
            val fromUser = userRepository.findUserByUserId(transaction.from)
            val toUser = userRepository.findUserByUserId(transaction.to)
            SimplifiedDebtResponse(
                fromUser = transaction.from.toHexString(),
                toUser = transaction.to.toHexString(),
                amount = transaction.amount.toDouble(),
                fromUsername = fromUser?.username ?: "Unknown",
                toUsername = toUser?.username ?: "Unknown"
            )
        }
    }
}