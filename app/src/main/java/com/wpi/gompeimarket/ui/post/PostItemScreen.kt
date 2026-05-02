package com.wpi.gompeimarket.ui.post

import android.Manifest
import android.content.ContentValues
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.wpi.gompeimarket.data.model.Listing
import com.wpi.gompeimarket.ui.feed.ListingViewModel
import com.wpi.gompeimarket.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostItemScreen(
    onBack: () -> Unit,
    onPosted: () -> Unit,
    listingViewModel: ListingViewModel = hiltViewModel(),
    cameraViewModel: CameraViewModel = hiltViewModel()
) {
    val aiState by cameraViewModel.aiState.collectAsState()
    val feedState by listingViewModel.feedState.collectAsState()
    val postResult by listingViewModel.postResult.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Constants.CATEGORIES.last()) }
    var price by remember { mutableStateOf("") }
    var selectedZone by remember { mutableStateOf(Constants.MEETUP_ZONES.first().name) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(aiState) {
        if (!aiState.isAnalyzing) {
            if (aiState.description.isNotBlank()) description = aiState.description
            if (aiState.category.isNotBlank() && Constants.CATEGORIES.contains(aiState.category)) {
                category = aiState.category
                if (title.isBlank()) title = aiState.category
            }
            aiState.errorMessage?.let { Toast.makeText(context, "AI: $it", Toast.LENGTH_LONG).show() }
        }
    }

    suspend fun processImage(uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                // inSampleSize=1 for better quality for AI analysis
                val bitmap = BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options())
                inputStream?.close()
                if (bitmap != null) {
                    cameraViewModel.analyzeImage(bitmap)
                }
                Unit
            } catch (e: Exception) {
                android.util.Log.e("PostItemScreen", "processImage failed: ${e.message}", e)
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { selectedImageUri = uri; scope.launch { processImage(uri) } }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraImageUri?.let { uri -> selectedImageUri = uri; scope.launch { processImage(uri) } }
        }
    }

    fun launchCamera() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "gompei_${System.currentTimeMillis()}")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) { cameraImageUri = uri; cameraLauncher.launch(uri) }
        else Toast.makeText(context, "Cannot create image file", Toast.LENGTH_SHORT).show()
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(postResult) {
        if (postResult != null) { listingViewModel.clearPostResult(); onPosted() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post an Item", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color.White)
                .verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image box
            Box(
                modifier = Modifier.fillMaxWidth().height(250.dp)
                    .clip(RoundedCornerShape(16.dp)).background(Color(0xFFF5F5F5))
                    .clickable { showImageSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = CircleShape, color = Color(0xFFA6192E).copy(alpha = 0.1f), modifier = Modifier.size(80.dp)) {
                            Icon(Icons.Default.AddAPhoto, null, tint = Color(0xFFA6192E), modifier = Modifier.padding(20.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Tap to Add Photo", color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text("AI will auto-fill description", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                if (aiState.isAnalyzing) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(10.dp))
                            Text("AI Analyzing...", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            var catExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = category, onValueChange = {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    Constants.CATEGORIES.forEach { cat -> DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; catExpanded = false }) }
                }
            }

            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = price, onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Price ($)") }, prefix = { Text("$ ") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            var zoneExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = zoneExpanded, onExpandedChange = { zoneExpanded = !zoneExpanded }, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = selectedZone, onValueChange = {}, readOnly = true, label = { Text("Safe Meetup Zone") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(zoneExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded = zoneExpanded, onDismissRequest = { zoneExpanded = false }) {
                    Constants.MEETUP_ZONES.forEach { zone -> DropdownMenuItem(text = { Text(zone.name) }, onClick = { selectedZone = zone.name; zoneExpanded = false }) }
                }
            }

            Button(
                onClick = {
                    val uri = selectedImageUri ?: return@Button
                    val user = FirebaseAuth.getInstance().currentUser ?: return@Button
                    listingViewModel.postListing(
                        Listing(
                            title = title,
                            description = description,
                            category = category,
                            price = price.toDoubleOrNull() ?: 0.0,
                            sellerId = user.uid,
                            sellerEmail = user.email ?: "",
                            meetupZone = selectedZone,
                            relatedSearches = aiState.relatedSearches
                        ),
                        uri
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA6192E)),
                enabled = selectedImageUri != null && !aiState.isAnalyzing && !feedState.isLoading && title.isNotBlank() && price.isNotBlank()
            ) {
                if (feedState.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Post Listing", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Add Photo", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Camera option
                    Surface(
                        onClick = { showImageSourceDialog = false; cameraPermLauncher.launch(Manifest.permission.CAMERA) },
                        color = Color(0xFFF8F8F8), shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color(0xFFA6192E), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("Take Photo", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Use camera to capture item", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    // Gallery option
                    Surface(
                        onClick = { showImageSourceDialog = false; imagePickerLauncher.launch("image/*") },
                        color = Color(0xFFF8F8F8), shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color(0xFF1565C0), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("Choose from Gallery", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Pick existing photo", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}
