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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.model.AuditLog
import com.example.data.model.PermissionKeys
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
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
fun StaffPermissionsScreen(
    userProfile: UserProfile?,
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val currentLang = LocalAppLanguage.current
    val isBangla = currentLang == AppLanguage.BANGLA
    val isOwner = userProfile?.role == UserRole.OWNER.name ||
            userProfile?.email.equals("hk3156140@gmail.com", ignoreCase = true)

    val staffList by mainViewModel.staffUsers.collectAsState()
    val auditLogs by mainViewModel.auditLogs.collectAsState()

    var selectedEmployee by remember { mutableStateOf<UserProfile?>(null) }
    var showCreateStaffModal by remember { mutableStateOf(false) }
    var showAuditLogsModal by remember { mutableStateOf(false) }

    // When an employee is selected, hold their editable permissions in a map
    val editablePermissions = remember { mutableStateMapOf<String, Boolean>() }
    var employeeRoleState by remember { mutableStateOf(UserRole.STAFF) }

    fun selectEmployeeForEdit(emp: UserProfile) {
        selectedEmployee = emp
        employeeRoleState = try {
            UserRole.valueOf(emp.role)
        } catch (e: Exception) {
            UserRole.STAFF
        }
        editablePermissions.clear()
        PermissionKeys.ALL.forEach { key ->
            editablePermissions[key] = emp.permissions[key] ?: false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBangla) "কর্মী ও কাস্টম পারমিশন" else "Staff & Custom Permissions",
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
                actions = {
                    IconButton(onClick = { showAuditLogsModal = true }) {
                        Icon(Icons.Default.History, contentDescription = "Audit Logs", tint = IlamGoldLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IlamDarkSurface)
            )
        },
        floatingActionButton = {
            if (isOwner && selectedEmployee == null) {
                FloatingActionButton(
                    onClick = { showCreateStaffModal = true },
                    containerColor = IlamGold,
                    contentColor = IlamBlack,
                    modifier = Modifier.testTag("add_staff_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Employee")
                }
            }
        },
        containerColor = IlamBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(14.dp)
        ) {
            if (!isOwner) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isBangla) "শুধুমাত্র মালিক এই সেকশনে প্রবেশ করতে পারবেন।" else "Only the Owner has access to Staff Permissions.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                return@Scaffold
            }

            if (selectedEmployee == null) {
                // --- LIST OF EMPLOYEES ---
                Text(
                    text = if (isBangla) "কর্মচারী নির্বাচন করে পারমিশন কনফিগার করুন:" else "Select Employee to Configure Permissions:",
                    color = IlamGoldLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(staffList) { emp ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectEmployeeForEdit(emp) }
                                .testTag("staff_item_${emp.userId}"),
                            colors = CardDefaults.cardColors(containerColor = IlamDarkSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(IlamDarkCard),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Badge, contentDescription = "Staff", tint = IlamGold)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(emp.name, fontWeight = FontWeight.Bold, color = IlamWhite, fontSize = 15.sp)
                                        Text("${emp.email.ifBlank { emp.phone }}", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (emp.role == "OWNER") IlamGold.copy(alpha = 0.2f) else Color(0xFF2563EB).copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(emp.role, color = if (emp.role == "OWNER") IlamGold else Color(0xFF60A5FA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val activeCount = emp.permissions.count { it.value }
                                    Text("$activeCount Perms", color = Color.LightGray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // --- INDIVIDUAL EMPLOYEE PERMISSION EDITOR (Section 5) ---
                val emp = selectedEmployee!!

                Card(
                    colors = CardDefaults.cardColors(containerColor = IlamDarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(emp.name, fontWeight = FontWeight.Bold, color = IlamWhite, fontSize = 16.sp)
                                Text(emp.email.ifBlank { emp.phone }, color = Color.Gray, fontSize = 12.sp)
                            }

                            TextButton(onClick = { selectedEmployee = null }) {
                                Text(if (isBangla) "তালিকায় ফিরুন" else "Back to List", color = IlamGoldLight)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Role Selector
                        Text(if (isBangla) "ভূমিকা (Role):" else "Role Assignment:", color = Color.LightGray, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = employeeRoleState == UserRole.STAFF,
                                onClick = { employeeRoleState = UserRole.STAFF },
                                colors = RadioButtonDefaults.colors(selectedColor = IlamGold)
                            )
                            Text("Staff", color = IlamWhite, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            RadioButton(
                                selected = employeeRoleState == UserRole.MANAGER,
                                onClick = { employeeRoleState = UserRole.MANAGER },
                                colors = RadioButtonDefaults.colors(selectedColor = IlamGold)
                            )
                            Text("Manager", color = IlamWhite, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            RadioButton(
                                selected = employeeRoleState == UserRole.OWNER,
                                onClick = { employeeRoleState = UserRole.OWNER },
                                colors = RadioButtonDefaults.colors(selectedColor = IlamGold)
                            )
                            Text("Owner", color = IlamWhite, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBangla) "২৮টি স্বতন্ত্র পারমিশন সুইচ:" else "28 Independent Permissions:",
                        fontWeight = FontWeight.Bold,
                        color = IlamGoldLight,
                        fontSize = 14.sp
                    )

                    Button(
                        onClick = {
                            mainViewModel.updateStaffPermissions(
                                employeeId = emp.userId,
                                employeeName = emp.name,
                                newRole = employeeRoleState,
                                oldPerms = emp.permissions,
                                newPerms = editablePermissions.toMap()
                            )
                            selectedEmployee = null
                        },
                        modifier = Modifier.testTag("save_permissions_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isBangla) "সেভ করুন" else "Save Changes")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // All 28 permissions list with ON/OFF switch
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    items(PermissionKeys.ALL) { permKey ->
                        val isChecked = editablePermissions[permKey] ?: false
                        Card(
                            colors = CardDefaults.cardColors(containerColor = IlamDarkSurface),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = PermissionKeys.getDisplayName(permKey, isBangla),
                                        color = IlamWhite,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = permKey,
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }

                                Switch(
                                    checked = isChecked,
                                    onCheckedChange = { editablePermissions[permKey] = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = IlamBlack,
                                        checkedTrackColor = IlamGold,
                                        uncheckedThumbColor = Color.LightGray,
                                        uncheckedTrackColor = Color.DarkGray
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- CREATE STAFF MODAL ---
    if (showCreateStaffModal) {
        var newStaffName by remember { mutableStateOf("") }
        var newStaffEmail by remember { mutableStateOf("") }
        var newStaffPhone by remember { mutableStateOf("") }
        var newRole by remember { mutableStateOf(UserRole.STAFF) }

        AlertDialog(
            onDismissRequest = { showCreateStaffModal = false },
            title = {
                Text(
                    text = if (isBangla) "নতুন কর্মচারী যোগ করুন" else "Add New Staff Member",
                    fontWeight = FontWeight.Bold,
                    color = IlamGoldLight,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newStaffName,
                        onValueChange = { newStaffName = it },
                        label = { Text(if (isBangla) "নাম *" else "Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("new_staff_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newStaffEmail,
                        onValueChange = { newStaffEmail = it },
                        label = { Text(if (isBangla) "ইমেইল" else "Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newStaffPhone,
                        onValueChange = { newStaffPhone = it },
                        label = { Text(if (isBangla) "ফোন নম্বর" else "Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = newRole == UserRole.STAFF,
                            onClick = { newRole = UserRole.STAFF },
                            colors = RadioButtonDefaults.colors(selectedColor = IlamGold)
                        )
                        Text("Staff", color = IlamWhite, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        RadioButton(
                            selected = newRole == UserRole.MANAGER,
                            onClick = { newRole = UserRole.MANAGER },
                            colors = RadioButtonDefaults.colors(selectedColor = IlamGold)
                        )
                        Text("Manager", color = IlamWhite, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newStaffName.isBlank()) return@Button
                        val initialPerms = if (newRole == UserRole.MANAGER)
                            PermissionKeys.defaultManagerPermissions()
                        else
                            PermissionKeys.defaultStaffPermissions()

                        mainViewModel.createStaffMember(
                            name = newStaffName,
                            email = newStaffEmail,
                            phone = newStaffPhone,
                            role = newRole,
                            permissions = initialPerms
                        )
                        showCreateStaffModal = false
                    },
                    modifier = Modifier.testTag("confirm_create_staff_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
                ) {
                    Text(if (isBangla) "তৈরি করুন" else "Create Staff")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateStaffModal = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel", color = Color.Gray)
                }
            },
            containerColor = IlamDarkSurface
        )
    }

    // --- AUDIT LOGS MODAL (Section 6) ---
    if (showAuditLogsModal) {
        AlertDialog(
            onDismissRequest = { showAuditLogsModal = false },
            title = {
                Text(
                    text = if (isBangla) "পারমিশন ও নিরাপত্তা অডিট লগ" else "Security & Permission Audit Trail",
                    fontWeight = FontWeight.Bold,
                    color = IlamGoldLight,
                    fontSize = 16.sp
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(auditLogs) { log ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = IlamDarkCard),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(log.action, fontWeight = FontWeight.Bold, color = IlamGold, fontSize = 12.sp)
                                    val timeStr = log.createdAt?.toDate()?.toString() ?: "Just now"
                                    Text(timeStr.take(19), color = Color.Gray, fontSize = 10.sp)
                                }
                                Text("By: ${log.actorName} (${log.actorEmail})", color = Color.LightGray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(log.details, color = IlamWhite, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAuditLogsModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack)
                ) {
                    Text(if (isBangla) "বন্ধ করুন" else "Close")
                }
            },
            containerColor = IlamDarkSurface
        )
    }
}
