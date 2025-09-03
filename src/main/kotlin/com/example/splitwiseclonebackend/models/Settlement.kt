package com.example.splitwiseclonebackend.models

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Document(collection = "Settlements")
data class Settlement(
    @Id val id: ObjectId = ObjectId(),
    @Field("group_id") val groupId: ObjectId?,
    @Field("paid_from_user_id") val paidFromUserId: ObjectId,
    @Field("paid_to_user_id") val paidToUserId: ObjectId,
    @Field("amount") val amount: BigDecimal,
    @Field("currencyCode") val currencyCode: String,
    @Field("created_at") val createdAt: Instant,
    @Field("settled_at") val settledAt: LocalDate,
)
