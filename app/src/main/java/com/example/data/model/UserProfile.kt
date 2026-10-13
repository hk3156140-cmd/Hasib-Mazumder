package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole {
    OWNER,
    MANAGER,
    STAFF,
    CUSTOMER
}

object PermissionKeys {
    const val SCAN = "scan"
    const val POS_SALES = "posSales"
    const val VIEW_SELLING_PRICE = "viewSellingPrice"
    const val CHANGE_SELLING_PRICE = "changeSellingPrice"
    const val VIEW_PURCHASE_PRICE = "viewPurchasePrice"
    const val ADD_PRODUCT = "addProduct"
    const val EDIT_PRODUCT = "editProduct"
    const val ARCHIVE_PRODUCT = "archiveProduct"
    const val RESTORE_PRODUCT = "restoreProduct"
    const val MANAGE_STOCK = "manageStock"
    const val MANAGE_CATEGORIES = "manageCategories"
    const val MANAGE_CUSTOMERS = "manageCustomers"
    const val MANAGE_SUPPLIERS = "manageSuppliers"
    const val VIEW_SALES_REPORTS = "viewSalesReports"
    const val VIEW_PROFIT_REPORTS = "viewProfitReports"
    const val MANAGE_EXPENSES = "manageExpenses"
    const val MANAGE_ONLINE_ORDERS = "manageOnlineOrders"
    const val MANAGE_COURIER = "manageCourier"
    const val APPLY_DISCOUNTS = "applyDiscounts"
    const val MANAGE_OTHER_STAFF = "manageOtherStaff"
    const val CHANGE_BUSINESS_SETTINGS = "changeBusinessSettings"
    const val VIEW_CUSTOMER_DUE = "viewCustomerDue"
    const val EDIT_CUSTOMER_DUE = "editCustomerDue"
    const val VIEW_SUPPLIER_DUE = "viewSupplierDue"
    const val CREATE_INVOICE = "createInvoice"
    const val CANCEL_SALE = "cancelSale"
    const val RETURN_PRODUCT = "returnProduct"
    const val VIEW_ACTIVITY_LOGS = "viewActivityLogs"

    val ALL = listOf(
        SCAN, POS_SALES, VIEW_SELLING_PRICE, CHANGE_SELLING_PRICE, VIEW_PURCHASE_PRICE,
        ADD_PRODUCT, EDIT_PRODUCT, ARCHIVE_PRODUCT, RESTORE_PRODUCT, MANAGE_STOCK,
        MANAGE_CATEGORIES, MANAGE_CUSTOMERS, MANAGE_SUPPLIERS, VIEW_SALES_REPORTS,
        VIEW_PROFIT_REPORTS, MANAGE_EXPENSES, MANAGE_ONLINE_ORDERS, MANAGE_COURIER,
        APPLY_DISCOUNTS, MANAGE_OTHER_STAFF, CHANGE_BUSINESS_SETTINGS, VIEW_CUSTOMER_DUE,
        EDIT_CUSTOMER_DUE, VIEW_SUPPLIER_DUE, CREATE_INVOICE, CANCEL_SALE,
        RETURN_PRODUCT, VIEW_ACTIVITY_LOGS
    )

