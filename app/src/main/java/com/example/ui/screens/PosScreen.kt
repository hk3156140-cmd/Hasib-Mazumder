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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItem
import com.example.data.model.PermissionKeys
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.UserProfile
import com.example.ui.MainViewModel
import com.example.ui.theme.IlamBlack
import com.example.ui.theme.IlamDanger
import com.example.ui.theme.IlamDarkCard
import com.example.ui.theme.IlamDarkSurface
import com.example.ui.theme.IlamGold
import com.example.ui.theme.IlamGoldLight
import com.example.ui.theme.IlamSuccess
import com.example.ui.theme.IlamWarning
import com.example.ui.theme.IlamWhite
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    userProfile: UserProfile?,
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val currentLang = LocalAppLanguage.current
    val isBangla = currentLang == AppLanguage.BANGLA

    val products by mainViewModel.products.collectAsState()
    val categories by mainViewModel.categories.collectAsState()
    val cartItems by mainViewModel.cartItems.collectAsState()
    val customers by mainViewModel.customers.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }

    // Dialog state for adding product with size/color selection
    var productForSelection by remember { mutableStateOf<Product?>(null) }
    var selectedSize by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("") }

    // Checkout & Payment State
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var selectedCustomerId by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") } // CASH, BKASH, OTHER
    var customDiscountText by remember { mutableStateOf("0") }
    var paidAmountText by remember { mutableStateOf("") }

    // Invoice Dialog
    var completedSaleInvoice by remember { mutableStateOf<Sale?>(null) }

    val activeProducts = products.filter { !it.isArchived && it.currentStock > 0 }
    val filteredProducts = activeProducts.filter { prod ->
        val matchesCategory = (selectedCategory == "ALL" || prod.category.equals(selectedCategory, ignoreCase = true))
        val matchesSearch = searchQuery.isBlank() ||
                prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.sku.contains(searchQuery, ignoreCase = true) ||
                prod.barcode.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    val subtotal = cartItems.sumOf { it.unitPrice * it.quantity }
    val itemDiscounts = cartItems.sumOf { it.discount * it.quantity }
    val overallDiscount = customDiscountText.toDoubleOrNull() ?: 0.0
    val totalDiscount = itemDiscounts + overallDiscount
    val grandTotal = (subtotal - totalDiscount).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBangla) "পিওএস বিক্রয় কাউন্টার" else "POS Sales Counter",
                        fontWeight = FontWeight.Bold,
                        color = IlamGoldLight,
                        fontSize = 18.sp
                    )
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
                SurfaceBottomBar(
                    cartItems = cartItems,
                    grandTotal = grandTotal,
                    isBangla = isBangla,
                    onCheckoutClicked = {
                        paidAmountText = grandTotal.toInt().toString()
                        showCheckoutDialog = true
                    },
                    onClearCart = { mainViewModel.clearCart() }
                )
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

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pos_search_input"),
                placeholder = {
                    Text(
                        if (isBangla) "পণ্য, SKU অথবা বারকোড দিয়ে খুঁজুন..." else "Search product name, SKU or barcode...",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = IlamGold)
                },
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == "ALL",
                        onClick = { selectedCategory = "ALL" },
                        label = { Text(if (isBangla) "সব" else "All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IlamGold,
                            selectedLabelColor = IlamBlack
                        )
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory.equals(cat.nameEn, ignoreCase = true),
                        onClick = { selectedCategory = cat.nameEn },
                        label = { Text(if (isBangla) cat.nameBn else cat.nameEn) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IlamGold,
                            selectedLabelColor = IlamBlack
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Product Selection Grid / List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(filteredProducts) { prod ->
                    PosProductRow(
                        product = prod,
                        isBangla = isBangla,
                        onSelect = {
                            selectedSize = prod.sizes.firstOrNull() ?: ""
                            selectedColor = prod.colors.firstOrNull() ?: ""
                            productForSelection = prod
                        }
                    )
                }
            }
        }
    }

    // --- DIALOG: SELECT SIZE & COLOR BEFORE ADDING TO CART ---
    if (productForSelection != null) {
        val prod = productForSelection!!
        AlertDialog(
            onDismissRequest = { productForSelection = null },
            title = {
                Text(prod.name, fontWeight = FontWeight.Bold, color = IlamWhite, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(
                        text = "SKU: ${prod.sku} | ৳${prod.sellingPrice.toInt()}",
                        color = IlamGoldLight,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (prod.sizes.isNotEmpty()) {
                        Text(if (isBangla) "সাইজ নির্বাচন করুন:" else "Select Size:", color = Color.LightGray, fontSize = 12.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                            items(prod.sizes) { size ->
                                FilterChip(
                                    selected = selectedSize == size,
                                    onClick = { selectedSize = size },
                                    label = { Text(size) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IlamGold,
                                        selectedLabelColor = IlamBlack
                                    )
                                )
                            }
                        }
                    }

                    if (prod.colors.isNotEmpty()) {
                        Text(if (isBangla) "রং নির্বাচন করুন:" else "Select Color:", color = Color.LightGray, fontSize = 12.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                            items(prod.colors) { color ->
                                FilterChip(
                                    selected = selectedColor == color,
                                    onClick = { selectedColor = color },
                                    label = { Text(color) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IlamGold,
                                        selectedLabelColor = IlamBlack
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mainViewModel.addToCart(prod, selectedSize, selectedColor, 1)
                        productForSelection = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
                ) {
                    Text(if (isBangla) "কার্টে যোগ করুন" else "Add to Cart")
                }
            },
            dismissButton = {
                TextButton(onClick = { productForSelection = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel", color = Color.Gray)
                }
            },
            containerColor = IlamDarkSurface
        )
    }

    // --- CHECKOUT & PAYMENT DIALOG ---
    if (showCheckoutDialog) {
        val paidAmount = paidAmountText.toDoubleOrNull() ?: grandTotal
        val dueAmount = (grandTotal - paidAmount).coerceAtLeast(0.0)

        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            title = {
                Text(
                    text = if (isBangla) "বিক্রয় নিশ্চিত করুন ও চালান" else "Confirm Sale & Payment",
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
                        // Customer selection or entry
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text(if (isBangla) "কাস্টমার নাম" else "Customer Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text(if (isBangla) "ফোন নম্বর" else "Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        // Quick Discount
                        OutlinedTextField(
                            value = customDiscountText,
                            onValueChange = { customDiscountText = it },
                            label = { Text(if (isBangla) "অতিরিক্ত ডিসকাউন্ট (৳)" else "Extra Discount (৳)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        Text(
                            text = if (isBangla) "পেমেন্ট মাধ্যম:" else "Payment Method:",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = paymentMethod == "CASH",
                                onClick = { paymentMethod = "CASH" },
                                colors = RadioButtonDefaults.colors(selectedColor = IlamGold)
                            )
                            Text(if (isBangla) "নগদ (Cash)" else "Cash", color = IlamWhite, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            RadioButton(
                                selected = paymentMethod == "BKASH",
                                onClick = { paymentMethod = "BKASH" },
                                colors = RadioButtonDefaults.colors(selectedColor = IlamGold)
                            )
                            Text("bKash", color = IlamWhite, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            RadioButton(
                                selected = paymentMethod == "OTHER",
                                onClick = { paymentMethod = "OTHER" },
                                colors = RadioButtonDefaults.colors(selectedColor = IlamGold)
                            )
                            Text("Card/Other", color = IlamWhite, fontSize = 13.sp)
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = paidAmountText,
                            onValueChange = { paidAmountText = it },
                            label = { Text(if (isBangla) "পরিশোধিত টাকা (Paid Amount)" else "Paid Amount") },
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(if (isBangla) "সর্বমোট টাকা:" else "Total Amount:", color = Color.LightGray, fontSize = 13.sp)
                                    Text("৳${grandTotal.toInt()}", fontWeight = FontWeight.Bold, color = IlamGoldLight, fontSize = 15.sp)
                                }
                                if (dueAmount > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(if (isBangla) "বকেয়া (Due):" else "Customer Due:", color = IlamDanger, fontSize = 13.sp)
                                        Text("৳${dueAmount.toInt()}", fontWeight = FontWeight.Bold, color = IlamDanger, fontSize = 15.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paid = paidAmountText.toDoubleOrNull() ?: grandTotal
                        mainViewModel.completePosSale(
                            customerId = selectedCustomerId,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            paymentMethod = paymentMethod,
                            overallDiscount = overallDiscount,
                            paidAmount = paid,
                            staffId = userProfile?.userId ?: "STAFF",
                            staffName = userProfile?.name ?: "Staff",
                            onSuccess = { createdSale ->
                                showCheckoutDialog = false
                                completedSaleInvoice = createdSale
                            }
                        )
                    },
                    modifier = Modifier.testTag("confirm_pos_sale_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
                ) {
                    Text(if (isBangla) "চালান নিশ্চিত করুন" else "Confirm & Print")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel", color = Color.Gray)
                }
            },
            containerColor = IlamDarkSurface
        )
    }

    // --- INVOICE VIEW / PRINT MODAL ---
    if (completedSaleInvoice != null) {
        val invoice = completedSaleInvoice!!
        AlertDialog(
            onDismissRequest = { completedSaleInvoice = null },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ILAM",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        color = IlamGoldLight,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = if (isBangla) "বিক্রয় চালান / ইনভয়েস" else "SALES INVOICE",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Divider(color = Color.DarkGray)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Invoice: ${invoice.invoiceNumber}", fontSize = 12.sp, color = IlamWhite)
                            Text(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()), fontSize = 12.sp, color = Color.LightGray)
                        }
                        Text("Customer: ${invoice.customerName} (${invoice.customerPhone.ifBlank { "N/A" }})", fontSize = 12.sp, color = Color.LightGray)
                        Text("Served by: ${invoice.staffName}", fontSize = 12.sp, color = Color.LightGray)
                        Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 6.dp))
                    }

                    items(invoice.items) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, color = IlamWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("${item.size} / ${item.color} x ${item.quantity}", color = Color.Gray, fontSize = 11.sp)
                            }
                            Text("৳${(item.unitPrice * item.quantity).toInt()}", color = IlamWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", color = Color.LightGray, fontSize = 12.sp)
                            Text("৳${invoice.subtotal.toInt()}", color = IlamWhite, fontSize = 12.sp)
                        }
                        if (invoice.discount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discount", color = IlamSuccess, fontSize = 12.sp)
                                Text("-৳${invoice.discount.toInt()}", color = IlamSuccess, fontSize = 12.sp)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", fontWeight = FontWeight.Bold, color = IlamGoldLight, fontSize = 14.sp)
                            Text("৳${invoice.total.toInt()}", fontWeight = FontWeight.Bold, color = IlamGoldLight, fontSize = 15.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Paid (${invoice.paymentMethod})", color = IlamWhite, fontSize = 12.sp)
                            Text("৳${invoice.paidAmount.toInt()}", color = IlamWhite, fontSize = 12.sp)
                        }
                        if (invoice.dueAmount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Due", fontWeight = FontWeight.Bold, color = IlamDanger, fontSize = 13.sp)
                                Text("৳${invoice.dueAmount.toInt()}", fontWeight = FontWeight.Bold, color = IlamDanger, fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Thank you for shopping at ILAM!",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { completedSaleInvoice = null },
                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
                ) {
                    Text(if (isBangla) "সম্পন্ন" else "Done")
                }
            },
            containerColor = IlamDarkSurface
        )
    }
}

@Composable
fun PosProductRow(
    product: Product,
    isBangla: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("pos_product_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = IlamDarkSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.SemiBold, color = IlamWhite, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SKU: ${product.sku}", color = Color.Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBangla) "স্টক: ${product.currentStock}" else "Stock: ${product.currentStock}",
                        color = if (product.isLowStock) IlamWarning else IlamSuccess,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("৳${product.sellingPrice.toInt()}", fontWeight = FontWeight.Bold, color = IlamGoldLight, fontSize = 16.sp)
                if (product.discount > 0) {
                    Text("-৳${product.discount.toInt()} off", color = IlamSuccess, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun SurfaceBottomBar(
    cartItems: List<CartItem>,
    grandTotal: Double,
    isBangla: Boolean,
    onCheckoutClicked: () -> Unit,
    onClearCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
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
                    text = "${cartItems.sumOf { it.quantity }} ${if (isBangla) "টি পণ্য" else "items"}",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                Text(
                    text = "৳${grandTotal.toInt()}",
                    fontWeight = FontWeight.Bold,
                    color = IlamGoldLight,
                    fontSize = 20.sp
                )
            }

            Row {
                IconButton(onClick = onClearCart) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.Gray)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onCheckoutClicked,
                    modifier = Modifier.testTag("pos_checkout_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isBangla) "চেকআউট" else "Checkout", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
