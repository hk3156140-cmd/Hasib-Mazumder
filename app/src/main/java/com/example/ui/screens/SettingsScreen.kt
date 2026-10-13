package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessSettings
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userProfile: UserProfile?,
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onLanguageToggle: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentLang = LocalAppLanguage.current
    val isBangla = currentLang == AppLanguage.BANGLA
    val isOwner = userProfile?.role == UserRole.OWNER.name ||
            userProfile?.email.equals("hk3156140@gmail.com", ignoreCase = true)
    val canChangeSettings = isOwner || userProfile?.hasPermission(PermissionKeys.CHANGE_BUSINESS_SETTINGS) == true

    val businessSettings by mainViewModel.businessSettings.collectAsState()
    val auditLogs by mainViewModel.auditLogs.collectAsState()

    var storeName by remember(businessSettings) { mutableStateOf(businessSettings.businessName) }
    var storePhone by remember(businessSettings) { mutableStateOf(businessSettings.phone) }
    var storeAddress by remember(businessSettings) { mutableStateOf(businessSettings.address) }
    var currency by remember(businessSettings) { mutableStateOf(businessSettings.currency) }
    var insideDhakaCharge by remember(businessSettings) {
        mutableStateOf(businessSettings.insideDhakaDeliveryCharge.toString())
    }
    var outsideDhakaCharge by remember(businessSettings) {
        mutableStateOf(businessSettings.outsideDhakaDeliveryCharge.toString())
    }
    var bkashNumber by remember(businessSettings) { mutableStateOf(businessSettings.bkashNumber) }
    var storePickupEnabled by remember(businessSettings) {
        mutableStateOf(businessSettings.storePickupAvailable)
    }
    var codEnabled by remember(businessSettings) {
        mutableStateOf(businessSettings.cashOnDeliveryEnabled)
    }

    var showBackupDialog by remember { mutableStateOf(false) }
    var backupJsonContent by remember { mutableStateOf("") }
    var isBackingUp by remember { mutableStateOf(false) }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var isRestoring by remember { mutableStateOf(false) }

    var showAuditLogsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBangla) "সেটিংস ও ব্যাকআপ" else "Settings & Backup",
                        color = IlamWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = IlamWhite)
                    }
                },
                actions = {
                    IconButton(onClick = onLanguageToggle, modifier = Modifier.testTag("settings_lang_btn")) {
                        Icon(Icons.Default.Language, contentDescription = "Language", tint = IlamGoldLight)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // --- STORE PROFILE SETTINGS ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = IlamDarkCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = IlamGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "শোরুম ও ব্যবসার তথ্য" else "Showroom & Business Info",
                                fontWeight = FontWeight.Bold,
                                color = IlamGoldLight,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = storeName,
                            onValueChange = { storeName = it },
                            label = { Text(if (isBangla) "দোকানের নাম" else "Store Name") },
                            enabled = canChangeSettings,
                            modifier = Modifier.fillMaxWidth().testTag("store_name_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = storePhone,
                            onValueChange = { storePhone = it },
                            label = { Text(if (isBangla) "মোবাইল নম্বর" else "Contact Phone") },
                            enabled = canChangeSettings,
                            modifier = Modifier.fillMaxWidth().testTag("store_phone_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = storeAddress,
                            onValueChange = { storeAddress = it },
                            label = { Text(if (isBangla) "শোরুমের ঠিকানা" else "Showroom Address") },
                            enabled = canChangeSettings,
                            modifier = Modifier.fillMaxWidth().testTag("store_address_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it },
                            label = { Text(if (isBangla) "মুদ্রা প্রতীক" else "Currency Symbol") },
                            enabled = canChangeSettings,
                            modifier = Modifier.fillMaxWidth().testTag("store_currency_input"),
                            singleLine = true
                        )
                    }
                }
            }

            // --- COURIER & DELIVERY SETTINGS ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = IlamDarkCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFF60A5FA))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "কুরিয়ার ও ডেলিভারি চার্জ" else "Courier & Delivery Settings",
                                fontWeight = FontWeight.Bold,
                                color = IlamWhite,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = insideDhakaCharge,
                            onValueChange = { insideDhakaCharge = it },
                            label = { Text(if (isBangla) "ঢাকার ভিতর চার্জ (৳)" else "Inside Dhaka Charge (৳)") },
                            enabled = canChangeSettings,
                            modifier = Modifier.fillMaxWidth().testTag("delivery_inside_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = outsideDhakaCharge,
                            onValueChange = { outsideDhakaCharge = it },
                            label = { Text(if (isBangla) "ঢাকার বাইরে চার্জ (৳)" else "Outside Dhaka Charge (৳)") },
                            enabled = canChangeSettings,
                            modifier = Modifier.fillMaxWidth().testTag("delivery_outside_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isBangla) "শোরুম থেকে পিকআপ চালু" else "Enable Store Pickup",
                                color = IlamWhite,
                                fontSize = 14.sp
                            )
                            Switch(
                                checked = storePickupEnabled,
                                onCheckedChange = { if (canChangeSettings) storePickupEnabled = it },
                                enabled = canChangeSettings,
                                colors = SwitchDefaults.colors(checkedThumbColor = IlamGold, checkedTrackColor = IlamGoldLight)
                            )
                        }
                    }
                }
            }

            // --- PAYMENT NUMBERS ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = IlamDarkCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = IlamSuccess)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "পেমেন্ট মাধ্যম সেটিংস" else "Payment Methods",
                                fontWeight = FontWeight.Bold,
                                color = IlamWhite,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = bkashNumber,
                            onValueChange = { bkashNumber = it },
                            label = { Text(if (isBangla) "বিকাশ মার্চেন্ট / পার্সোনাল নম্বর" else "bKash Number") },
                            enabled = canChangeSettings,
                            modifier = Modifier.fillMaxWidth().testTag("bkash_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isBangla) "ক্যাশ অন ডেলিভারি (COD)" else "Cash on Delivery (COD)",
                                color = IlamWhite,
                                fontSize = 14.sp
                            )
                            Switch(
                                checked = codEnabled,
                                onCheckedChange = { if (canChangeSettings) codEnabled = it },
                                enabled = canChangeSettings,
                                colors = SwitchDefaults.colors(checkedThumbColor = IlamGold, checkedTrackColor = IlamGoldLight)
                            )
                        }
                    }
                }
            }

            // SAVE SETTINGS BUTTON
            if (canChangeSettings) {
                item {
                    Button(
                        onClick = {
                            val updated = businessSettings.copy(
                                businessName = storeName,
                                phone = storePhone,
                                address = storeAddress,
                                currency = currency,
                                insideDhakaDeliveryCharge = insideDhakaCharge.toDoubleOrNull() ?: 70.0,
                                outsideDhakaDeliveryCharge = outsideDhakaCharge.toDoubleOrNull() ?: 130.0,
                                bkashNumber = bkashNumber,
                                storePickupAvailable = storePickupEnabled,
                                cashOnDeliveryEnabled = codEnabled
                            )
                            mainViewModel.updateBusinessSettings(updated)
                            Toast.makeText(
                                context,
                                if (isBangla) "সেটিংস সফলভাবে সংরক্ষিত হয়েছে" else "Settings saved successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_settings_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBangla) "সেটিংস সংরক্ষণ করুন" else "Save Settings",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- BACKUP & RESTORE SECTION ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = IlamDarkCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = IlamWarning)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "ডাটাবেজ ব্যাকআপ ও রিস্টোর" else "Database Backup & Restore",
                                fontWeight = FontWeight.Bold,
                                color = IlamWhite,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (isBangla)
                                "সম্পূর্ণ প্রোডাক্ট, স্টক, বিক্রয় এবং কাস্টমার লেজার ব্যাকআপ ফাইল ডাউনলোড বা এক্সপোর্ট করুন।"
                            else
                                "Export complete snapshot of products, stock, sales, and accounts as JSON.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isBackingUp = true
                                    scope.launch {
                                        try {
                                            val json = mainViewModel.exportBackupJson()
                                            backupJsonContent = json
                                            showBackupDialog = true
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Backup error: ${e.message}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isBackingUp = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("export_backup_btn"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = IlamGoldLight)
                            ) {
                                if (isBackingUp) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = IlamGold)
                                } else {
                                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isBangla) "ব্যাকআপ নিন" else "Export Backup", fontSize = 13.sp)
                                }
                            }

                            if (isOwner) {
                                OutlinedButton(
                                    onClick = { showRestoreDialog = true },
                                    modifier = Modifier.weight(1f).testTag("restore_backup_btn"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF60A5FA))
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isBangla) "রিস্টোর করুন" else "Restore JSON", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // --- AUDIT LOGS SECTION ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = IlamDarkCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, tint = IlamGoldLight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBangla) "কার্যকলাপ অডিট লগ (${auditLogs.size})" else "Activity Audit Logs (${auditLogs.size})",
                                    fontWeight = FontWeight.Bold,
                                    color = IlamWhite,
                                    fontSize = 15.sp
                                )
                            }
                            TextButton(onClick = { showAuditLogsDialog = true }) {
                                Text(if (isBangla) "সব দেখুন" else "View All", color = IlamGold)
                            }
                        }
                    }
                }
            }

            // --- SIGN OUT BUTTON ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_sign_out_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IlamDanger, contentColor = IlamWhite)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBangla) "লগ আউট (Sign Out)" else "Sign Out",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // BACKUP JSON DIALOG
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = {
                Text(
                    text = if (isBangla) "ব্যাকআপ ফাইল তৈরি সম্পন্ন" else "Backup Generated Successfully",
                    color = IlamGoldLight,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isBangla)
                            "সম্পূর্ণ ডাটাবেজ ব্যাকআপ JSON হিসেবে কপি করুন।"
                        else
                            "Copy complete backup JSON to clipboard or save for restore.",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = backupJsonContent.take(500) + if (backupJsonContent.length > 500) "\n\n...[${backupJsonContent.length} bytes total]" else "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ILAM Backup", backupJsonContent))
                        Toast.makeText(context, if (isBangla) "ক্লিপবোর্ডে কপি করা হয়েছে" else "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        showBackupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBangla) "কপি করুন" else "Copy JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text(if (isBangla) "বন্ধ করুন" else "Close", color = Color.Gray)
                }
            },
            containerColor = IlamDarkSurface
        )
    }

    // RESTORE BACKUP DIALOG
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { if (!isRestoring) showRestoreDialog = false },
            title = {
                Text(
                    text = if (isBangla) "ডাটাবেজ রিস্টোর করুন" else "Restore Database from JSON",
                    color = IlamDanger,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isBangla)
                            "সতর্কতা: রিস্টোর করলে ব্যাকআপ ফাইলের ডাটা ফায়ারবেস ক্লাউডে ওভাররাইট/আপডেট হবে। পূর্বে কপি করা JSON পেস্ট করুন:"
                        else
                            "Warning: Restoring will overwrite existing items from the backup JSON file. Paste JSON content below:",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        placeholder = { Text("Paste JSON here...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonInput.isBlank()) {
                            Toast.makeText(context, "Please paste JSON backup first", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isRestoring = true
                        mainViewModel.restoreBackup(restoreJsonInput) { success, msg ->
                            isRestoring = false
                            if (success) {
                                Toast.makeText(context, if (isBangla) "রিস্টোর সম্পন্ন হয়েছে!" else "Restore completed!", Toast.LENGTH_LONG).show()
                                showRestoreDialog = false
                                restoreJsonInput = ""
                            } else {
                                Toast.makeText(context, "Error: $msg", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isRestoring,
                    colors = ButtonDefaults.buttonColors(containerColor = IlamDanger, contentColor = IlamWhite)
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = IlamWhite, strokeWidth = 2.dp)
                    } else {
                        Text(if (isBangla) "রিস্টোর নিশ্চিত করুন" else "Confirm Restore")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }, enabled = !isRestoring) {
                    Text(if (isBangla) "বাতিল" else "Cancel", color = Color.Gray)
                }
            },
            containerColor = IlamDarkSurface
        )
    }

    // AUDIT LOGS DIALOG
    if (showAuditLogsDialog) {
        val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { showAuditLogsDialog = false },
            title = {
                Text(
                    text = if (isBangla) "কার্যকলাপ অডিট হিস্টোরি" else "Audit Log History",
                    color = IlamGoldLight,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    if (auditLogs.isEmpty()) {
                        Text(
                            text = if (isBangla) "কোনো লগ পাওয়া যায়নি" else "No audit logs found",
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(auditLogs) { log ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = IlamDarkCard)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = log.action,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = IlamGold
                                            )
                                            Text(
                                                text = log.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = log.details,
                                            fontSize = 12.sp,
                                            color = IlamWhite
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${log.actorName} (${log.actorEmail})",
                                            fontSize = 10.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAuditLogsDialog = false }) {
                    Text(if (isBangla) "বন্ধ করুন" else "Close", color = IlamGold)
                }
            },
            containerColor = IlamDarkSurface
        )
    }
}
