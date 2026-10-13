package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuditLog
import com.example.data.model.BusinessSettings
import com.example.data.model.CartItem
import com.example.data.model.Category
import com.example.data.model.Customer
import com.example.data.model.DailyLedger
import com.example.data.model.Expense
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.Supplier
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.AuthAndUserRepository
import com.example.data.repository.BusinessRepository
import com.example.data.repository.ProductAndCategoryRepository
import com.example.data.repository.SaleAndOrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardMetrics(
    val todaySalesTotal: Double = 0.0,
    val todaySalesCount: Int = 0,
    val todayOrdersCount: Int = 0,
    val todayDueTotal: Double = 0.0,
    val todayExpensesTotal: Double = 0.0,
    val totalProductsCount: Int = 0,
    val totalStockCount: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val estimatedProfit: Double = 0.0
)

class MainViewModel(
    private val productRepo: ProductAndCategoryRepository,
    private val saleOrderRepo: SaleAndOrderRepository,
    private val businessRepo: BusinessRepository,
    private val authUserRepo: AuthAndUserRepository
) : ViewModel() {

    // --- REALTIME REPOSITORIES OBSERVATION ---
    val products: StateFlow<List<Product>> = productRepo.observeProducts(includeArchived = true)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = productRepo.observeCategories()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<Sale>> = saleOrderRepo.observeSales()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<Order>> = saleOrderRepo.observeOrders()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = businessRepo.observeCustomers()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<Supplier>> = businessRepo.observeSuppliers()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<Expense>> = businessRepo.observeExpenses()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val staffUsers: StateFlow<List<UserProfile>> = authUserRepo.observeAllStaffUsers()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = authUserRepo.observeAuditLogs()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val businessSettings: StateFlow<BusinessSettings> = businessRepo.observeBusinessSettings()
        .catch { emit(BusinessSettings()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessSettings())

    // --- POS CART STATE ---
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    // --- STATUS / BANNER MESSAGES ---
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                productRepo.seedDefaultCategoriesIfEmpty()
                productRepo.seedInitialProductsIfEmpty()
            } catch (ignored: Exception) {}
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    // --- POS CART ACTIONS ---
    fun addToCart(product: Product, size: String = "", color: String = "", qty: Int = 1) {
        if (product.currentStock <= 0) {
            _userMessage.value = "${product.name} is out of stock!"
            return
        }
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst {
            it.productId == product.id && it.size == size && it.color == color
        }
        if (index >= 0) {
            val existing = currentList[index]
            val newQty = existing.quantity + qty
            if (newQty > product.currentStock) {
                _userMessage.value = "Only ${product.currentStock} in stock!"
                return
            }
            currentList[index] = existing.copy(quantity = newQty)
        } else {
            currentList.add(
                CartItem(
                    productId = product.id,
                    productName = product.name,
                    sku = product.sku,
                    size = size.ifBlank { product.sizes.firstOrNull() ?: "Standard" },
                    color = color.ifBlank { product.colors.firstOrNull() ?: "Default" },
                    unitPrice = product.sellingPrice,
                    purchasePrice = product.purchasePrice,
                    quantity = qty,
                    discount = product.discount
                )
            )
        }
        _cartItems.value = currentList
    }

    fun updateCartItemQuantity(productId: String, size: String, color: String, newQty: Int) {
        if (newQty <= 0) {
            _cartItems.value = _cartItems.value.filterNot {
                it.productId == productId && it.size == size && it.color == color
            }
        } else {
            val prod = products.value.find { it.id == productId }
            if (prod != null && newQty > prod.currentStock) {
                _userMessage.value = "Stock limit reached (${prod.currentStock})!"
                return
            }
            _cartItems.value = _cartItems.value.map { item ->
                if (item.productId == productId && item.size == size && item.color == color) {
                    item.copy(quantity = newQty)
                } else item
            }
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun completePosSale(
        customerId: String,
        customerName: String,
        customerPhone: String,
        paymentMethod: String,
        overallDiscount: Double,
        paidAmount: Double,
        staffId: String,
        staffName: String,
        onSuccess: (Sale) -> Unit
    ) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            _userMessage.value = "Cart is empty"
            return
        }
        val subtotal = items.sumOf { it.unitPrice * it.quantity }
        val itemsDiscount = items.sumOf { it.discount * it.quantity }
        val totalDiscount = itemsDiscount + overallDiscount
        val total = (subtotal - totalDiscount).coerceAtLeast(0.0)
        val dueAmount = (total - paidAmount).coerceAtLeast(0.0)

        val sale = Sale(
            invoiceNumber = "ILAM-${System.currentTimeMillis() % 1000000}",
            customerId = customerId,
            customerName = customerName.ifBlank { "Walk-in Customer" },
            customerPhone = customerPhone,
            items = items,
            subtotal = subtotal,
            discount = totalDiscount,
            total = total,
            paidAmount = paidAmount,
            dueAmount = dueAmount,
            paymentMethod = paymentMethod,
            staffId = staffId,
            staffName = staffName,
            status = "COMPLETED"
        )

        viewModelScope.launch {
            val result = saleOrderRepo.completePosSale(sale)
            if (result.isSuccess) {
                val invoiceNumber = result.getOrThrow()
                clearCart()
                authUserRepo.logAudit("SALE_COMPLETED", "Sale completed: $invoiceNumber, Total: ৳$total", invoiceNumber, "SALE")
                _userMessage.value = "Sale successful! Invoice: $invoiceNumber"
                onSuccess(sale.copy(invoiceNumber = invoiceNumber))
            } else {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Sale failed"
            }
        }
    }

    fun processReturn(
        sale: Sale,
        reason: String,
        refundAmount: Double,
        staffId: String,
        staffName: String
    ) {
        viewModelScope.launch {
            val result = saleOrderRepo.processReturn(
                saleId = sale.id,
                invoiceNumber = sale.invoiceNumber,
                customerId = sale.customerId,
                itemsToReturn = sale.items,
                refundAmount = refundAmount,
                reason = reason,
                staffId = staffId,
                staffName = staffName
            )
            if (result.isSuccess) {
                authUserRepo.logAudit("SALE_RETURN", "Return processed for ${sale.invoiceNumber}", sale.id, "RETURN")
                _userMessage.value = "Return processed successfully!"
            } else {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Return failed"
            }
        }
    }

    // --- PRODUCT ACTIONS ---
    fun addProduct(product: Product, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val id = productRepo.addProduct(product)
                authUserRepo.logAudit("PRODUCT_ADDED", "Added product: ${product.name} (SKU: ${product.sku})", id, "PRODUCT")
                _userMessage.value = "Product added successfully!"
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = "Failed to add product: ${e.message}"
            }
        }
    }

    fun updateProduct(product: Product, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                productRepo.updateProduct(product)
                authUserRepo.logAudit("PRODUCT_EDITED", "Updated product: ${product.name} (SKU: ${product.sku})", product.id, "PRODUCT")
                _userMessage.value = "Product updated successfully!"
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = "Failed to update product: ${e.message}"
            }
        }
    }

    fun archiveProduct(productId: String) {
        viewModelScope.launch {
            try {
                productRepo.archiveProduct(productId)
                authUserRepo.logAudit("PRODUCT_ARCHIVED", "Archived product ID $productId", productId, "PRODUCT")
                _userMessage.value = "Product archived."
            } catch (e: Exception) {
                _userMessage.value = e.message
            }
        }
    }

    fun restoreProduct(productId: String) {
        viewModelScope.launch {
            try {
                productRepo.restoreProduct(productId)
                authUserRepo.logAudit("PRODUCT_RESTORED", "Restored product ID $productId", productId, "PRODUCT")
                _userMessage.value = "Product restored."
            } catch (e: Exception) {
                _userMessage.value = e.message
            }
        }
    }

    fun restockProduct(productId: String, amount: Int) {
        viewModelScope.launch {
            try {
                productRepo.restockProduct(productId, amount)
                authUserRepo.logAudit("PRODUCT_RESTOCKED", "Restocked $amount units for product ID $productId", productId, "PRODUCT")
                _userMessage.value = "Stock added: +$amount units"
            } catch (e: Exception) {
                _userMessage.value = e.message
            }
        }
    }

    // --- CATEGORY ACTIONS ---
    fun addCategory(nameEn: String, nameBn: String, subcategories: List<String>) {
        viewModelScope.launch {
            try {
                productRepo.addCategory(nameEn, nameBn, subcategories)
                _userMessage.value = "Category $nameEn added."
            } catch (e: Exception) {
                _userMessage.value = e.message
            }
        }
    }

    fun toggleCategoryOnlineVisibility(categoryId: String, isVisible: Boolean) {
        viewModelScope.launch {
            try {
                productRepo.updateCategoryOnlineVisibility(categoryId, isVisible)
            } catch (e: Exception) {
                _userMessage.value = e.message
            }
        }
    }

    // --- ONLINE ORDER ACTIONS ---
    fun placeOnlineOrder(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        deliveryArea: String,
        deliveryMethod: String,
        deliveryCharge: Double,
        notes: String,
        customerId: String = "",
        onSuccess: (String) -> Unit
    ) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            _userMessage.value = "Cart is empty"
            return
        }
        val subtotal = items.sumOf { it.unitPrice * it.quantity }
        val discount = items.sumOf { it.discount * it.quantity }
        val total = subtotal - discount + deliveryCharge

        val order = Order(
            customerId = customerId,
            customerName = customerName,
            customerPhone = customerPhone,
            deliveryAddress = deliveryAddress,
            deliveryArea = deliveryArea,
            deliveryMethod = deliveryMethod,
            items = items,
            subtotal = subtotal,
            discount = discount,
            deliveryCharge = deliveryCharge,
            total = total,
            paymentStatus = "UNPAID",
            orderStatus = "Pending",
            notes = notes
        )

        viewModelScope.launch {
            val result = saleOrderRepo.placeOnlineOrder(order)
            if (result.isSuccess) {
                val orderNo = result.getOrThrow()
                clearCart()
                _userMessage.value = "Order placed successfully! Order: $orderNo"
                onSuccess(orderNo)
            } else {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Order placement failed"
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String, courierName: String = "", trackingNumber: String = "") {
        viewModelScope.launch {
            try {
                saleOrderRepo.updateOrderStatus(orderId, newStatus, courierName, trackingNumber)
                authUserRepo.logAudit("ORDER_STATUS_CHANGED", "Order $orderId status changed to $newStatus", orderId, "ORDER")
                _userMessage.value = "Order status updated to $newStatus"
            } catch (e: Exception) {
                _userMessage.value = e.message
            }
        }
    }

    // --- EXPENSES ---
    fun addExpense(category: String, amount: Double, description: String, addedById: String, addedByName: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val expense = Expense(
            date = today,
            category = category,
            amount = amount,
            description = description,
            addedById = addedById,
            addedByName = addedByName
        )
        viewModelScope.launch {
            try {
                businessRepo.addExpense(expense)
                authUserRepo.logAudit("EXPENSE_ADDED", "Added expense: ৳$amount ($category)", "", "EXPENSE")
                _userMessage.value = "Expense ৳$amount recorded"
            } catch (e: Exception) {
                _userMessage.value = e.message
            }
        }
    }

    // --- CUSTOMERS & SUPPLIERS ---
    fun addCustomer(name: String, phone: String, email: String, address: String) {
        viewModelScope.launch {
            try {
                businessRepo.addCustomer(Customer(name = name, phone = phone, email = email, address = address))
                _userMessage.value = "Customer $name added"
            } catch (e: Exception) {
                _userMessage.value = e.message
            }
        }
    }

    fun recordCustomerPayment(customerId: String, amount: Double) {
        viewModelScope.launch {
            try {
                businessRepo.recordCustomerPayment(customerId, amount)
                _userMessage.value = "Recorded payment: ৳$amount"
            } catch (e: Exception) {
                _userMessage.value = e.message
            }
        }
    }

    fun addSupplier(name: String, phone: String, address: String) {
        viewModelScope.launch {
            try {
                businessRepo.addSupplier(Supplier(name = name, phone = phone, address = address))
                _userMessage.value = "Supplier $name added"
            } catch (e: Exception) {
                _userMessage.value = e.message
            }
        }
    }

    fun recordSupplierPayment(supplierId: String, amount: Double) {
        viewModelScope.launch {
            try {
                businessRepo.recordSupplierPayment(supplierId, amount)
                _userMessage.value = "Paid to supplier: ৳$amount"
            } catch (e: Exception) {
                _userMessage.value = e.message
            }
        }
    }

    // --- STAFF & PERMISSIONS ---
    fun createStaffMember(name: String, email: String, phone: String, role: UserRole, permissions: Map<String, Boolean>) {
        viewModelScope.launch {
            try {
                authUserRepo.createStaffMember(name, email, phone, role, permissions)
                _userMessage.value = "Staff $name created!"
            } catch (e: Exception) {
                _userMessage.value = e.message
            }
        }
    }

    fun updateStaffPermissions(employeeId: String, employeeName: String, newRole: UserRole, oldPerms: Map<String, Boolean>, newPerms: Map<String, Boolean>) {
        viewModelScope.launch {
            try {
                authUserRepo.updateStaffPermissions(employeeId, employeeName, newRole, oldPerms, newPerms)
                _userMessage.value = "Permissions updated for $employeeName"
            } catch (e: Exception) {
                _userMessage.value = e.message
            }
        }
    }

    // --- SETTINGS & BACKUP ---
    fun updateBusinessSettings(settings: BusinessSettings) {
        viewModelScope.launch {
            try {
                businessRepo.updateBusinessSettings(settings)
                authUserRepo.logAudit("SETTINGS_CHANGED", "Updated business settings", "main", "SETTINGS")
                _userMessage.value = "Business settings saved!"
            } catch (e: Exception) {
                _userMessage.value = e.message
            }
        }
    }

    suspend fun exportBackupJson(): String {
        return businessRepo.exportFullBackupJson()
    }

    fun restoreBackup(jsonString: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                businessRepo.restoreBackupJson(jsonString)
                authUserRepo.logAudit("SYSTEM_RESTORE", "Restored system data from JSON backup", "system", "BACKUP")
                _userMessage.value = "Backup restored successfully!"
                onComplete(true, "Restore successful")
            } catch (e: Exception) {
                _userMessage.value = "Restore failed: ${e.message}"
                onComplete(false, e.localizedMessage ?: "Restore failed")
            }
        }
    }

    // --- DASHBOARD METRICS CALCULATION ---
    fun calculateDashboardMetrics(): DashboardMetrics {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val allSales = sales.value
        val allOrders = orders.value
        val allExpenses = expenses.value
        val allProducts = products.value.filter { !it.isArchived }

        val todaySales = allSales.filter { sale ->
            val saleDate = sale.createdAt?.toDate()?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
            }
            saleDate == todayStr && sale.status == "COMPLETED"
        }

        val todayOrders = allOrders.filter { order ->
            val orderDate = order.createdAt?.toDate()?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
            }
            orderDate == todayStr
        }

        val todayExp = allExpenses.filter { it.date == todayStr }

        val todaySalesTotal = todaySales.sumOf { it.total }
        val todayDueTotal = todaySales.sumOf { it.dueAmount }
        val todayExpensesTotal = todayExp.sumOf { it.amount }

        var totalEstimatedCost = 0.0
        for (sale in todaySales) {
            for (item in sale.items) {
                totalEstimatedCost += (item.purchasePrice * item.quantity)
            }
        }
        val estimatedGrossProfit = if (totalEstimatedCost > 0) {
            (todaySalesTotal - totalEstimatedCost - todayExpensesTotal).coerceAtLeast(0.0)
        } else {
            (todaySalesTotal * 0.40 - todayExpensesTotal).coerceAtLeast(0.0) // 40% margin estimate fallback
        }

        return DashboardMetrics(
            todaySalesTotal = todaySalesTotal,
            todaySalesCount = todaySales.size,
            todayOrdersCount = todayOrders.size,
            todayDueTotal = todayDueTotal,
            todayExpensesTotal = todayExpensesTotal,
            totalProductsCount = allProducts.size,
            totalStockCount = allProducts.sumOf { it.currentStock },
            lowStockCount = allProducts.count { it.isLowStock },
            outOfStockCount = allProducts.count { it.isOutOfStock },
            estimatedProfit = estimatedGrossProfit
        )
    }
}
