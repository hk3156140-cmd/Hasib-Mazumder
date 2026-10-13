package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.BusinessSettings
import com.example.data.model.Customer
import com.example.data.model.DailyLedger
import com.example.data.model.Expense
import com.example.data.model.Supplier
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class BusinessRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    // --- CUSTOMERS ---
    fun observeCustomers(): Flow<List<Customer>> = callbackFlow {
        val reg = db.collection("customers").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "customers")
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(Customer::class.java) } ?: emptyList()
            trySend(list.sortedByDescending { it.totalDue })
        }
        awaitClose { reg.remove() }
    }

    suspend fun addCustomer(customer: Customer): String {
        val docRef = db.collection("customers").document()
        val data = mapOf(
            "name" to customer.name,
            "phone" to customer.phone,
            "email" to customer.email,
            "address" to customer.address,
            "totalPurchases" to customer.totalPurchases,
            "totalPaid" to customer.totalPaid,
            "totalDue" to customer.totalDue,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
        return docRef.id
    }

    suspend fun recordCustomerPayment(customerId: String, paymentAmount: Double) {
        db.collection("customers").document(customerId).update(
            mapOf(
                "totalPaid" to FieldValue.increment(paymentAmount),
                "totalDue" to FieldValue.increment(-paymentAmount),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // --- SUPPLIERS ---
    fun observeSuppliers(): Flow<List<Supplier>> = callbackFlow {
        val reg = db.collection("suppliers").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "suppliers")
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(Supplier::class.java) } ?: emptyList()
            trySend(list.sortedByDescending { it.totalDue })
        }
        awaitClose { reg.remove() }
    }

    suspend fun addSupplier(supplier: Supplier): String {
        val docRef = db.collection("suppliers").document()
        val data = mapOf(
            "name" to supplier.name,
            "phone" to supplier.phone,
            "address" to supplier.address,
            "totalPurchase" to supplier.totalPurchase,
            "totalPaid" to supplier.totalPaid,
            "totalDue" to supplier.totalDue,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
        return docRef.id
    }

    suspend fun recordSupplierPayment(supplierId: String, paymentAmount: Double) {
        db.collection("suppliers").document(supplierId).update(
            mapOf(
                "totalPaid" to FieldValue.increment(paymentAmount),
                "totalDue" to FieldValue.increment(-paymentAmount),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // --- EXPENSES ---
    fun observeExpenses(): Flow<List<Expense>> = callbackFlow {
        val reg = db.collection("expenses").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "expenses")
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(Expense::class.java) } ?: emptyList()
            trySend(list.sortedByDescending { it.createdAt?.seconds ?: 0L })
        }
        awaitClose { reg.remove() }
    }

    suspend fun addExpense(expense: Expense): String {
        val docRef = db.collection("expenses").document()
        val data = mapOf(
            "date" to expense.date,
            "category" to expense.category,
            "amount" to expense.amount,
            "description" to expense.description,
            "receiptUrl" to expense.receiptUrl,
            "addedById" to expense.addedById,
            "addedByName" to expense.addedByName,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
        return docRef.id
    }

    // --- DAILY LEDGER ---
    suspend fun getDailyLedger(date: String): DailyLedger? {
        val doc = db.collection("daily_ledgers").document(date).get().await()
        return if (doc.exists()) doc.toObject(DailyLedger::class.java) else null
    }

    suspend fun saveDailyLedger(ledger: DailyLedger) {
        val data = mapOf(
            "date" to ledger.date,
            "openingCash" to ledger.openingCash,
            "cashSales" to ledger.cashSales,
            "customerPayments" to ledger.customerPayments,
            "expenses" to ledger.expenses,
            "withdrawals" to ledger.withdrawals,
            "closingCash" to ledger.closingCash,
            "totalSales" to ledger.totalSales,
            "totalDiscount" to ledger.totalDiscount,
            "totalDue" to ledger.totalDue,
            "estimatedProfit" to ledger.estimatedProfit,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("daily_ledgers").document(ledger.date).set(data).await()
    }

    // --- BUSINESS SETTINGS ---
    fun observeBusinessSettings(): Flow<BusinessSettings> = callbackFlow {
        val reg = db.collection("business_settings").document("main").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, "business_settings/main")
                close(error)
                return@addSnapshotListener
            }
            val settings = snapshot?.toObject(BusinessSettings::class.java) ?: BusinessSettings()
            trySend(settings)
        }
        awaitClose { reg.remove() }
    }

    suspend fun updateBusinessSettings(settings: BusinessSettings) {
        val data = mapOf(
            "businessName" to settings.businessName,
            "phone" to settings.phone,
            "address" to settings.address,
            "currency" to settings.currency,
            "insideDhakaDeliveryCharge" to settings.insideDhakaDeliveryCharge,
            "outsideDhakaDeliveryCharge" to settings.outsideDhakaDeliveryCharge,
            "storePickupAvailable" to settings.storePickupAvailable,
            "bkashNumber" to settings.bkashNumber,
            "cashOnDeliveryEnabled" to settings.cashOnDeliveryEnabled,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("business_settings").document("main").set(data).await()
    }

    // --- BACKUP & RESTORE ---
    suspend fun exportFullBackupJson(): String {
        val backup = JSONObject()
        val collections = listOf("categories", "products", "sales", "orders", "customers", "suppliers", "expenses", "users")
        for (col in collections) {
            val docs = db.collection(col).get().await()
            val arr = JSONArray()
            for (doc in docs.documents) {
                val obj = JSONObject(doc.data ?: emptyMap<String, Any>())
                obj.put("_docId", doc.id)
                arr.put(obj)
            }
            backup.put(col, arr)
        }
        backup.put("exportTime", System.currentTimeMillis())
        backup.put("system", "ILAM STORE MANAGER")
        return backup.toString(2)
    }

    suspend fun restoreBackupJson(jsonString: String) {
        val backup = JSONObject(jsonString)
        val collections = listOf("categories", "products", "sales", "orders", "customers", "suppliers", "expenses")
        for (col in collections) {
            if (backup.has(col)) {
                val arr = backup.getJSONArray(col)
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val docId = if (item.has("_docId")) item.getString("_docId") else null
                    val dataMap = mutableMapOf<String, Any?>()
                    val keys = item.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        if (k != "_docId") {
                            val v = item.get(k)
                            if (v != JSONObject.NULL) {
                                dataMap[k] = v
                            }
                        }
                    }
                    if (docId != null) {
                        db.collection(col).document(docId).set(dataMap).await()
                    } else {
                        db.collection(col).add(dataMap).await()
                    }
                }
            }
        }
    }
}
