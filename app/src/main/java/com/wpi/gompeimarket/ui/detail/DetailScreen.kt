package com.wpi.gompeimarket.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.wpi.gompeimarket.data.model.Listing
import com.wpi.gompeimarket.ui.feed.ListingViewModel
import com.wpi.gompeimarket.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    listing: Listing,
    onBack: () -> Unit,
    onChat: ((Listing) -> Unit)? = null,
    onEdit: ((Listing) -> Unit)? = null,
    viewModel: ListingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val meetupZone = Constants.MEETUP_ZONES.find {
        it.name.trim().equals(listing.meetupZone.trim(), ignoreCase = true)
    } ?: Constants.MEETUP_ZONES.first()

    val meetupLatLng = LatLng(meetupZone.lat, meetupZone.lng)
    val isOwner = viewModel.currentUserId == listing.sellerId

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(meetupLatLng, 17f)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(meetupLatLng) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(meetupLatLng, 17f)
    }

    fun openGoogleMapsDirections() {
        val uri = Uri.parse("google.navigation:q=${meetupZone.lat},${meetupZone.lng}&mode=w")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        val fallbackUri = Uri.parse("https://maps.google.com/maps?daddr=${meetupZone.lat},${meetupZone.lng}")
        val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)

        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            context.startActivity(fallbackIntent)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                AsyncImage(
                    model = listing.imageUrl,
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (listing.status == "sold") {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        Surface(color = Color.Red, shape = RoundedCornerShape(8.dp)) {
                            Text("SOLD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 40.sp, modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp))
                        }
                    }
                }
                Surface(
                    modifier = Modifier.padding(16.dp).size(44.dp),
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = CircleShape,
                    onClick = onBack
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.padding(10.dp))
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(listing.title, fontSize = 28.sp, fontWeight = FontWeight.Bold)

                Column {
                    Text("Description", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(listing.description, fontSize = 16.sp, color = Color.DarkGray, lineHeight = 22.sp)
                }

                if (listing.relatedSearches.isNotEmpty()) {
                    Column {
                        Text("Related Keywords", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listing.relatedSearches.forEach { keyword ->
                                SuggestionChip(
                                    onClick = { /* Could navigate to feed with search */ },
                                    label = { Text(keyword, fontSize = 12.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = Color(0xFFF5F5F5),
                                        labelColor = Color(0xFF333333)
                                    )
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Price: ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("$${listing.price.toInt()}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFA6192E))
                }

                Column {
                    Text("Seller", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(if (listing.sellerName.isNotBlank()) listing.sellerName else listing.sellerEmail.substringBefore("@"), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                Column {
                    Text("Category", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFA6192E).copy(alpha = 0.1f)) {
                        Text(listing.category, fontSize = 14.sp, color = Color(0xFFA6192E), fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Safe Meetup Zone", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    OutlinedButton(
                        onClick = { openGoogleMapsDirections() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA6192E)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA6192E)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Directions, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Directions", fontSize = 13.sp)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = false),
                        properties = MapProperties(isMyLocationEnabled = false),
                        onMapClick = { openGoogleMapsDirections() }
                    ) {
                        Marker(
                            state = MarkerState(position = meetupLatLng),
                            title = meetupZone.name,
                            snippet = "Tap for directions",
                            onClick = { openGoogleMapsDirections(); false }
                        )
                    }
                }

                Text(meetupZone.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterHorizontally), color = Color.DarkGray)

                if (isOwner) {
                    if (listing.status == "active") {
                        OutlinedButton(
                            onClick = { onEdit?.invoke(listing) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA6192E))
                        ) {
                            Icon(Icons.Default.Edit, null); Spacer(Modifier.width(8.dp))
                            Text("Edit Listing", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.markAsSold(listing.id); onBack() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA6192E)),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Icon(Icons.Default.Check, null); Spacer(Modifier.width(8.dp))
                            Text("Mark as Sold", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA6192E)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA6192E))
                        ) {
                            Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp))
                            Text("Delete Listing", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    if (listing.status == "active") {
                        Button(
                            onClick = { onChat?.invoke(listing) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA6192E)),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Icon(Icons.Default.Chat, null); Spacer(Modifier.width(8.dp))
                            Text("Chat with Seller", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Listing?") },
            text = { Text("Are you sure you want to remove this item? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteListing(listing)
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
