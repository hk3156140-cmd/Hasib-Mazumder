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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineOrdersScreen(
    userProfile: UserProfile?,
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val currentLang = LocalAppLanguage.current
    val isBangla = currentLang == AppLanguage.BANGLA

    val orders by mainViewModel.orders.collectAsState()
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    // Dialog state for manual courier booking
    var courierOrderTarget by remember { mutableStateOf<Order?>(null) }
    var courierCompanyName by remember { mutableStateOf("Steadfast Courier") }
    var consignmentTrackingNo by remember { mutableStateOf("") }

    val allStatuses = listOf(
        "ALL", "Pending", "Confirmed", "Preparing", "Ready",
        "Courier Booked", "Picked Up", "In Transit", "Delivered", "Cancelled", "Returned"
    )

    val filteredOrders = if (selectedStatusFilter == "ALL") orders else orders.filter { it.orderStatus == selectedStatusFilter }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBangla) "অনলাইন অর্ডার ও ডেলিভারি" else "Online Orders & Delivery",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Status Tabs Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allStatuses) { st ->
                    FilterChip(
                        selected = selectedStatusFilter == st,
                        onClick = { selectedStatusFilter = st },
                        label = { Text(st) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IlamGold,
                            selectedLabelColor = IlamBlack
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredOrders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isBangla) "এই স্ট্যাটাসে কোন অর্ডার নেই" else "No orders in this status",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredOrders) { order ->
                        OrderItemCard(
                            order = order,
                            isBangla = isBangla,
                            onStatusChange = { newStatus ->
                                mainViewModel.updateOrderStatus(order.id, newStatus)
                            },
                            onOpenCourierBooking = {
                                courierOrderTarget = order
                                courierCompanyName = order.courierName.ifBlank { "Steadfast Courier" }
                                consignmentTrackingNo = order.trackingNumber
                            }
                        )
                    }
                }
            }
        }
    }

    // --- MANUAL COURIER BOOKING MODAL (Section 17) ---
    if (courierOrderTarget != null) {
        val order = courierOrderTarget!!
        AlertDialog(
            onDismissRequest = { courierOrderTarget = null },
            title = {
                Text(
                    text = if (isBangla) "কুরিয়ার বুকিং তথ্য" else "Courier Consignment Details",
                    fontWeight = FontWeight.Bold,
                    color = IlamGoldLight,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Order: ${order.orderNumber}", color = IlamWhite, fontWeight = FontWeight.SemiBold)
                    Text("Customer: ${order.customerName} (${order.customerPhone})", color = Color.Gray, fontSize = 12.sp)
                    Text("Address: ${order.deliveryAddress}", color = Color.LightGray, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = courierCompanyName,
                        onValueChange = { courierCompanyName = it },
                        label = { Text(if (isBangla) "কুরিয়ার কোম্পানি (যেমন Steadfast, Pathao, RedX)" else "Courier Company") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = consignmentTrackingNo,
                        onValueChange = { consignmentTrackingNo = it },
                        label = { Text(if (isBangla) "কনসাইনমেন্ট / ট্র্যাকিং নম্বর" else "Consignment / Tracking Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mainViewModel.updateOrderStatus(
                            orderId = order.id,
                            newStatus = "Courier Booked",
                            courierName = courierCompanyName,
                            trackingNumber = consignmentTrackingNo
                        )
                        courierOrderTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
                ) {
                    Text(if (isBangla) "বুকিং সেভ করুন" else "Save Courier Booking")
                }
            },
            dismissButton = {
                TextButton(onClick = { courierOrderTarget = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel", color = Color.Gray)
                }
            },
            containerColor = IlamDarkSurface
        )
    }
}

@Composable
fun OrderItemCard(
    order: Order,
    isBangla: Boolean,
    onStatusChange: (String) -> Unit,
    onOpenCourierBooking: () -> Unit
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }

    val statusColor = when (order.orderStatus) {
        "Delivered" -> IlamSuccess
        "Cancelled", "Returned" -> IlamDanger
        "Pending" -> IlamWarning
        else -> Color(0xFF60A5FA)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = IlamDarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(order.orderNumber, fontWeight = FontWeight.Bold, color = IlamWhite, fontSize = 15.sp)
                    Text("${order.customerName} • ${order.customerPhone}", color = Color.Gray, fontSize = 12.sp)
                }

                // Status Badge Dropdown
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.18f))
                            .clickable { statusMenuExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(order.orderStatus, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Change", tint = statusColor)
                    }

                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        listOf(
                            "Pending", "Confirmed", "Preparing", "Ready",
                            "Courier Booked", "Picked Up", "In Transit", "Delivered", "Cancelled", "Returned"
                        ).forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = {
                                    statusMenuExpanded = false
                                    onStatusChange(st)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Delivery method & address
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (order.deliveryMethod == "STORE_PICKUP") Icons.Default.Store else Icons.Default.LocalShipping,
                    contentDescription = "Method",
                    tint = IlamGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (order.deliveryMethod == "STORE_PICKUP")
                        (if (isBangla) "স্টোর পিকআপ" else "Store Pickup")
                    else
                        "${if (isBangla) "কুরিয়ার: " else "Courier: "}${order.deliveryAddress}",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            if (order.courierName.isNotBlank() || order.trackingNumber.isNotBlank()) {
                Text(
                    text = "${order.courierName} | Tracking: ${order.trackingNumber}",
                    color = Color(0xFF60A5FA),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

            // Order items summary
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${item.productName} (${item.size}/${item.color}) x ${item.quantity}", color = IlamWhite, fontSize = 12.sp)
                    Text("৳${(item.unitPrice * item.quantity).toInt()}", color = IlamWhite, fontSize = 12.sp)
                }
            }

            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total: ৳${order.total.toInt()} (Delivery ৳${order.deliveryCharge.toInt()})",
                        fontWeight = FontWeight.Bold,
                        color = IlamGoldLight,
                        fontSize = 14.sp
                    )
                }

                if (order.deliveryMethod == "COURIER") {
                    OutlinedButton(
                        onClick = onOpenCourierBooking,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = IlamGoldLight)
                    ) {
                        Text(if (isBangla) "কুরিয়ার বুকিং" else "Courier Booking", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
