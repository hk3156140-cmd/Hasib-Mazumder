package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PermissionKeys
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    userProfile: UserProfile?,
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val currentLang = LocalAppLanguage.current
    val isBangla = currentLang == AppLanguage.BANGLA
    val isOwner = userProfile?.role == UserRole.OWNER.name ||
            userProfile?.email.equals("hk3156140@gmail.com", ignoreCase = true)
    val canViewProfit = isOwner || userProfile?.hasPermission(PermissionKeys.VIEW_PROFIT_REPORTS) == true

    val sales by mainViewModel.sales.collectAsState()
    val orders by mainViewModel.orders.collectAsState()
    val expenses by mainViewModel.expenses.collectAsState()
    val products by mainViewModel.products.collectAsState()
    val customers by mainViewModel.customers.collectAsState()
    val suppliers by mainViewModel.suppliers.collectAsState()

    var selectedPeriod by remember { mutableStateOf("ALL") } // ALL, TODAY, MONTH

    val completedSales = sales.filter { it.status == "COMPLETED" }
    val totalSalesRevenue = completedSales.sumOf { it.total }
    val totalDiscountsGiven = completedSales.sumOf { it.discount }
    val totalItemsSold = completedSales.sumOf { s -> s.items.sumOf { it.quantity } }

    var totalCostPrice = 0.0
    for (s in completedSales) {
        for (i in s.items) totalCostPrice += (i.purchasePrice * i.quantity)
    }

    val totalExpensesSum = expenses.sumOf { it.amount }
    val estimatedGrossProfit = if (totalCostPrice > 0) {
        (totalSalesRevenue - totalCostPrice - totalExpensesSum).coerceAtLeast(0.0)
    } else {
        (totalSalesRevenue * 0.40 - totalExpensesSum).coerceAtLeast(0.0)
    }

    val totalCustomerDue = customers.sumOf { it.totalDue }
    val totalSupplierDue = suppliers.sumOf { it.totalDue }

    // Best-selling products calculation
    val productSalesMap = mutableMapOf<String, Int>()
    for (s in completedSales) {
        for (item in s.items) {
            productSalesMap[item.productName] = (productSalesMap[item.productName] ?: 0) + item.quantity
        }
    }
    val bestSellers = productSalesMap.toList().sortedByDescending { it.second }.take(5)

    // Sales by staff calculation
    val staffSalesMap = mutableMapOf<String, Double>()
    for (s in completedSales) {
        val staff = s.staffName.ifBlank { "Staff" }
        staffSalesMap[staff] = (staffSalesMap[staff] ?: 0.0) + s.total
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBangla) "ব্যবসায়িক রিপোর্ট ও মুনাফা" else "Sales & Profit Reports",
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
        containerColor = IlamBlack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedPeriod == "ALL",
                        onClick = { selectedPeriod = "ALL" },
                        label = { Text(if (isBangla) "সার্বিক রিপোর্ট" else "All Time") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IlamGold, selectedLabelColor = IlamBlack)
                    )
                    FilterChip(
                        selected = selectedPeriod == "TODAY",
                        onClick = { selectedPeriod = "TODAY" },
                        label = { Text(if (isBangla) "আজকের দিন" else "Today") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IlamGold, selectedLabelColor = IlamBlack)
                    )
                    FilterChip(
                        selected = selectedPeriod == "MONTH",
                        onClick = { selectedPeriod = "MONTH" },
                        label = { Text(if (isBangla) "এই মাস" else "This Month") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IlamGold, selectedLabelColor = IlamBlack)
                    )
                }
            }

            // Summary Metrics Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IlamDarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isBangla) "প্রধান পরিসংখ্যান" else "Key Performance Indicators",
                            fontWeight = FontWeight.Bold,
                            color = IlamGoldLight,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ReportMetricLine("Total Revenue", "৳${totalSalesRevenue.toInt()}", IlamSuccess)
                        ReportMetricLine("Total Transactions", "${completedSales.size}", IlamWhite)
                        ReportMetricLine("Items Sold", "$totalItemsSold pcs", Color.LightGray)
                        ReportMetricLine("Total Discounts", "৳${totalDiscountsGiven.toInt()}", Color.LightGray)
                        ReportMetricLine("Total Operating Expenses", "৳${totalExpensesSum.toInt()}", IlamWarning)
                        ReportMetricLine("Total Customer Due", "৳${totalCustomerDue.toInt()}", IlamDanger)
                        ReportMetricLine("Total Supplier Due", "৳${totalSupplierDue.toInt()}", IlamWarning)

                        if (canViewProfit) {
                            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 10.dp))
                            ReportMetricLine("Cost of Goods Sold (COGS)", "৳${totalCostPrice.toInt()}", Color.LightGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isBangla) "আনুমানিক নেট লাভ:" else "Estimated Net Profit:",
                                    fontWeight = FontWeight.Bold,
                                    color = IlamGold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "৳${estimatedGrossProfit.toInt()}",
                                    fontWeight = FontWeight.Bold,
                                    color = IlamGold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // Best Selling Products Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IlamDarkCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isBangla) "শীর্ষ বিক্রিত পোশাক" else "Top Selling Clothing Items",
                            fontWeight = FontWeight.Bold,
                            color = IlamWhite,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (bestSellers.isEmpty()) {
                            Text(if (isBangla) "এখনো বিক্রয় রেকর্ড নেই" else "No sales data yet", color = Color.Gray, fontSize = 12.sp)
                        } else {
                            bestSellers.forEachIndexed { idx, (prodName, count) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${idx + 1}. $prodName", color = Color.LightGray, fontSize = 13.sp)
                                    Text("$count sold", fontWeight = FontWeight.Bold, color = IlamGoldLight, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Sales By Staff Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IlamDarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isBangla) "কর্মী ভিত্তিক বিক্রয় পারফরম্যান্স" else "Sales Performance by Staff",
                            fontWeight = FontWeight.Bold,
                            color = IlamWhite,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (staffSalesMap.isEmpty()) {
                            Text("No staff sales yet", color = Color.Gray, fontSize = 12.sp)
                        } else {
                            staffSalesMap.forEach { (staff, rev) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(staff, color = Color.LightGray, fontSize = 13.sp)
                                    Text("৳${rev.toInt()}", fontWeight = FontWeight.Bold, color = IlamSuccess, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportMetricLine(title: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = Color.LightGray, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, color = color, fontSize = 14.sp)
    }
}
