package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun ExpenseLedgerScreen(
    userProfile: UserProfile?,
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val currentLang = LocalAppLanguage.current
    val isBangla = currentLang == AppLanguage.BANGLA
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val expenses by mainViewModel.expenses.collectAsState()
    val sales by mainViewModel.sales.collectAsState()

    var activeTab by remember { mutableStateOf("EXPENSES") } // EXPENSES or DAILY_LEDGER

    var showAddExpenseModal by remember { mutableStateOf(false) }

    // Daily Ledger user inputs
    var openingCashText by remember { mutableStateOf("5000") }
    var otherWithdrawalsText by remember { mutableStateOf("0") }

    val expenseCategories = listOf("Rent", "Electricity", "Internet", "Transport", "Packaging", "Marketing", "Salary", "Other")

    val todaySales = sales.filter {
        val sDate = it.createdAt?.toDate()?.let { d -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(d) }
        sDate == todayDateStr && it.status == "COMPLETED"
    }
    val todayCashSales = todaySales.filter { it.paymentMethod == "CASH" }.sumOf { it.paidAmount }
    val todayAllSalesTotal = todaySales.sumOf { it.total }
    val todayDiscountTotal = todaySales.sumOf { it.discount }
    val todayDueTotal = todaySales.sumOf { it.dueAmount }

    val todayExpensesList = expenses.filter { it.date == todayDateStr }
    val todayExpensesTotal = todayExpensesList.sumOf { it.amount }

    val openingCash = openingCashText.toDoubleOrNull() ?: 0.0
    val withdrawals = otherWithdrawalsText.toDoubleOrNull() ?: 0.0
    val closingCash = openingCash + todayCashSales - todayExpensesTotal - withdrawals

    var totalCost = 0.0
    for (s in todaySales) {
        for (i in s.items) totalCost += (i.purchasePrice * i.quantity)
    }
    val estimatedProfit = if (totalCost > 0) {
        (todayAllSalesTotal - totalCost - todayExpensesTotal).coerceAtLeast(0.0)
    } else {
        (todayAllSalesTotal * 0.40 - todayExpensesTotal).coerceAtLeast(0.0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBangla) "দৈনিক খরচ ও হিসাব লেজার" else "Expenses & Daily Ledger",
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
            if (activeTab == "EXPENSES") {
                FloatingActionButton(
                    onClick = { showAddExpenseModal = true },
                    containerColor = IlamGold,
                    contentColor = IlamBlack,
                    modifier = Modifier.testTag("add_expense_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
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

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = activeTab == "EXPENSES",
                    onClick = { activeTab = "EXPENSES" },
                    label = { Text(if (isBangla) "দোকানের খরচ সমূহ" else "Expenses") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IlamGold, selectedLabelColor = IlamBlack)
                )
                FilterChip(
                    selected = activeTab == "DAILY_LEDGER",
                    onClick = { activeTab = "DAILY_LEDGER" },
                    label = { Text(if (isBangla) "আজকের ক্যাশ হিসাব" else "Daily Ledger") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IlamGold, selectedLabelColor = IlamBlack)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (activeTab == "EXPENSES") {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(expenses) { exp ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = IlamDarkSurface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("expense_${exp.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exp.category, fontWeight = FontWeight.Bold, color = IlamWhite, fontSize = 15.sp)
                                    if (exp.description.isNotBlank()) {
                                        Text(exp.description, color = Color.LightGray, fontSize = 12.sp)
                                    }
                                    Text("${exp.date} • by ${exp.addedByName}", color = Color.Gray, fontSize = 11.sp)
                                }

                                Text("৳${exp.amount.toInt()}", fontWeight = FontWeight.Bold, color = IlamWarning, fontSize = 17.sp)
                            }
                        }
                    }
                }
            } else {
                // --- DAILY ACCOUNTS LEDGER (Section 22) ---
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = IlamDarkSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (isBangla) "আজকের ক্যাশ ক্লোজিং ক্যালকুলেটর" else "Daily Cash Closing Statement",
                                    fontWeight = FontWeight.Bold,
                                    color = IlamGoldLight,
                                    fontSize = 16.sp
                                )
                                Text("Date: $todayDateStr", color = Color.Gray, fontSize = 12.sp)

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = openingCashText,
                                    onValueChange = { openingCashText = it },
                                    label = { Text(if (isBangla) "প্রারম্ভিক ক্যাশ (Opening Cash)" else "Opening Cash (৳)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = otherWithdrawalsText,
                                    onValueChange = { otherWithdrawalsText = it },
                                    label = { Text(if (isBangla) "মালিকের উত্তোলন / অন্যান্য ড্রয়িং" else "Owner Withdrawals (৳)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))

                                LedgerRow(if (isBangla) "প্রারম্ভিক ক্যাশ (Opening Cash)" else "Opening Cash", "+৳${openingCash.toInt()}", Color.LightGray)
                                LedgerRow(if (isBangla) "নগদ বিক্রয় (Cash Sales)" else "Cash Sales", "+৳${todayCashSales.toInt()}", IlamSuccess)
                                LedgerRow(if (isBangla) "আজকের খরচ (Expenses)" else "Expenses Paid", "-৳${todayExpensesTotal.toInt()}", IlamWarning)
                                LedgerRow(if (isBangla) "উত্তোলন (Withdrawals)" else "Withdrawals", "-৳${withdrawals.toInt()}", IlamDanger)

                                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isBangla) "সমাপনী ক্যাশ (Closing Cash):" else "Closing Cash in Drawer:",
                                        fontWeight = FontWeight.Bold,
                                        color = IlamGoldLight,
                                        fontSize = 15.sp
                                    )
                                    Text("৳${closingCash.toInt()}", fontWeight = FontWeight.Bold, color = IlamGoldLight, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = IlamDarkCard),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (isBangla) "আজকের মুনাফা ও পরিসংখ্যান" else "Business Performance & Estimated Profit",
                                    fontWeight = FontWeight.Bold,
                                    color = IlamWhite,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LedgerRow("Total Sales", "৳${todayAllSalesTotal.toInt()}", IlamWhite)
                                LedgerRow("Discounts Given", "-৳${todayDiscountTotal.toInt()}", Color.LightGray)
                                LedgerRow("Customer Due Added", "৳${todayDueTotal.toInt()}", IlamDanger)
                                LedgerRow("Expenses Incurred", "৳${todayExpensesTotal.toInt()}", IlamWarning)
                                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(if (isBangla) "আনুমানিক গ্রস লাভ" else "Estimated Gross Profit", fontWeight = FontWeight.Bold, color = IlamGold, fontSize = 14.sp)
                                        Text(if (isBangla) "(মোট বিক্রয় - ক্রয় মূল্য - খরচ)" else "(Sales - Cost - Expenses)", color = Color.Gray, fontSize = 11.sp)
                                    }
                                    Text("৳${estimatedProfit.toInt()}", fontWeight = FontWeight.Bold, color = IlamGold, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ADD EXPENSE MODAL ---
    if (showAddExpenseModal) {
        var selectedCat by remember { mutableStateOf("Rent") }
        var amountText by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddExpenseModal = false },
            title = { Text(if (isBangla) "নতুন খরচ যুক্ত করুন" else "Record Showroom Expense", color = IlamGoldLight, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (isBangla) "ক্যাটাগরি নির্বাচন করুন:" else "Expense Category:", color = Color.LightGray, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(expenseCategories) { c ->
                            FilterChip(
                                selected = selectedCat == c,
                                onClick = { selectedCat = c },
                                label = { Text(c) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IlamGold, selectedLabelColor = IlamBlack)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (৳) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description / Note") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            mainViewModel.addExpense(
                                category = selectedCat,
                                amount = amount,
                                description = desc,
                                addedById = userProfile?.userId ?: "STAFF",
                                addedByName = userProfile?.name ?: "Staff"
                            )
                        }
                        showAddExpenseModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
                ) {
                    Text(if (isBangla) "যোগ করুন" else "Save Expense")
                }
            },
            dismissButton = { TextButton(onClick = { showAddExpenseModal = false }) { Text("Cancel", color = Color.Gray) } },
            containerColor = IlamDarkSurface
        )
    }
}

@Composable
fun LedgerRow(title: String, value: String, color: Color) {
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
