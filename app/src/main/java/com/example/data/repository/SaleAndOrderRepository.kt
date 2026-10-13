package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.Sale
import com.example.data.model.SalesReturn
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SaleAndOrderRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = FirebaseAuth.getInstance()

    fun observeSales(): Flow<List<Sale>> = callbackFlow {
        val reg = db.collection("sales").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "sales")
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(Sale::class.java) } ?: emptyList()
            trySend(list.sortedByDescending { it.createdAt?.seconds ?: 0L })
        }
        awaitClose { reg.remove() }
    }

    suspend fun completePosSale(sale: Sale): Result<String> {
        return try {
            val saleDocRef = db.collection("sales").document()
            val saleId = saleDocRef.id
            val invoiceNo = if (sale.invoiceNumber.isBlank()) "ILAM-${System.currentTimeMillis() % 1000000}" else sale.invoiceNumber

            db.runTransaction { transaction ->
                // 1. Verify stock for each item
                for (item in sale.items) {
                    val prodRef = db.collection("products").document(item.productId)
                    val prodDoc = transaction.get(prodRef)
                    if (prodDoc.exists()) {
                        val currentStock = prodDoc.getLong("currentStock") ?: 0L
                        if (currentStock < item.quantity) {
                            throw IllegalStateException("Insufficient stock for ${item.productName}: available $currentStock, requested ${item.quantity}")
                        }
                        transaction.update(prodRef, "currentStock", currentStock - item.quantity)
                    }
                }

                // 2. Write sale document
                val payload = mapOf(
                    "invoiceNumber" to invoiceNo,
                    "customerId" to sale.customerId,
                    "customerName" to sale.customerName,
                    "customerPhone" to sale.customerPhone,
                    "items" to sale.items,
                    "subtotal" to sale.subtotal,
                    "discount" to sale.discount,
                    "total" to sale.total,
                    "paidAmount" to sale.paidAmount,
                    "dueAmount" to sale.dueAmount,
                    "paymentMethod" to sale.paymentMethod,
                    "staffId" to sale.staffId,
                    "staffName" to sale.staffName,
                    "status" to "COMPLETED",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                transaction.set(saleDocRef, payload)

                // 3. Update customer due if customerId provided
                if (sale.customerId.isNotBlank()) {
                    val custRef = db.collection("customers").document(sale.customerId)
                    transaction.update(
                        custRef,
                        mapOf(
                            "totalPurchases" to FieldValue.increment(sale.total),
                            "totalPaid" to FieldValue.increment(sale.paidAmount),
                            "totalDue" to FieldValue.increment(sale.dueAmount),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()

            Result.success(invoiceNo)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "sales")
            Result.failure(e)
        }
    }

    suspend fun processReturn(
        saleId: String,
        invoiceNumber: String,
        customerId: String,
        itemsToReturn: List<CartItem>,
        refundAmount: Double,
        reason: String,
        staffId: String,
        staffName: String
    ): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                // Restore stock for returned items
                for (item in itemsToReturn) {
                    val prodRef = db.collection("products").document(item.productId)
                    val prodDoc = transaction.get(prodRef)
                    if (prodDoc.exists()) {
                        val currentStock = prodDoc.getLong("currentStock") ?: 0L
                        transaction.update(prodRef, "currentStock", currentStock + item.quantity)
                    }
                }

                // Mark sale status
                val saleRef = db.collection("sales").document(saleId)
                transaction.update(saleRef, "status", "RETURNED", "updatedAt", FieldValue.serverTimestamp())

                // Adjust customer due/purchases if customerId present
                if (customerId.isNotBlank()) {
                    val custRef = db.collection("customers").document(customerId)
                    transaction.update(
                        custRef,
                        mapOf(
                            "totalPurchases" to FieldValue.increment(-refundAmount),
                            "totalPaid" to FieldValue.increment(-refundAmount),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                // Create return record
                val returnDoc = db.collection("returns").document()
                val returnData = mapOf(
                    "saleId" to saleId,
                    "invoiceNumber" to invoiceNumber,
                    "customerId" to customerId,
                    "items" to itemsToReturn,
                    "totalRefundAmount" to refundAmount,
                    "reason" to reason,
                    "staffId" to staffId,
                    "staffName" to staffName,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                transaction.set(returnDoc, returnData)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "sales/$saleId")
            Result.failure(e)
        }
    }

    fun observeOrders(): Flow<List<Order>> = callbackFlow {
        val reg = db.collection("orders").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "orders")
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(Order::class.java) } ?: emptyList()
            trySend(list.sortedByDescending { it.createdAt?.seconds ?: 0L })
        }
        awaitClose { reg.remove() }
    }

    fun observeCustomerOrders(customerId: String): Flow<List<Order>> = callbackFlow {
        val reg = db.collection("orders")
            .whereEqualTo("customerId", customerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "orders")
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(Order::class.java) } ?: emptyList()
                trySend(list.sortedByDescending { it.createdAt?.seconds ?: 0L })
            }
        awaitClose { reg.remove() }
    }

    suspend fun placeOnlineOrder(order: Order): Result<String> {
        return try {
            val orderRef = db.collection("orders").document()
            val orderNo = "ORD-${System.currentTimeMillis() % 1000000}"

            db.runTransaction { transaction ->
                // Check stock
                for (item in order.items) {
                    val prodRef = db.collection("products").document(item.productId)
                    val prodDoc = transaction.get(prodRef)
                    if (prodDoc.exists()) {
                        val currentStock = prodDoc.getLong("currentStock") ?: 0L
                        if (currentStock < item.quantity) {
                            throw IllegalStateException("Product ${item.productName} is out of stock.")
                        }
                        // Reserve stock
                        transaction.update(prodRef, "currentStock", currentStock - item.quantity)
                    }
                }

                val payload = mapOf(
                    "orderNumber" to orderNo,
                    "customerId" to order.customerId,
                    "customerName" to order.customerName,
                    "customerPhone" to order.customerPhone,
                    "deliveryAddress" to order.deliveryAddress,
                    "deliveryArea" to order.deliveryArea,
                    "deliveryMethod" to order.deliveryMethod,
                    "courierName" to order.courierName,
                    "trackingNumber" to order.trackingNumber,
                    "items" to order.items,
                    "subtotal" to order.subtotal,
                    "discount" to order.discount,
                    "deliveryCharge" to order.deliveryCharge,
                    "total" to order.total,
                    "paymentStatus" to order.paymentStatus,
                    "orderStatus" to "Pending",
                    "notes" to order.notes,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                transaction.set(orderRef, payload)
            }.await()

            Result.success(orderNo)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "orders")
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        courierName: String = "",
        trackingNumber: String = ""
    ) {
        val doc = db.collection("orders").document(orderId).get().await()
        if (!doc.exists()) return

        val order = doc.toObject(Order::class.java) ?: return
        val oldStatus = order.orderStatus

        // If order cancelled or returned from active status, restore stock!
        if ((newStatus == "Cancelled" || newStatus == "Returned") && oldStatus != "Cancelled" && oldStatus != "Returned") {
            for (item in order.items) {
                db.collection("products").document(item.productId).update(
                    "currentStock", FieldValue.increment(item.quantity.toLong())
                ).await()
            }
        }

        val updates = mutableMapOf<String, Any>(
            "orderStatus" to newStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (courierName.isNotBlank()) updates["courierName"] = courierName
        if (trackingNumber.isNotBlank()) updates["trackingNumber"] = trackingNumber

        db.collection("orders").document(orderId).update(updates).await()
    }
}
