package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val nameEn: String = "",
    val nameBn: String = "",
    val subcategories: List<String> = emptyList(),
    val isOnlineVisible: Boolean = true,
    val orderIndex: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val qrCode: String = "",
    val category: String = "",
    val subcategory: String = "",
    val description: String = "",
    val photoUrl: String = "",
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val discount: Double = 0.0,
    val sizes: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val currentStock: Int = 0,
    val minimumStock: Int = 5,
    val supplierId: String = "",
    val supplierName: String = "",
    val isArchived: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val finalPrice: Double
        get() = if (discount > 0) (sellingPrice - discount).coerceAtLeast(0.0) else sellingPrice

    val isLowStock: Boolean
        get() = currentStock in 1..minimumStock

    val isOutOfStock: Boolean
        get() = currentStock <= 0
}
