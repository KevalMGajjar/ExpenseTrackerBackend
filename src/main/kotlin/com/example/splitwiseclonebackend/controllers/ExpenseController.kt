package com.example.splitwiseclonebackend.controllers

import com.example.splitwiseclonebackend.models.Expense
import com.example.splitwiseclonebackend.models.Split
import com.example.splitwiseclonebackend.repository.ExpenseRepository
import com.example.splitwiseclonebackend.repository.FriendsRepository
import com.example.splitwiseclonebackend.services.BalanceService
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.jetbrains.annotations.NotNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.E

@RestController
@RequestMapping("/expense")
class ExpenseController(
    private val expenseRepository: ExpenseRepository,
    private val friendsRepository: FriendsRepository,
    private val balanceService: BalanceService
) {

    data class AddExpenseRequest(
        val groupId: String?,
        val createdByUserId: String,
        val totalExpense: Double,
        val description: String,
        val splitType: String,
        val splits: List<RequestSplitDto>,
        val currencyCode: String,
        val paidByUserIds: List<String>,
        val participants: List<String>,
        @field: NotNull
        val expenseDate: LocalDate
    )

    data class UpdateExpenseRequest(
        val id: String,
        val description: String,
        val totalExpense: BigDecimal,
        val splitType: String,
        val splits: List<RequestSplitDto>,
        val currencyCode: String,
        val paidByUserIds: List<String>,
        val participants: List<String>,
        val groupId: String?,
    )

    data class RequestSplitDto(
        val owedByUserId: String,
        val owedAmount: Double,
        val owedToUserId: String
    )

    data class ResponseSplitDto(
        val id: String,
        val owedByUserId: String,
        val owedAmount: Double,
        val owedToUserId: String
    )

    data class ExpenseResponse(
        val id: String,
        val description: String?,
        val totalExpense: Double,
        val splits: List<ResponseSplitDto>,
        val currencyCode: String,
        val paidByUserIds: List<String>,
        val participants: List<String>,
        val groupId: String?,
        val expenseDate: String,
        val splitType: String,
        val createdByUserId: String,
        val deleted: Boolean
    )

    data class GetAllExpenseRequest(
        val currentUserId: String
    )

    @PostMapping("/add")
    fun saveExpense(@Valid @RequestBody addExpenseRequest: AddExpenseRequest): ExpenseResponse {
        val newExpense = Expense(
            groupId = if (!addExpenseRequest.groupId.isNullOrBlank()) {
                ObjectId(addExpenseRequest.groupId)
            } else {
                null
            },
            createdByUserId = ObjectId(addExpenseRequest.createdByUserId),
            totalExpense = addExpenseRequest.totalExpense.toBigDecimal(),
            description = addExpenseRequest.description,
            splitType = addExpenseRequest.splitType,
            currencyCode = addExpenseRequest.currencyCode,
            paidByUserIds = addExpenseRequest.paidByUserIds.map { user ->
                ObjectId(user)
            },
            participants = addExpenseRequest.participants.map { ObjectId(it) },
            createdAt = Instant.now(),
            splits = addExpenseRequest.splits.map { it ->
                Split(
                    owedByUserId = ObjectId(it.owedByUserId),
                    owedAmount = it.owedAmount.toBigDecimal(),
                    owedToUserId = ObjectId(it.owedToUserId)
                )
            },
            expenseDate = addExpenseRequest.expenseDate
        )
        expenseRepository.save(
            newExpense
        )

        return ExpenseResponse(
            id = newExpense.id.toHexString(),
            description = newExpense.description,
            totalExpense = newExpense.totalExpense.toDouble(),
            currencyCode = newExpense.currencyCode,
            splits = newExpense.splits.map { split ->
                ResponseSplitDto(
                    id = split.id.toHexString(),
                    owedByUserId = split.owedByUserId.toHexString(),
                    owedAmount = split.owedAmount.toDouble(),
                    owedToUserId = split.owedToUserId.toHexString()
                )
            },
            expenseDate = newExpense.expenseDate.toString(),
            participants = newExpense.participants.map { participant ->
                participant.toHexString()
            },
            splitType = newExpense.splitType,
            paidByUserIds = newExpense.paidByUserIds.map { user ->
                user.toHexString()
            },
            deleted = newExpense.isDeleted,
            createdByUserId = newExpense.createdByUserId.toHexString(),
            groupId = newExpense.groupId?.toHexString()
        )
    }
    @DeleteMapping("/delete/{id}")
    fun deleteExpense(@PathVariable id: String) {
        val expenseToDelete = expenseRepository.findExpenseById(ObjectId(id))
        if(expenseToDelete != null) {
            balanceService.reverseSplits(splits = expenseToDelete.splits)
            expenseRepository.deleteById(ObjectId(id))
        }
        else{
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found")
        }

    }

    @PostMapping("/update")
    fun updateExpense(@Valid @RequestBody updateExpenseRequest: UpdateExpenseRequest): ExpenseResponse {
        val expenseToUpdate = expenseRepository.findExpenseById(ObjectId(updateExpenseRequest.id))
        if (expenseToUpdate != null) {
            updateExpenseRequest.description.let { expenseToUpdate.description = it }
            updateExpenseRequest.totalExpense.let { expenseToUpdate.totalExpense = it }
            updateExpenseRequest.splitType.let { expenseToUpdate.splitType = it }
            updateExpenseRequest.currencyCode.let { expenseToUpdate.currencyCode = it }
            updateExpenseRequest.paidByUserIds.let { paidByIds -> expenseToUpdate.paidByUserIds = paidByIds.map { ObjectId(it) } }
            updateExpenseRequest.participants.let { expenseToUpdate.participants = it.map { pId -> ObjectId(pId) } }
            if(!updateExpenseRequest.groupId.isNullOrBlank()) {
                updateExpenseRequest.groupId.let { expenseToUpdate.groupId = ObjectId(it) }
            }
            updateExpenseRequest.splits.let { splits ->
                expenseToUpdate.splits = splits.map { split ->
                    Split(
                        owedByUserId = ObjectId(split.owedByUserId),
                        owedAmount = split.owedAmount.toBigDecimal(),
                        owedToUserId = ObjectId(split.owedToUserId)
                    )
                }
            }
            expenseRepository.save(expenseToUpdate)

            return ExpenseResponse(
                id = expenseToUpdate.id.toHexString() ?: "",
                description = expenseToUpdate.description,
                totalExpense = expenseToUpdate.totalExpense.toDouble(),
                currencyCode = expenseToUpdate.currencyCode,
                splits = expenseToUpdate.splits.map { split ->
                    ResponseSplitDto(
                        id = split.id.toHexString(),
                        owedByUserId = split.owedByUserId.toHexString(),
                        owedAmount = split.owedAmount.toDouble(),
                        owedToUserId = split.owedToUserId.toHexString()
                    )
                },
                expenseDate = expenseToUpdate.expenseDate.toString(),
                splitType = expenseToUpdate.splitType,
                participants = expenseToUpdate.participants.map { participant ->
                    participant.toHexString()
                },
                paidByUserIds = expenseToUpdate.paidByUserIds.map { user ->
                    user.toHexString()
                },
                createdByUserId = expenseToUpdate.createdByUserId.toHexString(),
                deleted = expenseToUpdate.isDeleted,
                groupId = expenseToUpdate.groupId?.toHexString(),
            )
        }
        else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found")
        }
    }

    @PostMapping("/getAll")
    fun getAllExpenses(@Valid @RequestBody getAllExpenseRequest: GetAllExpenseRequest): List<ExpenseResponse> {

        val expenses = expenseRepository.findExpensesByUserId(ObjectId(getAllExpenseRequest.currentUserId))

        if (expenses.isNotEmpty()) {
            return expenses.map { expense ->
                ExpenseResponse(
                    id = expense.id.toHexString(),
                    description = expense.description,
                    totalExpense = expense.totalExpense.toDouble(),
                    currencyCode = expense.currencyCode,
                    splitType = expense.splitType,
                    splits = expense.splits.map { split ->
                        ResponseSplitDto(
                            id = split.id.toHexString(),
                            owedByUserId = split.owedByUserId.toHexString(),
                            owedAmount = split.owedAmount.toDouble(),
                            owedToUserId = split.owedToUserId.toHexString()
                        )
                    },
                    paidByUserIds = expense.paidByUserIds.map { userId ->
                        userId.toHexString()
                    },
                    expenseDate = expense.expenseDate.toString(),
                    participants = expense.participants.map { participant ->
                        participant.toHexString()
                    },
                    deleted = expense.isDeleted,
                    createdByUserId = expense.createdByUserId.toHexString(),
                    groupId = expense.groupId?.toHexString()
                )
            }
        }else{
            return emptyList()
        }

    }

}