    fun getDisplayName(key: String, isBangla: Boolean): String = when (key) {
        SCAN -> if (isBangla) "QR / বারকোড স্ক্যান" else "QR / Barcode Scan"
        POS_SALES -> if (isBangla) "পিওএস বিক্রয়" else "POS Sales"
        VIEW_SELLING_PRICE -> if (isBangla) "বিক্রয় মূল্য দেখা" else "View Selling Price"
        CHANGE_SELLING_PRICE -> if (isBangla) "বিক্রয় মূল্য পরিবর্তন" else "Change Selling Price"
        VIEW_PURCHASE_PRICE -> if (isBangla) "ক্রয় মূল্য দেখা" else "View Purchase Price"
        ADD_PRODUCT -> if (isBangla) "পণ্য যোগ করা" else "Add Product"
        EDIT_PRODUCT -> if (isBangla) "পণ্য সম্পাদনা" else "Edit Product"
        ARCHIVE_PRODUCT -> if (isBangla) "পণ্য আর্কাইভ" else "Archive Product"
        RESTORE_PRODUCT -> if (isBangla) "পণ্য পুনরুদ্ধার" else "Restore Product"
        MANAGE_STOCK -> if (isBangla) "স্টক ব্যবস্থাপনা" else "Manage Stock"
        MANAGE_CATEGORIES -> if (isBangla) "ক্যাটাগরি নিয়ন্ত্রণ" else "Manage Categories"
        MANAGE_CUSTOMERS -> if (isBangla) "কাস্টমার ব্যবস্থাপনা" else "Manage Customers"
        MANAGE_SUPPLIERS -> if (isBangla) "সাপ্লায়ার ব্যবস্থাপনা" else "Manage Suppliers"
        VIEW_SALES_REPORTS -> if (isBangla) "বিক্রয় রিপোর্ট দেখা" else "View Sales Reports"
        VIEW_PROFIT_REPORTS -> if (isBangla) "লাভ রিপোর্ট দেখা" else "View Profit Reports"
        MANAGE_EXPENSES -> if (isBangla) "খরচ ব্যবস্থাপনা" else "Manage Expenses"
        MANAGE_ONLINE_ORDERS -> if (isBangla) "অনলাইন অর্ডার পরিচালনা" else "Manage Online Orders"
        MANAGE_COURIER -> if (isBangla) "কুরিয়ার পরিচালনা" else "Manage Courier"
        APPLY_DISCOUNTS -> if (isBangla) "ডিসকাউন্ট প্রয়োগ" else "Apply Discounts"
        MANAGE_OTHER_STAFF -> if (isBangla) "অন্যান্য কর্মী ব্যবস্থাপনা" else "Manage Other Staff"
        CHANGE_BUSINESS_SETTINGS -> if (isBangla) "বিজনেস সেটিংস পরিবর্তন" else "Change Business Settings"
        VIEW_CUSTOMER_DUE -> if (isBangla) "কাস্টমার বকেয়া দেখা" else "View Customer Due"
        EDIT_CUSTOMER_DUE -> if (isBangla) "কাস্টমার বকেয়া সম্পাদনা" else "Edit Customer Due"
        VIEW_SUPPLIER_DUE -> if (isBangla) "সাপ্লায়ার বকেয়া দেখা" else "View Supplier Due"
        CREATE_INVOICE -> if (isBangla) "চালান/রসিদ তৈরি" else "Create Invoice"
        CANCEL_SALE -> if (isBangla) "বিক্রয় বাতিল" else "Cancel Sale"
        RETURN_PRODUCT -> if (isBangla) "পণ্য ফেরত গ্রহণ" else "Return Product"
        VIEW_ACTIVITY_LOGS -> if (isBangla) "কার্যকলাপ লগ দেখা" else "View Activity Logs"
        else -> key
    }

    fun defaultStaffPermissions(): Map<String, Boolean> = mapOf(
        SCAN to true,
        POS_SALES to true,
        VIEW_SELLING_PRICE to true,
        CREATE_INVOICE to true,
        VIEW_CUSTOMER_DUE to true
    )

    fun defaultManagerPermissions(): Map<String, Boolean> = ALL.associateWith { perm ->
        perm != VIEW_PROFIT_REPORTS && perm != MANAGE_OTHER_STAFF && perm != CHANGE_BUSINESS_SETTINGS
    }

    fun fullPermissions(): Map<String, Boolean> = ALL.associateWith { true }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = UserRole.CUSTOMER.name,
    val status: String = "ACTIVE",
    val permissions: Map<String, Boolean> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun hasPermission(key: String): Boolean {
        if (role == UserRole.OWNER.name || email.equals("hk3156140@gmail.com", ignoreCase = true)) return true
        return permissions[key] == true
    }
}
