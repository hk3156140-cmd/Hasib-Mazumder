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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PermissionKeys
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.DashboardMetrics
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
import com.example.util.stringRes

data class QuickActionItem(
    val titleEn: String,
    val titleBn: String,
    val icon: ImageVector,
    val iconColor: Color,
    val route: String,
    val requiredPermission: String? = null,
    val ownerOnly: Boolean = false
)

@Composable
fun DashboardScreen(
    userProfile: UserProfile?,
    mainViewModel: MainViewModel,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit,
    onLanguageToggle: () -> Unit
) {
    val currentLang = LocalAppLanguage.current
    val isBangla = currentLang == AppLanguage.BANGLA
    val metrics = mainViewModel.calculateDashboardMetrics()
    val isOwner = userProfile?.role == UserRole.OWNER.name ||
            userProfile?.email.equals("hk3156140@gmail.com", ignoreCase = true)

    val allActions = listOf(
        QuickActionItem("New Sale (POS)", "নতুন বিক্রয় (POS)", Icons.Default.PointOfSale, IlamGold, "pos", PermissionKeys.POS_SALES),
        QuickActionItem("Scan QR / Barcode", "স্ক্যান কোড", Icons.Default.QrCodeScanner, Color(0xFF60A5FA), "scan", PermissionKeys.SCAN),
        QuickActionItem("Products & Stock", "পণ্য তালিকা", Icons.Default.LocalMall, Color(0xFF34D399), "products", PermissionKeys.VIEW_SELLING_PRICE),
        QuickActionItem("Online Orders", "অনলাইন অর্ডার", Icons.Default.ReceiptLong, Color(0xFFFBBF24), "orders", PermissionKeys.MANAGE_ONLINE_ORDERS),
        QuickActionItem("Online Shop", "অনলাইন শপ", Icons.Default.ShoppingBag, Color(0xFFA78BFA), "shop"),
        QuickActionItem("Customers Ledger", "কাস্টমার বকেয়া", Icons.Default.People, Color(0xFF38BDF8), "customers", PermissionKeys.MANAGE_CUSTOMERS),
        QuickActionItem("Expenses & Accounts", "খরচ ও দৈনিক হিসাব", Icons.Default.LocalAtm, Color(0xFFF87171), "expenses", PermissionKeys.MANAGE_EXPENSES),
        QuickActionItem("Sales & Profit Reports", "রিপোর্ট ও লাভ", Icons.Default.Assessment, Color(0xFF818CF8), "reports", PermissionKeys.VIEW_SALES_REPORTS),
        QuickActionItem("Staff & Permissions", "কর্মী ও পারমিশন", Icons.Default.Badge, IlamGold, "staff_permissions", ownerOnly = true),
        QuickActionItem("Settings & Backup", "সেটিংস ও ব্যাকআপ", Icons.Default.Backup, Color(0xFF9CA3AF), "settings")
    )

    // Filter quick actions based on permissions
    val visibleActions = allActions.filter { action ->
        if (action.ownerOnly) return@filter isOwner
        if (action.requiredPermission == null) return@filter true
        userProfile?.hasPermission(action.requiredPermission) == true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(IlamBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- HEADER ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_ilam_logo),
                        contentDescription = "ILAM Logo",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ILAM",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = IlamGoldLight,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = userProfile?.name ?: "User",
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Role Badge
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = userProfile?.role ?: "GUEST",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOwner) IlamGold else Color.LightGray
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = IlamDarkSurface
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = if (isOwner) IlamGold else Color.DarkGray
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onLanguageToggle) {
                        Icon(Icons.Default.Language, contentDescription = "Lang", tint = IlamGoldLight)
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = Color.Gray)
                    }
                }
            }
        }

        // --- LOW STOCK ALERT BANNER (If any) ---
        if (metrics.lowStockCount > 0 || metrics.outOfStockCount > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1616))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Alert", tint = IlamWarning)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBangla) "স্টক সতর্কতা!" else "Stock Alert!",
                                fontWeight = FontWeight.Bold,
                                color = IlamWhite,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isBangla)
                                    "কম স্টক: ${metrics.lowStockCount} টি, স্টক শেষ: ${metrics.outOfStockCount} টি"
                                else
                                    "Low stock: ${metrics.lowStockCount}, Out of stock: ${metrics.outOfStockCount}",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // --- SECTION: TODAY'S METRICS ---
        item {
            Text(
                text = if (isBangla) "আজকের সারসংক্ষেপ" else "Today's Business Pulse",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = IlamGoldLight
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = if (isBangla) "আজকের বিক্রয়" else "Today's Sales",
                        value = "৳${metrics.todaySalesTotal.toInt()}",
                        subtitle = "${metrics.todaySalesCount} ${if (isBangla) "বিক্রয়" else "Sales"}",
                        valueColor = IlamSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = if (isBangla) "আজকের অর্ডার" else "Today's Orders",
                        value = "${metrics.todayOrdersCount}",
                        subtitle = if (isBangla) "অনলাইন অর্ডার" else "Online Orders",
                        valueColor = Color(0xFF60A5FA),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = if (isBangla) "আজকের বকেয়া" else "Today's Due",
                        value = "৳${metrics.todayDueTotal.toInt()}",
                        subtitle = if (isBangla) "কাস্টমার বকেয়া" else "Customer Due",
                        valueColor = IlamDanger,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = if (isBangla) "আজকের খরচ" else "Today's Expenses",
                        value = "৳${metrics.todayExpensesTotal.toInt()}",
                        subtitle = if (isBangla) "দোকানের খরচ" else "Daily Expenses",
                        valueColor = IlamWarning,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isOwner || userProfile?.hasPermission(PermissionKeys.VIEW_PROFIT_REPORTS) == true) {
                    MetricCard(
                        title = if (isBangla) "আনুমানিক লাভ" else "Estimated Profit",
                        value = "৳${metrics.estimatedProfit.toInt()}",
                        subtitle = if (isBangla) "মোট বিক্রয় - আনুমানিক ব্যয় ও খরচ" else "Gross Sales - Est. Cost & Expenses",
                        valueColor = IlamGold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // --- SECTION: QUICK ACTIONS ---
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isBangla) "দ্রুত কাজ (Quick Actions)" else "Quick Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = IlamGoldLight
            )
        }

        // Quick Actions Grid (rendered directly inside items)
        val chunkedActions = visibleActions.chunked(2)
        chunkedActions.forEach { rowActions ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowActions.forEach { action ->
                        QuickActionCard(
                            action = action,
                            isBangla = isBangla,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigate(action.route) }
                        )
                    }
                    if (rowActions.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = IlamDarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.sp, color = Color.LightGray)
        }
    }
}

@Composable
fun QuickActionCard(
    action: QuickActionItem,
    isBangla: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("action_${action.route}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = IlamDarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(action.iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.titleEn,
                    tint = action.iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isBangla) action.titleBn else action.titleEn,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = IlamWhite
            )
        }
    }
}
