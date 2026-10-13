package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.ui.MainViewModel
import com.example.ui.theme.IlamBlack
import com.example.ui.theme.IlamDarkCard
import com.example.ui.theme.IlamDarkSurface
import com.example.ui.theme.IlamGold
import com.example.ui.theme.IlamGoldLight
import com.example.ui.theme.IlamSuccess
import com.example.ui.theme.IlamWhite
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineShopScreen(
    userProfile: UserProfile?,
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val currentLang = LocalAppLanguage.current
    val isBangla = currentLang == AppLanguage.BANGLA

    val products by mainViewModel.products.collectAsState()
    val categories by mainViewModel.categories.collectAsState()
    val cartItems by mainViewModel.cartItems.collectAsState()
    val settings by mainViewModel.businessSettings.collectAsState()

    var selectedCategory by remember { mutableStateOf("ALL") }

    // Product detail / size picker
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }
    var chosenSize by remember { mutableStateOf("") }
    var chosenColor by remember { mutableStateOf("") }

    // Checkout Modal
    var showCheckoutModal by remember { mutableStateOf(false) }
    var customerName by remember { mutableStateOf(userProfile?.name ?: "") }
    var customerPhone by remember { mutableStateOf(userProfile?.phone ?: "") }
    var deliveryAddress by remember { mutableStateOf("") }
    var deliveryArea by remember { mutableStateOf("Dhaka") } // Dhaka / Outside Dhaka
    var deliveryMethod by remember { mutableStateOf("COURIER") } // COURIER / STORE_PICKUP
    var orderNotes by remember { mutableStateOf("") }

    // Confirmation
    var confirmedOrderNumber by remember { mutableStateOf<String?>(null) }

    val visibleCategories = categories.filter { it.isOnlineVisible }
    val onlineProducts = products.filter { !it.isArchived && it.currentStock > 0 }
    val filteredProducts = if (selectedCategory == "ALL") onlineProducts else onlineProducts.filter { it.category.equals(selectedCategory, ignoreCase = true) }

    val deliveryCharge = if (deliveryMethod == "STORE_PICKUP") 0.0 else {
        if (deliveryArea.contains("Dhaka", ignoreCase = true)) settings.insideDhakaDeliveryCharge else settings.outsideDhakaDeliveryCharge
    }
    val cartSubtotal = cartItems.sumOf { it.unitPrice * it.quantity }
    val cartDiscount = cartItems.sumOf { it.discount * it.quantity }
    val cartGrandTotal = cartSubtotal - cartDiscount + deliveryCharge

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ILAM",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = IlamGoldLight,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Serif
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBangla) "অনলাইন শপ" else "Online Boutique",
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = IlamWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IlamDarkSurface)
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    colors = CardDefaults.cardColors(containerColor = IlamDarkCard),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${cartItems.sumOf { it.quantity }} ${if (isBangla) "আইটেম কার্টে" else "items in bag"}",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "৳${cartSubtotal.toInt()}",
                                fontWeight = FontWeight.Bold,
                                color = IlamGoldLight,
                                fontSize = 18.sp
                            )
                        }

                        Button(
                            onClick = { showCheckoutModal = true },
                            modifier = Modifier.testTag("customer_checkout_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isBangla) "অর্ডার করুন" else "Place Order", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = IlamBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Category Tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == "ALL",
                        onClick = { selectedCategory = "ALL" },
                        label = { Text(if (isBangla) "সব কালেকশন" else "All Collections") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IlamGold, selectedLabelColor = IlamBlack)
                    )
                }
                items(visibleCategories) { cat ->
                    FilterChip(
                        selected = selectedCategory.equals(cat.nameEn, ignoreCase = true),
                        onClick = { selectedCategory = cat.nameEn },
                        label = { Text(if (isBangla) cat.nameBn else cat.nameEn) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IlamGold, selectedLabelColor = IlamBlack)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Products Grid / List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(filteredProducts) { prod ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                chosenSize = prod.sizes.firstOrNull() ?: ""
                                chosenColor = prod.colors.firstOrNull() ?: ""
                                selectedProductForDetail = prod
                            }
                            .testTag("shop_prod_${prod.id}"),
                        colors = CardDefaults.cardColors(containerColor = IlamDarkSurface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.name, fontWeight = FontWeight.Bold, color = IlamWhite, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${prod.category} • ${prod.subcategory}", color = Color.Gray, fontSize = 12.sp)
                                    if (prod.description.isNotBlank()) {
                                        Text(prod.description, color = Color.LightGray, fontSize = 12.sp, maxLines = 2)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("৳${prod.sellingPrice.toInt()}", fontWeight = FontWeight.Bold, color = IlamGoldLight, fontSize = 18.sp)
                                    if (prod.discount > 0) {
                                        Text("-৳${prod.discount.toInt()} OFF", color = IlamSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row {
                                    if (prod.sizes.isNotEmpty()) {
                                        Text("Sizes: ${prod.sizes.joinToString(", ")}", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }

                                Button(
                                    onClick = {
                                        mainViewModel.addToCart(prod, prod.sizes.firstOrNull() ?: "", prod.colors.firstOrNull() ?: "", 1)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.LocalMall, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isBangla) "ব্যাগে যোগ করুন" else "Add to Bag", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- CHECKOUT MODAL (Store Pickup vs Courier Delivery) ---
    if (showCheckoutModal) {
        AlertDialog(
            onDismissRequest = { showCheckoutModal = false },
            title = {
                Text(
                    text = if (isBangla) "অর্ডার সম্পন্ন করুন" else "Complete Your Order",
                    fontWeight = FontWeight.Bold,
                    color = IlamGoldLight,
                    fontSize = 17.sp
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text(if (isBangla) "আপনার নাম *" else "Full Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text(if (isBangla) "মোবাইল নম্বর *" else "Phone Number *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        Text(if (isBangla) "ডেলিভারি পদ্ধতি:" else "Delivery Method:", color = Color.LightGray, fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = deliveryMethod == "COURIER",
                                onClick = { deliveryMethod = "COURIER" },
                                colors = RadioButtonDefaults.colors(selectedColor = IlamGold)
                            )
                            Text(if (isBangla) "কুরিয়ার হোম ডেলিভারি" else "Courier Delivery", color = IlamWhite, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            RadioButton(
                                selected = deliveryMethod == "STORE_PICKUP",
                                onClick = { deliveryMethod = "STORE_PICKUP" },
                                colors = RadioButtonDefaults.colors(selectedColor = IlamGold)
                            )
                            Text(if (isBangla) "শো-রুম পিকআপ" else "Store Pickup", color = IlamWhite, fontSize = 13.sp)
                        }
                    }

                    if (deliveryMethod == "COURIER") {
                        item {
                            Text(if (isBangla) "ডেলিভারি এরিয়া:" else "Delivery Area:", color = Color.LightGray, fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = deliveryArea == "Dhaka",
                                    onClick = { deliveryArea = "Dhaka" },
                                    colors = RadioButtonDefaults.colors(selectedColor = IlamGold)
                                )
                                Text("ঢাকা সিটি (৳${settings.insideDhakaDeliveryCharge.toInt()})", color = IlamWhite, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                RadioButton(
                                    selected = deliveryArea == "Outside",
                                    onClick = { deliveryArea = "Outside" },
                                    colors = RadioButtonDefaults.colors(selectedColor = IlamGold)
                                )
                                Text("ঢাকার বাইরে (৳${settings.outsideDhakaDeliveryCharge.toInt()})", color = IlamWhite, fontSize = 12.sp)
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = deliveryAddress,
                                onValueChange = { deliveryAddress = it },
                                label = { Text(if (isBangla) "পূর্ণ ঠিকানা *" else "Delivery Address *") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = orderNotes,
                            onValueChange = { orderNotes = it },
                            label = { Text(if (isBangla) "কোনো বিশেষ নির্দেশনা (ঐচ্ছিক)" else "Special Delivery Notes (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = IlamDarkCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Subtotal:", color = Color.LightGray, fontSize = 12.sp)
                                    Text("৳${cartSubtotal.toInt()}", color = IlamWhite, fontSize = 12.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Delivery Charge:", color = Color.LightGray, fontSize = 12.sp)
                                    Text("৳${deliveryCharge.toInt()}", color = IlamWhite, fontSize = 12.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Grand Total:", fontWeight = FontWeight.Bold, color = IlamGoldLight, fontSize = 14.sp)
                                    Text("৳${cartGrandTotal.toInt()}", fontWeight = FontWeight.Bold, color = IlamGoldLight, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customerName.isBlank() || customerPhone.isBlank()) return@Button
                        mainViewModel.placeOnlineOrder(
                            customerName = customerName,
                            customerPhone = customerPhone,
                            deliveryAddress = if (deliveryMethod == "STORE_PICKUP") "Store Pickup (Banani Showroom)" else deliveryAddress,
                            deliveryArea = deliveryArea,
                            deliveryMethod = deliveryMethod,
                            deliveryCharge = deliveryCharge,
                            notes = orderNotes,
                            customerId = userProfile?.userId ?: "",
                            onSuccess = { orderNo ->
                                showCheckoutModal = false
                                confirmedOrderNumber = orderNo
                            }
                        )
                    },
                    modifier = Modifier.testTag("submit_order_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
                ) {
                    Text(if (isBangla) "অর্ডার কনফার্ম করুন" else "Confirm Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutModal = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel", color = Color.Gray)
                }
            },
            containerColor = IlamDarkSurface
        )
    }

    // --- ORDER CONFIRMATION MODAL ---
    if (confirmedOrderNumber != null) {
        AlertDialog(
            onDismissRequest = { confirmedOrderNumber = null },
            title = {
                Text(
                    text = if (isBangla) "অভিনন্দন! অর্ডার সফল হয়েছে" else "Order Placed Successfully!",
                    fontWeight = FontWeight.Bold,
                    color = IlamGoldLight,
                    fontSize = 17.sp
                )
            },
            text = {
                Column {
                    Text("Order Number: $confirmedOrderNumber", fontWeight = FontWeight.Bold, color = IlamWhite, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isBangla)
                            "আমাদের শোরুম প্রতিনিধি শীঘ্রই আপনার নম্বরে কল করে অর্ডার কনফার্ম করবেন।"
                        else
                            "Our showroom representative will call your number shortly to confirm delivery.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { confirmedOrderNumber = null },
                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
                ) {
                    Text(if (isBangla) "ধন্যবাদ" else "Thank You")
                }
            },
            containerColor = IlamDarkSurface
        )
    }
}
