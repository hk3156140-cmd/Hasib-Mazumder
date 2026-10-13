package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CartItem(
    val productId: String = "",
    val productName: String = "",
    val sku: String = "",
    val size: String = "",
    val color: String = "",
    val unitPrice: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val quantity: Int = 1,
    val discount: Double = 0.0
) {
    val subtotal: Double
        get() = (unitPrice * quantity) - discount
}

@IgnoreExtraProperties
data class Sale(
    @DocumentId val id: String = "",
    val invoiceNumber: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val paymentMethod: String = "CASH", // CASH, BKASH, OTHER
    val staffId: String = "",
    val staffName: String = "",
    val status: String = "COMPLETED", // COMPLETED, RETURNED, CANCELLED
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val orderNumber: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val deliveryArea: String = "",
    val deliveryMethod: String = "COURIER", // STORE_PICKUP, COURIER
    val courierName: String = "",
    val trackingNumber: String = "",
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val total: Double = 0.0,
    val paymentStatus: String = "UNPAID", // UNPAID, PAID
    val paymentMethod: String = "COD", // COD, BKASH, CARD
    val transactionId: String = "",
    val orderStatus: String = "Pending", // Pending, Confirmed, Preparing, Ready, Courier Booked, Picked Up, In Transit, Delivered, Cancelled, Returned
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SalesReturn(
    @DocumentId val id: String = "",
    val saleId: String = "",
    val invoiceNumber: String = "",
    val customerId: String = "",
    val items: List<CartItem> = emptyList(),
    val totalRefundAmount: Double = 0.0,
    val reason: String = "",
    val staffId: String = "",
    val staffName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
