package com.example.splitwiseclonebackend.services

import com.example.splitwiseclonebackend.models.Expense
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

data class Transaction(val from: ObjectId, val to: ObjectId, val amount: BigDecimal)

@Service
class DebtSimplificationService {

    fun simplifyDebts(expenses: List<Expense>): List<Transaction> {
        // FIX: Use BigDecimal for all financial calculations to avoid floating-point errors.
        val balances = mutableMapOf<ObjectId, BigDecimal>()

        // 1. Calculate the net balance for each participant.
        expenses.forEach { expense ->
            expense.splits.forEach { split ->
                val owedBy = split.owedByUserId
                val owedTo = split.owedToUserId
                val amount = split.owedAmount

                balances[owedBy] = (balances[owedBy] ?: BigDecimal.ZERO).subtract(amount)
                balances[owedTo] = (balances[owedTo] ?: BigDecimal.ZERO).add(amount)
            }
        }

        // 2. Separate into debtors (negative balance) and creditors (positive balance).
        val debtors = balances.filterValues { it < BigDecimal.ZERO }.toMutableMap()
        val creditors = balances.filterValues { it > BigDecimal.ZERO }.toMutableMap()

        val transactions = mutableListOf<Transaction>()
        val tolerance = BigDecimal("0.01") // A small tolerance for comparisons

        // 3. Settle debts iteratively.
        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            val debtorEntry = debtors.entries.first()
            val creditorEntry = creditors.entries.first()

            val debtor = debtorEntry.key
            val creditor = creditorEntry.key

            val debtAmount = debtorEntry.value.abs()
            val creditAmount = creditorEntry.value

            val settlementAmount = debtAmount.min(creditAmount)

            transactions.add(Transaction(from = debtor, to = creditor, amount = settlementAmount.setScale(2, RoundingMode.HALF_UP)))

            // Update balances
            debtors[debtor] = debtors[debtor]!!.add(settlementAmount)
            creditors[creditor] = creditors[creditor]!!.subtract(settlementAmount)

            // Remove if settled (using a tolerance check)
            if (debtors[debtor]!!.abs() < tolerance) {
                debtors.remove(debtor)
            }
            if (creditors[creditor]!! < tolerance) {
                creditors.remove(creditor)
            }
        }

        return transactions
    }
}