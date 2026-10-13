package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

enum class AppLanguage {
    BANGLA,
    ENGLISH
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.BANGLA }

object Strings {
    fun get(key: String, lang: AppLanguage): String {
        val bn = (lang == AppLanguage.BANGLA)
        return when (key) {
            "app_title" -> if (bn) "ইলাম স্টোর ম্যানেজার" else "ILAM STORE MANAGER"
            "showroom" -> if (bn) "শো-রুম ও ফ্যাশন হাউস" else "Showroom & Fashion House"
            "dashboard" -> if (bn) "ড্যাশবোর্ড" else "Dashboard"
            "pos" -> if (bn) "পিওএস বিক্রয়" else "POS Sale"
            "products" -> if (bn) "পণ্য তালিকা" else "Products"
            "orders" -> if (bn) "অনলাইন অর্ডার" else "Online Orders"
            "customers" -> if (bn) "কাস্টমার" else "Customers"
            "suppliers" -> if (bn) "সাপ্লায়ার" else "Suppliers"
            "expenses" -> if (bn) "দৈনিক খরচ" else "Expenses"
            "ledger" -> if (bn) "দৈনিক হিসাব" else "Daily Ledger"
            "reports" -> if (bn) "রিপোর্ট ও লাভ" else "Reports & Profit"
            "staff_permissions" -> if (bn) "কর্মী ও পারমিশন" else "Staff & Permissions"
            "audit_logs" -> if (bn) "কার্যকলাপ লগ" else "Activity Logs"
            "settings" -> if (bn) "সেটিংস" else "Settings"
            "online_shop" -> if (bn) "অনলাইন শপ" else "Online Shop"
            "scan_qr" -> if (bn) "QR স্ক্যান" else "Scan QR"
            "scan_barcode" -> if (bn) "বারকোড স্ক্যান" else "Scan Barcode"
            "today_sales" -> if (bn) "আজকের বিক্রয়" else "Today's Sales"
            "today_orders" -> if (bn) "আজকের অর্ডার" else "Today's Orders"
            "today_due" -> if (bn) "আজকের বকেয়া" else "Today's Due"
            "today_expenses" -> if (bn) "আজকের খরচ" else "Today's Expenses"
            "total_products" -> if (bn) "মোট পণ্য" else "Total Products"
            "total_stock" -> if (bn) "মোট স্টক" else "Total Stock"
            "low_stock" -> if (bn) "কম স্টক" else "Low Stock"
            "out_of_stock" -> if (bn) "স্টক শেষ" else "Out of Stock"
            "estimated_profit" -> if (bn) "আনুমানিক লাভ" else "Estimated Profit"
            "add_product" -> if (bn) "নতুন পণ্য যোগ" else "Add New Product"
            "new_sale" -> if (bn) "নতুন বিক্রয় (POS)" else "New POS Sale"
            "checkout" -> if (bn) "চেকআউট" else "Checkout"
            "store_pickup" -> if (bn) "স্টোর পিকআপ" else "Store Pickup"
            "courier_delivery" -> if (bn) "কুরিয়ার ডেলিভারি" else "Courier Delivery"
            "cart" -> if (bn) "কার্ট" else "Cart"
            "role" -> if (bn) "পদবী / ভূমিকা" else "Role"
            "owner" -> if (bn) "মালিক (Owner)" else "Owner"
            "manager" -> if (bn) "ম্যানেজার (Manager)" else "Manager"
            "staff" -> if (bn) "কর্মী (Staff)" else "Staff"
            "customer" -> if (bn) "কাস্টমার (Customer)" else "Customer"
            "bdt" -> "৳"
            "login" -> if (bn) "লগইন করুন" else "Sign In"
            "logout" -> if (bn) "লগআউট" else "Sign Out"
            "backup_title" -> if (bn) "ডাটা ব্যাকআপ ও রিস্টোর" else "Data Backup & Restore"
            "backup_now" -> if (bn) "সম্পূর্ণ ডাটা ব্যাকআপ নিন" else "Backup Full Database"
            "permission_denied" -> if (bn) "আপনার এই কাজের অনুমতি নেই।" else "You don't have permission to perform this action."
            else -> key
        }
    }
}

@Composable
fun stringRes(key: String): String {
    val lang = LocalAppLanguage.current
    return Strings.get(key, lang)
}
