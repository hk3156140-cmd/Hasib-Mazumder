package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val totalPurchases: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalDue: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Supplier(
    @DocumentId val id: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val totalPurchase: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalDue: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val date: String = "",
    val category: String = "Other", // Rent, Electricity, Internet, Transport, Packaging, Marketing, Salary, Other
    val amount: Double = 0.0,
    val description: String = "",
    val receiptUrl: String = "",
    val addedById: String = "",
    val addedByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DailyLedger(
    @DocumentId val id: String = "", // formatted YYYY-MM-DD
    val date: String = "",
    val openingCash: Double = 0.0,
    val cashSales: Double = 0.0,
    val customerPayments: Double = 0.0,
    val expenses: Double = 0.0,
    val withdrawals: Double = 0.0,
    val closingCash: Double = 0.0,
    val totalSales: Double = 0.0,
    val totalDiscount: Double = 0.0,
    val totalDue: Double = 0.0,
    val estimatedProfit: Double = 0.0,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val action: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val actorEmail: String = "",
    val targetId: String = "",
    val targetType: String = "", // USER, PRODUCT, SALE, EXPENSE, SETTINGS, etc.
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BusinessSettings(
    @DocumentId val id: String = "main",
    val businessName: String = "ILAM",
    val phone: String = "+880 1700-000000",
    val address: String = "Banani, Dhaka, Bangladesh",
    val currency: String = "৳",
    val insideDhakaDeliveryCharge: Double = 70.0,
    val outsideDhakaDeliveryCharge: Double = 130.0,
    val storePickupAvailable: Boolean = true,
    val bkashNumber: String = "01700000000",
    val cashOnDeliveryEnabled: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Transaction(
    @DocumentId val id: String = "",
    val transactionNumber: String = "",
    val type: String = "SALE", // SALE, CUSTOMER_DUE_PAYMENT, SUPPLIER_PAYMENT, EXPENSE, REFUND, CAPITAL_INJECTION, WITHDRAWAL
    val referenceType: String = "", // SALE, ORDER, CUSTOMER, SUPPLIER, EXPENSE
    val referenceId: String = "",
    val invoiceNumber: String = "",
    val amount: Double = 0.0,
    val paymentMethod: String = "CASH", // CASH, BKASH, BANK_TRANSFER, CARD, OTHER
    val trxId: String = "",
    val payerId: String = "",
    val payerName: String = "",
    val payeeId: String = "",
    val payeeName: String = "",
    val staffId: String = "",
    val staffName: String = "",
    val notes: String = "",
    val status: String = "COMPLETED", // COMPLETED, PENDING, FAILED, VOID
    @ServerTimestamp val createdAt: Timestamp? = null
)
