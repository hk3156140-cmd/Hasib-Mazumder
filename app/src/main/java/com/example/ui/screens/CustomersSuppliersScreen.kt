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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payment
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
import com.example.data.model.Customer
import com.example.data.model.Supplier
import com.example.data.model.UserProfile
import com.example.ui.MainViewModel
import com.example.ui.theme.IlamBlack
import com.example.ui.theme.IlamDanger
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
fun CustomersSuppliersScreen(
    userProfile: UserProfile?,
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val currentLang = LocalAppLanguage.current
    val isBangla = currentLang == AppLanguage.BANGLA

    val customers by mainViewModel.customers.collectAsState()
    val suppliers by mainViewModel.suppliers.collectAsState()

    var activeTab by remember { mutableStateOf("CUSTOMERS") } // CUSTOMERS or SUPPLIERS

    // Modals
    var showAddCustomerModal by remember { mutableStateOf(false) }
    var showAddSupplierModal by remember { mutableStateOf(false) }
    var customerForPayment by remember { mutableStateOf<Customer?>(null) }
    var supplierForPayment by remember { mutableStateOf<Supplier?>(null) }
    var paymentAmountText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBangla) "কাস্টমার ও সাপ্লায়ার লেজার" else "Customer & Supplier Ledgers",
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
            FloatingActionButton(
                onClick = {
                    if (activeTab == "CUSTOMERS") showAddCustomerModal = true else showAddSupplierModal = true
                },
                containerColor = IlamGold,
                contentColor = IlamBlack,
                modifier = Modifier.testTag("add_party_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
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

            // Tab Selector
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = activeTab == "CUSTOMERS",
                    onClick = { activeTab = "CUSTOMERS" },
                    label = { Text(if (isBangla) "কাস্টমার বকেয়া (${customers.size})" else "Customers (${customers.size})") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IlamGold, selectedLabelColor = IlamBlack)
                )
                FilterChip(
                    selected = activeTab == "SUPPLIERS",
                    onClick = { activeTab = "SUPPLIERS" },
                    label = { Text(if (isBangla) "সাপ্লায়ার দেনা (${suppliers.size})" else "Suppliers (${suppliers.size})") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IlamGold, selectedLabelColor = IlamBlack)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (activeTab == "CUSTOMERS") {
                // --- CUSTOMERS LIST ---
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(customers) { cust ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = IlamDarkSurface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("cust_${cust.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(cust.name, fontWeight = FontWeight.Bold, color = IlamWhite, fontSize = 15.sp)
                                        Text(cust.phone, color = Color.Gray, fontSize = 12.sp)
                                        if (cust.address.isNotBlank()) {
                                            Text(cust.address, color = Color.LightGray, fontSize = 11.sp)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isBangla) "বকেয়া: ৳${cust.totalDue.toInt()}" else "Due: ৳${cust.totalDue.toInt()}",
                                            fontWeight = FontWeight.Bold,
                                            color = if (cust.totalDue > 0) IlamDanger else IlamSuccess,
                                            fontSize = 15.sp
                                        )
                                        Text("Purchases: ৳${cust.totalPurchases.toInt()}", color = Color.Gray, fontSize = 11.sp)
                                    }
                                }

                                if (cust.totalDue > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        Button(
                                            onClick = {
                                                customerForPayment = cust
                                                paymentAmountText = cust.totalDue.toInt().toString()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Payment, contentDescription = "Pay")
                                            Text(if (isBangla) " টাকা জমা নিন" else " Collect Due", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // --- SUPPLIERS LIST ---
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(suppliers) { sup ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = IlamDarkSurface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("sup_${sup.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(sup.name, fontWeight = FontWeight.Bold, color = IlamWhite, fontSize = 15.sp)
                                        Text(sup.phone, color = Color.Gray, fontSize = 12.sp)
                                        if (sup.address.isNotBlank()) {
                                            Text(sup.address, color = Color.LightGray, fontSize = 11.sp)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isBangla) "দেনা: ৳${sup.totalDue.toInt()}" else "Payable: ৳${sup.totalDue.toInt()}",
                                            fontWeight = FontWeight.Bold,
                                            color = if (sup.totalDue > 0) IlamWarning else IlamSuccess,
                                            fontSize = 15.sp
                                        )
                                        Text("Total Buy: ৳${sup.totalPurchase.toInt()}", color = Color.Gray, fontSize = 11.sp)
                                    }
                                }

                                if (sup.totalDue > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        Button(
                                            onClick = {
                                                supplierForPayment = sup
                                                paymentAmountText = sup.totalDue.toInt().toString()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Payment, contentDescription = "Pay")
                                            Text(if (isBangla) " দেনা পরিশোধ করুন" else " Pay Supplier", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ADD CUSTOMER MODAL ---
    if (showAddCustomerModal) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCustomerModal = false },
            title = { Text(if (isBangla) "নতুন কাস্টমার যোগ করুন" else "Add New Customer", color = IlamGoldLight, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Customer Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank() || phone.isBlank()) return@Button
                        mainViewModel.addCustomer(name, phone, email, address)
                        showAddCustomerModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
                ) {
                    Text(if (isBangla) "যোগ করুন" else "Add Customer")
                }
            },
            dismissButton = { TextButton(onClick = { showAddCustomerModal = false }) { Text("Cancel", color = Color.Gray) } },
            containerColor = IlamDarkSurface
        )
    }

    // --- ADD SUPPLIER MODAL ---
    if (showAddSupplierModal) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSupplierModal = false },
            title = { Text(if (isBangla) "নতুন সাপ্লায়ার যোগ করুন" else "Add New Supplier", color = IlamGoldLight, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Supplier / Company Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank() || phone.isBlank()) return@Button
                        mainViewModel.addSupplier(name, phone, address)
                        showAddSupplierModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
                ) {
                    Text(if (isBangla) "যোগ করুন" else "Add Supplier")
                }
            },
            dismissButton = { TextButton(onClick = { showAddSupplierModal = false }) { Text("Cancel", color = Color.Gray) } },
            containerColor = IlamDarkSurface
        )
    }

    // --- RECORD DUE PAYMENT MODAL ---
    if (customerForPayment != null) {
        val cust = customerForPayment!!
        AlertDialog(
            onDismissRequest = { customerForPayment = null },
            title = { Text(if (isBangla) "কাস্টমার বকেয়া টাকা জমা" else "Collect Due Payment", color = IlamGoldLight) },
            text = {
                Column {
                    Text(cust.name, fontWeight = FontWeight.Bold, color = IlamWhite)
                    Text("Current Due: ৳${cust.totalDue.toInt()}", color = IlamDanger, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = paymentAmountText,
                        onValueChange = { paymentAmountText = it },
                        label = { Text("Payment Amount (৳)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            mainViewModel.recordCustomerPayment(cust.id, amount)
                        }
                        customerForPayment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
                ) {
                    Text(if (isBangla) "জমা নিশ্চিত করুন" else "Record Payment")
                }
            },
            dismissButton = { TextButton(onClick = { customerForPayment = null }) { Text("Cancel", color = Color.Gray) } },
            containerColor = IlamDarkSurface
        )
    }

    // --- PAY SUPPLIER MODAL ---
    if (supplierForPayment != null) {
        val sup = supplierForPayment!!
        AlertDialog(
            onDismissRequest = { supplierForPayment = null },
            title = { Text(if (isBangla) "সাপ্লায়ার বকেয়া পরিশোধ" else "Pay Supplier Due", color = IlamGoldLight) },
            text = {
                Column {
                    Text(sup.name, fontWeight = FontWeight.Bold, color = IlamWhite)
                    Text("Total Payable: ৳${sup.totalDue.toInt()}", color = IlamWarning, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = paymentAmountText,
                        onValueChange = { paymentAmountText = it },
                        label = { Text("Paid Amount (৳)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            mainViewModel.recordSupplierPayment(sup.id, amount)
                        }
                        supplierForPayment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
                ) {
                    Text(if (isBangla) "পরিশোধ নিশ্চিত করুন" else "Confirm Payment")
                }
            },
            dismissButton = { TextButton(onClick = { supplierForPayment = null }) { Text("Cancel", color = Color.Gray) } },
            containerColor = IlamDarkSurface
        )
    }
}
