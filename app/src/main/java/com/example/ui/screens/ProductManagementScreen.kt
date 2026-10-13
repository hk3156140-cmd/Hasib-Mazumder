package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PermissionKeys
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
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
import com.example.util.QrCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    userProfile: UserProfile?,
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val currentLang = LocalAppLanguage.current
    val isBangla = currentLang == AppLanguage.BANGLA
    val isOwner = userProfile?.role == UserRole.OWNER.name ||
            userProfile?.email.equals("hk3156140@gmail.com", ignoreCase = true)

    val canAdd = isOwner || userProfile?.hasPermission(PermissionKeys.ADD_PRODUCT) == true
    val canEdit = isOwner || userProfile?.hasPermission(PermissionKeys.EDIT_PRODUCT) == true
    val canArchive = isOwner || userProfile?.hasPermission(PermissionKeys.ARCHIVE_PRODUCT) == true
    val canViewPurchasePrice = isOwner || userProfile?.hasPermission(PermissionKeys.VIEW_PURCHASE_PRICE) == true

    val products by mainViewModel.products.collectAsState()
    val categories by mainViewModel.categories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showArchived by remember { mutableStateOf(false) }

    // Dialog state for Add/Edit
    var showEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    // Dialog state for QR Code
    var qrCodeProduct by remember { mutableStateOf<Product?>(null) }

    // Restock dialog
    var restockProductTarget by remember { mutableStateOf<Product?>(null) }
    var restockAmountText by remember { mutableStateOf("10") }

    val displayedProducts = products.filter { prod ->
        val matchesArchive = if (showArchived) prod.isArchived else !prod.isArchived
        val matchesCategory = (selectedCategory == "ALL" || prod.category.equals(selectedCategory, ignoreCase = true))
        val matchesSearch = searchQuery.isBlank() ||
                prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.sku.contains(searchQuery, ignoreCase = true) ||
                prod.barcode.contains(searchQuery, ignoreCase = true)
        matchesArchive && matchesCategory && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBangla) "পণ্য ও স্টক ইনভেন্টরি" else "Products & Stock Inventory",
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
        floatingActionButton = {
            if (canAdd) {
                FloatingActionButton(
                    onClick = {
                        editingProduct = Product()
                        showEditDialog = true
                    },
                    containerColor = IlamGold,
                    contentColor = IlamBlack,
                    modifier = Modifier.testTag("add_product_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Product")
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

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("product_search_input"),
                placeholder = { Text(if (isBangla) "নাম, SKU বা বারকোড দিয়ে খুঁজুন..." else "Search product name, SKU or barcode...", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = IlamGold) },
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Categories & Archive Filter
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == "ALL",
                        onClick = { selectedCategory = "ALL" },
                        label = { Text(if (isBangla) "সব পণ্য" else "All Products") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IlamGold, selectedLabelColor = IlamBlack)
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory.equals(cat.nameEn, ignoreCase = true),
                        onClick = { selectedCategory = cat.nameEn },
                        label = { Text(if (isBangla) cat.nameBn else cat.nameEn) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IlamGold, selectedLabelColor = IlamBlack)
                    )
                }
                item {
                    FilterChip(
                        selected = showArchived,
                        onClick = { showArchived = !showArchived },
                        label = { Text(if (isBangla) "আর্কাইভকৃত" else "Archived") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IlamWarning, selectedLabelColor = IlamBlack)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(displayedProducts) { prod ->
                    ProductItemCard(
                        product = prod,
                        isBangla = isBangla,
                        canViewPurchasePrice = canViewPurchasePrice,
                        canEdit = canEdit,
                        canArchive = canArchive,
                        onEdit = {
                            editingProduct = prod
                            showEditDialog = true
                        },
                        onArchive = { mainViewModel.archiveProduct(prod.id) },
                        onRestore = { mainViewModel.restoreProduct(prod.id) },
                        onShowQr = { qrCodeProduct = prod },
                        onRestock = {
                            restockProductTarget = prod
                            restockAmountText = "10"
                        }
                    )
                }
            }
        }
    }

    // --- DIALOG: ADD / EDIT PRODUCT ---
    if (showEditDialog && editingProduct != null) {
        ProductFormDialog(
            initialProduct = editingProduct!!,
            categories = categories,
            isBangla = isBangla,
            canViewPurchasePrice = canViewPurchasePrice,
            onDismiss = { showEditDialog = false },
            onSave = { productToSave ->
                if (productToSave.id.isBlank()) {
                    mainViewModel.addProduct(productToSave) { showEditDialog = false }
                } else {
                    mainViewModel.updateProduct(productToSave) { showEditDialog = false }
                }
            }
        )
    }

    // --- DIALOG: QR CODE LABEL ---
    if (qrCodeProduct != null) {
        val prod = qrCodeProduct!!
        AlertDialog(
            onDismissRequest = { qrCodeProduct = null },
            title = {
                Text(
                    text = if (isBangla) "পণ্য QR কোড লেবেল" else "Product QR Code Label",
                    fontWeight = FontWeight.Bold,
                    color = IlamGoldLight,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val qrBitmap = remember(prod.sku) {
                        QrCodeGenerator.generateQrBitmap("ILAM:${prod.sku}")
                    }

                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(prod.name, fontWeight = FontWeight.Bold, color = IlamWhite, fontSize = 15.sp, textAlign = TextAlign.Center)
                    Text("SKU: ${prod.sku}", color = Color.Gray, fontSize = 13.sp)
                    Text("Price: ৳${prod.sellingPrice.toInt()}", fontWeight = FontWeight.Bold, color = IlamGoldLight, fontSize = 16.sp)
                    if (prod.discount > 0) {
                        Text("Discount: ৳${prod.discount.toInt()}", color = IlamSuccess, fontSize = 12.sp)
                    }
                    Text("Stock: ${prod.currentStock}", color = if (prod.isLowStock) IlamWarning else IlamSuccess, fontSize = 13.sp)
                    if (prod.sizes.isNotEmpty()) {
                        Text("Sizes: ${prod.sizes.joinToString(", ")}", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { qrCodeProduct = null },
                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
                ) {
                    Text(if (isBangla) "বন্ধ করুন" else "Close")
                }
            },
            containerColor = IlamDarkSurface
        )
    }

    // --- DIALOG: QUICK RESTOCK ---
    if (restockProductTarget != null) {
        val prod = restockProductTarget!!
        AlertDialog(
            onDismissRequest = { restockProductTarget = null },
            title = { Text(if (isBangla) "স্টক বৃদ্ধি করুন" else "Restock Product", color = IlamGoldLight) },
            text = {
                Column {
                    Text(prod.name, color = IlamWhite, fontWeight = FontWeight.SemiBold)
                    Text(if (isBangla) "বর্তমান স্টক: ${prod.currentStock}" else "Current stock: ${prod.currentStock}", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = restockAmountText,
                        onValueChange = { restockAmountText = it },
                        label = { Text(if (isBangla) "যোগ করার পরিমাণ" else "Units to Add") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = restockAmountText.toIntOrNull() ?: 10
                        mainViewModel.restockProduct(prod.id, amount)
                        restockProductTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
                ) {
                    Text(if (isBangla) "যোগ করুন" else "Add Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { restockProductTarget = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel", color = Color.Gray)
                }
            },
            containerColor = IlamDarkSurface
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    isBangla: Boolean,
    canViewPurchasePrice: Boolean,
    canEdit: Boolean,
    canArchive: Boolean,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onShowQr: () -> Unit,
    onRestock: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = IlamDarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.Bold, color = IlamWhite, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${product.category} • ${product.subcategory}", color = Color.Gray, fontSize = 12.sp)
                    Text("SKU: ${product.sku} | Barcode: ${product.barcode}", color = Color.LightGray, fontSize = 11.sp)
                }

                // Stock Badge
                val badgeColor = when {
                    product.isOutOfStock -> IlamDanger
                    product.isLowStock -> IlamWarning
                    else -> IlamSuccess
                }
                val badgeText = when {
                    product.isOutOfStock -> if (isBangla) "স্টক শেষ" else "Out of Stock"
                    product.isLowStock -> if (isBangla) "কম স্টক (${product.currentStock})" else "Low (${product.currentStock})"
                    else -> "${product.currentStock} ${if (isBangla) "মজুদ" else "In Stock"}"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(badgeText, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("৳${product.sellingPrice.toInt()}", fontWeight = FontWeight.Bold, color = IlamGoldLight, fontSize = 16.sp)
                        if (product.discount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("-৳${product.discount.toInt()}", color = IlamSuccess, fontSize = 11.sp)
                        }
                    }
                    if (canViewPurchasePrice) {
                        Text(
                            text = "${if (isBangla) "ক্রয় মূল্য: " else "Cost: "}৳${product.purchasePrice.toInt()}",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                Row {
                    IconButton(onClick = onShowQr) {
                        Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = IlamGoldLight)
                    }
                    if (canEdit) {
                        IconButton(onClick = onRestock) {
                            Icon(Icons.Default.Add, contentDescription = "Restock", tint = IlamSuccess)
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.LightGray)
                        }
                    }
                    if (canArchive) {
                        if (product.isArchived) {
                            IconButton(onClick = onRestore) {
                                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = IlamSuccess)
                            }
                        } else {
                            IconButton(onClick = onArchive) {
                                Icon(Icons.Default.Archive, contentDescription = "Archive", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductFormDialog(
    initialProduct: Product,
    categories: List<com.example.data.model.Category>,
    isBangla: Boolean,
    canViewPurchasePrice: Boolean,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct.name) }
    var sku by remember { mutableStateOf(initialProduct.sku) }
    var barcode by remember { mutableStateOf(initialProduct.barcode) }
    var category by remember { mutableStateOf(initialProduct.category.ifBlank { categories.firstOrNull()?.nameEn ?: "MEN" }) }
    var subcategory by remember { mutableStateOf(initialProduct.subcategory) }
    var description by remember { mutableStateOf(initialProduct.description) }
    var sellingPriceText by remember { mutableStateOf(if (initialProduct.sellingPrice > 0) initialProduct.sellingPrice.toInt().toString() else "") }
    var purchasePriceText by remember { mutableStateOf(if (initialProduct.purchasePrice > 0) initialProduct.purchasePrice.toInt().toString() else "") }
    var discountText by remember { mutableStateOf(if (initialProduct.discount > 0) initialProduct.discount.toInt().toString() else "0") }
    var stockText by remember { mutableStateOf(if (initialProduct.currentStock > 0) initialProduct.currentStock.toString() else "10") }
    var minStockText by remember { mutableStateOf(initialProduct.minimumStock.toString()) }
    var sizesText by remember { mutableStateOf(initialProduct.sizes.joinToString(", ")) }
    var colorsText by remember { mutableStateOf(initialProduct.colors.joinToString(", ")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialProduct.id.isBlank())
                    (if (isBangla) "নতুন পোশাক পণ্য যোগ করুন" else "Add New Clothing Product")
                else
                    (if (isBangla) "পণ্য সম্পাদনা" else "Edit Product"),
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
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isBangla) "পণ্যের নাম *" else "Product Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                        singleLine = true
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text("SKU") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("Barcode") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Text(if (isBangla) "ক্যাটাগরি নির্বাচন করুন:" else "Category:", color = Color.LightGray, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat.nameEn,
                                onClick = { category = cat.nameEn },
                                label = { Text(if (isBangla) cat.nameBn else cat.nameEn) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IlamGold, selectedLabelColor = IlamBlack)
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = subcategory,
                        onValueChange = { subcategory = it },
                        label = { Text(if (isBangla) "সাব-ক্যাটাগরি (যেমন Shirt, Panjabi)" else "Subcategory") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sellingPriceText,
                            onValueChange = { sellingPriceText = it },
                            label = { Text(if (isBangla) "বিক্রয় মূল্য (৳) *" else "Selling Price *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        if (canViewPurchasePrice) {
                            OutlinedTextField(
                                value = purchasePriceText,
                                onValueChange = { purchasePriceText = it },
                                label = { Text(if (isBangla) "ক্রয় মূল্য (৳)" else "Cost Price") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stockText,
                            onValueChange = { stockText = it },
                            label = { Text(if (isBangla) "বর্তমান স্টক" else "Stock") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = minStockText,
                            onValueChange = { minStockText = it },
                            label = { Text(if (isBangla) "মিনিমাম স্টক" else "Min Alert") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = sizesText,
                        onValueChange = { sizesText = it },
                        label = { Text(if (isBangla) "সাইজ সমূহ (যেমন M, L, XL)" else "Sizes (e.g. M, L, XL)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = colorsText,
                        onValueChange = { colorsText = it },
                        label = { Text(if (isBangla) "রং সমূহ (যেমন Black, White)" else "Colors (e.g. Black, White)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(if (isBangla) "বিবরণ" else "Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || sellingPriceText.isBlank()) return@Button
                    val updatedSizes = sizesText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val updatedColors = colorsText.split(",").map { it.trim() }.filter { it.isNotBlank() }

                    val saved = initialProduct.copy(
                        name = name,
                        sku = sku,
                        barcode = barcode,
                        category = category,
                        subcategory = subcategory,
                        description = description,
                        sellingPrice = sellingPriceText.toDoubleOrNull() ?: 0.0,
                        purchasePrice = purchasePriceText.toDoubleOrNull() ?: 0.0,
                        discount = discountText.toDoubleOrNull() ?: 0.0,
                        currentStock = stockText.toIntOrNull() ?: 0,
                        minimumStock = minStockText.toIntOrNull() ?: 5,
                        sizes = updatedSizes,
                        colors = updatedColors
                    )
                    onSave(saved)
                },
                modifier = Modifier.testTag("save_product_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
            ) {
                Text(if (isBangla) "সংরক্ষণ করুন" else "Save Product")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBangla) "বাতিল" else "Cancel", color = Color.Gray)
            }
        },
        containerColor = IlamDarkSurface
    )
}
