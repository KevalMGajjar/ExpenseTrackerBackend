package com.example.splitwiseclonebackend.models

import org.bson.types.ObjectId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Document(collection = "expense")
data class Expense(
    @Id val id: ObjectId = ObjectId(),
    @Field("group_id") var groupId: ObjectId? = null,
    @Field("created_by_user_id")val createdByUserId: ObjectId,
    @Field("total_expense", targetType = FieldType.DECIMAL128)  var totalExpense: BigDecimal,
    @Field("description") var description: String?,
    @Field("split") var splitType: String,
    @Field("Splits") var splits: List<Split>,
    val isDeleted: Boolean = false,
    @Field("currency_code") var currencyCode: String,
    var paidByUserIds: List<ObjectId>,
    var participants: List<ObjectId>,
    @CreatedDate @Field("created_at") val createdAt: Instant?,
    @LastModifiedDate @Field("updated_at") val updatedAt: Instant? = null,
    @Field("expense_date") val expenseDate: LocalDate
)

data class Split(
    @Id val id: ObjectId = ObjectId(),
    @Field("owed_by_which_user") val owedByUserId: ObjectId,
    @Field("owed_amount", targetType = FieldType.DECIMAL128) val owedAmount: BigDecimal,
    @Field("owed_to_user_id") val owedToUserId: ObjectId,
)
