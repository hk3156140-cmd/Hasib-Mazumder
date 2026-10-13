package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.Category
import com.example.data.model.Product
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProductAndCategoryRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeCategories(): Flow<List<Category>> = callbackFlow {
        val reg = db.collection("categories").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "categories")
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(Category::class.java) } ?: emptyList()
            trySend(list.sortedBy { it.orderIndex })
        }
        awaitClose { reg.remove() }
    }

    suspend fun seedDefaultCategoriesIfEmpty() {
        val current = db.collection("categories").limit(1).get().await()
        if (current.isEmpty) {
            val defaults = listOf(
                Category(
                    nameEn = "MEN",
                    nameBn = "মেন",
                    subcategories = listOf(
                        "T-Shirt", "Drop Shoulder T-Shirt", "Shirt", "Half Sleeve Shirt",
                        "Old Money Shirt", "Formal Shirt", "Pants", "Jeans",
                        "Trousers", "Hoodie", "Jacket", "Leather Jacket"
                    ),
                    orderIndex = 1
                ),
                Category(
                    nameEn = "WOMEN",
                    nameBn = "উইমেন",
                    subcategories = listOf(
                        "T-Shirt", "Shirt", "Top", "Dress", "Hoodie", "Jacket",
                        "Pants", "Jeans", "Shoes", "Loafer", "Boots"
                    ),
                    orderIndex = 2
                ),
                Category(
                    nameEn = "KIDS",
                    nameBn = "কিডস",
                    subcategories = listOf("Boys", "Girls"),
                    orderIndex = 3
                ),
                Category(
                    nameEn = "ACCESSORIES",
                    nameBn = "এক্সেসরিজ",
                    subcategories = listOf(
                        "Sneakers", "Shoes", "Loafer", "Boots", "Watch",
                        "Sunglasses", "Belt", "Wallet", "Cap", "Bag", "Other"
                    ),
                    orderIndex = 4
                ),
                Category(
                    nameEn = "WINTER COLLECTION",
                    nameBn = "উইন্টার কালেকশন",
                    subcategories = listOf("Hoodie", "Jacket", "Sweater", "Shawl", "Leather Jacket"),
                    orderIndex = 5
                )
            )

            for (cat in defaults) {
                val data = mapOf(
                    "nameEn" to cat.nameEn,
                    "nameBn" to cat.nameBn,
                    "subcategories" to cat.subcategories,
                    "isOnlineVisible" to cat.isOnlineVisible,
                    "orderIndex" to cat.orderIndex,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("categories").add(data).await()
            }
        }
    }

    suspend fun addCategory(nameEn: String, nameBn: String, subcategories: List<String>, isOnlineVisible: Boolean = true) {
        val data = mapOf(
            "nameEn" to nameEn,
            "nameBn" to nameBn,
            "subcategories" to subcategories,
            "isOnlineVisible" to isOnlineVisible,
            "orderIndex" to 10,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("categories").add(data).await()
    }

    suspend fun updateCategoryOnlineVisibility(categoryId: String, isVisible: Boolean) {
        db.collection("categories").document(categoryId).update("isOnlineVisible", isVisible).await()
    }

    fun observeProducts(includeArchived: Boolean = false): Flow<List<Product>> = callbackFlow {
        val reg = db.collection("products").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "products")
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(Product::class.java) } ?: emptyList()
            val filtered = if (includeArchived) list else list.filter { !it.isArchived }
            trySend(filtered.sortedByDescending { it.createdAt?.seconds ?: 0L })
        }
        awaitClose { reg.remove() }
    }

    suspend fun getProductById(id: String): Product? {
        val doc = db.collection("products").document(id).get().await()
        return if (doc.exists()) doc.toObject(Product::class.java) else null
    }

    suspend fun findProductByCode(code: String): Product? {
        val clean = code.trim()
        val bySku = db.collection("products").whereEqualTo("sku", clean).limit(1).get().await()
        if (!bySku.isEmpty) return bySku.documents.first().toObject(Product::class.java)

        val byBarcode = db.collection("products").whereEqualTo("barcode", clean).limit(1).get().await()
        if (!byBarcode.isEmpty) return byBarcode.documents.first().toObject(Product::class.java)

        val byQr = db.collection("products").whereEqualTo("qrCode", clean).limit(1).get().await()
        if (!byQr.isEmpty) return byQr.documents.first().toObject(Product::class.java)

        val byId = db.collection("products").document(clean).get().await()
        if (byId.exists()) return byId.toObject(Product::class.java)

        return null
    }

    suspend fun addProduct(product: Product): String {
        val docRef = db.collection("products").document()
        val generatedSku = if (product.sku.isBlank()) "ILAM-${System.currentTimeMillis() % 100000}" else product.sku
        val generatedBarcode = if (product.barcode.isBlank()) generatedSku else product.barcode
        val generatedQr = if (product.qrCode.isBlank()) "ILAM:$generatedSku" else product.qrCode

        val payload = mapOf(
            "name" to product.name,
            "sku" to generatedSku,
            "barcode" to generatedBarcode,
            "qrCode" to generatedQr,
            "category" to product.category,
            "subcategory" to product.subcategory,
            "description" to product.description,
            "photoUrl" to product.photoUrl,
            "purchasePrice" to product.purchasePrice,
            "sellingPrice" to product.sellingPrice,
            "discount" to product.discount,
            "sizes" to product.sizes,
            "colors" to product.colors,
            "currentStock" to product.currentStock,
            "minimumStock" to product.minimumStock,
            "supplierId" to product.supplierId,
            "supplierName" to product.supplierName,
            "isArchived" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        return docRef.id
    }

    suspend fun updateProduct(product: Product) {
        require(product.id.isNotBlank())
        val docRef = db.collection("products").document(product.id)
        val payload = mapOf(
            "name" to product.name,
            "sku" to product.sku,
            "barcode" to product.barcode,
            "qrCode" to product.qrCode,
            "category" to product.category,
            "subcategory" to product.subcategory,
            "description" to product.description,
            "photoUrl" to product.photoUrl,
            "purchasePrice" to product.purchasePrice,
            "sellingPrice" to product.sellingPrice,
            "discount" to product.discount,
            "sizes" to product.sizes,
            "colors" to product.colors,
            "currentStock" to product.currentStock,
            "minimumStock" to product.minimumStock,
            "supplierId" to product.supplierId,
            "supplierName" to product.supplierName,
            "isArchived" to product.isArchived,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(payload).await()
    }

    suspend fun archiveProduct(productId: String) {
        db.collection("products").document(productId).update(
            mapOf("isArchived" to true, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun restoreProduct(productId: String) {
        db.collection("products").document(productId).update(
            mapOf("isArchived" to false, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun restockProduct(productId: String, additionalStock: Int) {
        db.collection("products").document(productId).update(
            mapOf(
                "currentStock" to FieldValue.increment(additionalStock.toLong()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun seedInitialProductsIfEmpty() {
        val current = db.collection("products").limit(1).get().await()
        if (current.isEmpty) {
            val samples = listOf(
                Product(
                    name = "Signature Old Money Linen Shirt",
                    sku = "ILAM-OM-001",
                    barcode = "890123456781",
                    category = "MEN",
                    subcategory = "Old Money Shirt",
                    description = "Premium breathable Italian linen with mother-of-pearl buttons. Relaxed luxury fit.",
                    purchasePrice = 1400.0,
                    sellingPrice = 2850.0,
                    discount = 0.0,
                    sizes = listOf("M", "L", "XL"),
                    colors = listOf("Ivory White", "Sage Green", "Charcoal"),
                    currentStock = 18,
                    minimumStock = 4
                ),
                Product(
                    name = "Luxury Drop Shoulder Heavyweight T-Shirt",
                    sku = "ILAM-DS-002",
                    barcode = "890123456782",
                    category = "MEN",
                    subcategory = "Drop Shoulder T-Shirt",
                    description = "240 GSM combed cotton. Structured drape with minimalist embroidered ILAM monogram.",
                    purchasePrice = 750.0,
                    sellingPrice = 1650.0,
                    discount = 150.0,
                    sizes = listOf("S", "M", "L", "XL"),
                    colors = listOf("Midnight Black", "Off-White", "Mocha Brown"),
                    currentStock = 32,
                    minimumStock = 6
                ),
                Product(
                    name = "Tailored High-Waist Pleated Trousers",
                    sku = "ILAM-TR-003",
                    barcode = "890123456783",
                    category = "MEN",
                    subcategory = "Trousers",
                    description = "Bespoke drape wool-blend fabric with adjustable side tabs.",
                    purchasePrice = 1600.0,
                    sellingPrice = 3200.0,
                    discount = 0.0,
                    sizes = listOf("30", "32", "34", "36"),
                    colors = listOf("Camel", "Dark Grey", "Black"),
                    currentStock = 14,
                    minimumStock = 3
                ),
                Product(
                    name = "Silk Satin Slip Evening Dress",
                    sku = "ILAM-WD-004",
                    barcode = "890123456784",
                    category = "WOMEN",
                    subcategory = "Dress",
                    description = "Exquisite Japanese silk satin with subtle cowl neckline and graceful silhouette.",
                    purchasePrice = 2200.0,
                    sellingPrice = 4500.0,
                    discount = 300.0,
                    sizes = listOf("S", "M", "L"),
                    colors = listOf("Champagne Gold", "Emerald", "Jet Black"),
                    currentStock = 9,
                    minimumStock = 2
                ),
                Product(
                    name = "Handcrafted Full-Grain Leather Loafers",
                    sku = "ILAM-LF-005",
                    barcode = "890123456785",
                    category = "ACCESSORIES",
                    subcategory = "Loafer",
                    description = "Genuine calfskin leather with Blake stitch construction and memory foam insole.",
                    purchasePrice = 2800.0,
                    sellingPrice = 5800.0,
                    discount = 0.0,
                    sizes = listOf("40", "41", "42", "43", "44"),
                    colors = listOf("Cognac Tan", "Classic Black"),
                    currentStock = 8,
                    minimumStock = 2
                )
            )

            for (p in samples) {
                addProduct(p)
            }
        }
    }
}
