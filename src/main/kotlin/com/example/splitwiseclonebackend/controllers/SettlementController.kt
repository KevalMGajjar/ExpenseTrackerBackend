package com.example.splitwiseclonebackend.controllers

import com.example.splitwiseclonebackend.models.Settlement
import com.example.splitwiseclonebackend.repository.SettlementRepository
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@RestController
@RequestMapping("/settlements")
class SettlementController(private val settlementRepository: SettlementRepository) {

    data class AddSettlementRequest(
        val groupId: String?,
        val paidFromUserId: String,
        val paidToUserId: String,
        val amount: BigDecimal,
        val currencyCode: String,
        val settlementDate: LocalDate
        )

    @PostMapping("/add")
    fun save(@Valid @RequestBody addRequest: AddSettlementRequest) {

        settlementRepository.save(
            Settlement(
                groupId = ObjectId(addRequest.groupId),
                paidToUserId = ObjectId(addRequest.paidToUserId),
                paidFromUserId = ObjectId(addRequest.paidFromUserId),
                amount = addRequest.amount,
                currencyCode = addRequest.currencyCode,
                createdAt = Instant.now(),
                settledAt = addRequest.settlementDate
            )
        )
    }


}