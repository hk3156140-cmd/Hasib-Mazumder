package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.PermissionKeys
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.ui.MainViewModel
import com.example.ui.theme.IlamBlack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    userProfile: UserProfile?,
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToPos: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentLang = LocalAppLanguage.current
    val isBangla = currentLang == AppLanguage.BANGLA
    val scope = rememberCoroutineScope()

    val products by mainViewModel.products.collectAsState()
    var scannedCodeInput by remember { mutableStateOf("") }
    var foundProduct by remember { mutableStateOf<Product?>(null) }
    var searchStatus by remember { mutableStateOf<String?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun lookupCode(code: String) {
        val clean = code.trim().removePrefix("ILAM:")
        val match = products.find { prod ->
            !prod.isArchived && (
                prod.sku.equals(clean, ignoreCase = true) ||
                prod.barcode.equals(clean, ignoreCase = true) ||
                prod.id.equals(clean, ignoreCase = true) ||
                prod.qrCode.contains(clean, ignoreCase = true)
            )
        }
        if (match != null) {
            foundProduct = match
            searchStatus = null
        } else {
            foundProduct = null
            searchStatus = if (isBangla) "কোন পণ্য খুঁজে পাওয়া যায়নি" else "No matching product found"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBangla) "QR ও বারকোড স্ক্যানার" else "QR & Barcode Scanner",
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scanner Viewport Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(IlamDarkSurface),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                                } catch (ignored: Exception) {}
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (isBangla) "ক্যামেরা পারমিশন দিন" else "Grant Camera Permission", color = Color.LightGray, fontSize = 13.sp)
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(if (isBangla) "অনুমতি দিন" else "Allow Camera")
                        }
                    }
                }

                // Targeting Reticle Overlay
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Code Search / Manual Input Bar
            OutlinedTextField(
                value = scannedCodeInput,
                onValueChange = {
                    scannedCodeInput = it
                    if (it.length >= 4) lookupCode(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scanner_code_input"),
                placeholder = {
                    Text(
                        if (isBangla) "বারকোড বা SKU কোড লিখুন..." else "Enter or scan Barcode / SKU...",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Barcode", tint = IlamGold)
                },
                trailingIcon = {
                    IconButton(onClick = { lookupCode(scannedCodeInput) }) {
                        Icon(Icons.Default.Search, contentDescription = "Lookup", tint = IlamGoldLight)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Result Display Card
            if (foundProduct != null) {
                val prod = foundProduct!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = IlamDarkCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, fontWeight = FontWeight.Bold, color = IlamWhite, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("SKU: ${prod.sku} | Barcode: ${prod.barcode}", color = Color.Gray, fontSize = 12.sp)
                                Text("${prod.category} • ${prod.subcategory}", color = Color.LightGray, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("৳${prod.sellingPrice.toInt()}", fontWeight = FontWeight.Bold, color = IlamGoldLight, fontSize = 18.sp)
                                if (prod.discount > 0) {
                                    Text("-৳${prod.discount.toInt()} off", color = IlamSuccess, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${if (isBangla) "মজুদ স্টক: " else "Current Stock: "}${prod.currentStock}",
                                color = if (prod.isLowStock) IlamWarning else IlamSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (prod.sizes.isNotEmpty()) {
                                Text("Sizes: ${prod.sizes.joinToString(", ")}", color = Color.LightGray, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Direct "Start POS Sale" Button (Section 11 requirement)
                        Button(
                            onClick = {
                                mainViewModel.addToCart(prod)
                                onNavigateToPos()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_pos_sale_from_scan_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = IlamGold, contentColor = IlamBlack),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PointOfSale, contentDescription = "POS")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "এই পণ্য দিয়ে পিওএস বিক্রয় শুরু করুন" else "Start POS Sale with This Product",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else if (searchStatus != null) {
                Text(searchStatus ?: "", color = IlamWarning, fontSize = 13.sp)
            }
        }
    }
}
