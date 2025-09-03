package com.example.splitwiseclonebackend.services

import com.example.splitwiseclonebackend.models.Split
import com.example.splitwiseclonebackend.repository.FriendsRepository
import org.apache.commons.logging.Log
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class BalanceService(private val friendsRepository: FriendsRepository) {

    @Transactional
    fun processSplits(splits: List<SplitDto>) {
        splits.forEach { split ->
            println("Split: $split")
            val friendRecord1 = friendsRepository.findByFriendIdAndCurrentUserId(ObjectId(split.owedToUserId), ObjectId(split.owedByUserId))
            friendRecord1.let {
                it.balanceWithUser = it.balanceWithUser?.minus(split.owedAmount)
                println("Balance: ${it.balanceWithUser}")
                friendsRepository.save(it)
            }

            val friendRecord2 = friendsRepository.findByFriendIdAndCurrentUserId(ObjectId(split.owedByUserId), ObjectId(split.owedToUserId))
            friendRecord2.let {
                it.balanceWithUser = it.balanceWithUser?.plus(split.owedAmount)
                friendsRepository.save(it)
            }
        }
    }

    @Transactional
    fun reverseSplits(splits: List<Split>) {
        splits.forEach { split ->
            // In the original split, 'owedByUserId' owed 'owedToUserId'.
            // To reverse this, we do the opposite math.

            // Find the record for the person who WAS OWED money.
            // Their balance with the other person should DECREASE.
            val personOwedRecord = friendsRepository.findByFriendIdAndCurrentUserId(
                split.owedByUserId,
                split.owedToUserId
            )
            personOwedRecord.let {
                it.balanceWithUser = it.balanceWithUser?.minus(split.owedAmount.toDouble())
                friendsRepository.save(it)
            }

            // Find the record for the person who OWED money.
            // Their balance with the other person should INCREASE (back towards zero).
            val personWhoOwesRecord = friendsRepository.findByFriendIdAndCurrentUserId(
                split.owedToUserId,
                split.owedByUserId
            )
            personWhoOwesRecord.let {
                it.balanceWithUser = it.balanceWithUser?.plus(split.owedAmount.toDouble())
                friendsRepository.save(it)
            }
        }
    }
}

data class SplitDto(
    val owedByUserId: String,
    val owedAmount: Double,
    val owedToUserId: String
)