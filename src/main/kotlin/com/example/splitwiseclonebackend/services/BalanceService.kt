package com.example.splitwiseclonebackend.services

import com.example.splitwiseclonebackend.models.Split
import com.example.splitwiseclonebackend.repository.FriendsRepository
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal

@Service
@Transactional
class BalanceService(private val friendsRepository: FriendsRepository) {

    fun processSplits(splits: List<SplitDto>) {
        splits.forEach { split ->
            // FIX: Convert the incoming Double to BigDecimal immediately for safe calculations.
            val amount = BigDecimal.valueOf(split.owedAmount)

            // 1. Update the debtor's record.
            // From the debtor's perspective, they owe more, so their balance decreases (becomes more negative).
            friendsRepository.findByFriendIdAndCurrentUserId(ObjectId(split.owedToUserId), ObjectId(split.owedByUserId))?.let { debtorFriendRecord ->
                // Use .subtract() for BigDecimal math.
                debtorFriendRecord.balanceWithUser = (debtorFriendRecord.balanceWithUser ?: BigDecimal.ZERO).subtract(amount)
                friendsRepository.save(debtorFriendRecord)
            }

            // 2. Update the creditor's record.
            // From the creditor's perspective, they are owed more, so their balance increases (becomes more positive).
            friendsRepository.findByFriendIdAndCurrentUserId(ObjectId(split.owedByUserId), ObjectId(split.owedToUserId))?.let { creditorFriendRecord ->
                // Use .add() for BigDecimal math.
                creditorFriendRecord.balanceWithUser = (creditorFriendRecord.balanceWithUser ?: BigDecimal.ZERO).add(amount)
                friendsRepository.save(creditorFriendRecord)
            }
        }
    }

    fun reverseSplits(splits: List<Split>) {
        splits.forEach { split ->
            // The owedAmount in the Split entity is already a BigDecimal, so no conversion is needed.
            val amount = split.owedAmount

            // 1. Update the original creditor's record (DECREASE their balance).
            friendsRepository.findByFriendIdAndCurrentUserId(split.owedByUserId, split.owedToUserId)?.let { creditorFriendRecord ->
                creditorFriendRecord.balanceWithUser = (creditorFriendRecord.balanceWithUser ?: BigDecimal.ZERO).subtract(amount)
                friendsRepository.save(creditorFriendRecord)
            }

            // 2. Update the original debtor's record (INCREASE their balance).
            friendsRepository.findByFriendIdAndCurrentUserId(split.owedToUserId, split.owedByUserId)?.let { debtorFriendRecord ->
                debtorFriendRecord.balanceWithUser = (debtorFriendRecord.balanceWithUser ?: BigDecimal.ZERO).add(amount)
                friendsRepository.save(debtorFriendRecord)
            }
        }
    }

    fun settleUp(payerId: ObjectId, receiverId: ObjectId, amount: Double) {
        // FIX: Convert the incoming Double to BigDecimal immediately.
        val settlementAmount = BigDecimal.valueOf(amount)

        // 1. Update the Payer's Record:
        // If I pay you, my debt decreases, so my balance with you INCREASES (moves from negative towards zero).
        val payerFriendRecord = friendsRepository.findByFriendIdAndCurrentUserId(receiverId, payerId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship record not found for payer.")

        payerFriendRecord.balanceWithUser = (payerFriendRecord.balanceWithUser ?: BigDecimal.ZERO).add(settlementAmount)
        friendsRepository.save(payerFriendRecord)

        // 2. Update the Receiver's Record:
        // If I pay you, your credit towards me decreases, so your balance with me DECREASES (moves from positive towards zero).
        val receiverFriendRecord = friendsRepository.findByFriendIdAndCurrentUserId(payerId, receiverId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship record not found for receiver.")

        receiverFriendRecord.balanceWithUser = (receiverFriendRecord.balanceWithUser ?: BigDecimal.ZERO).subtract(settlementAmount)
        friendsRepository.save(receiverFriendRecord)
    }
}

data class SplitDto(
    val owedByUserId: String,
    val owedAmount: Double,
    val owedToUserId: String
)