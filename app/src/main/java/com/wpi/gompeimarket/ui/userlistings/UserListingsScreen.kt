package com.wpi.gompeimarket.ui.userlistings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wpi.gompeimarket.data.model.Listing
import com.wpi.gompeimarket.ui.feed.ListingGridItem
import com.wpi.gompeimarket.ui.feed.ListingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListingsScreen(
    title: String,
    showSoldOnly: Boolean,
    onBack: () -> Unit,
    onListingClick: (Listing) -> Unit,
    viewModel: ListingViewModel = hiltViewModel()
) {
    val listings by viewModel.getMyListings().collectAsState(initial = emptyList())
    
    val filteredListings = remember(listings, showSoldOnly) {
        if (showSoldOnly) {
            listings.filter { it.status == "sold" }
        } else {
            listings.filter { it.status == "active" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFA6192E))
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (filteredListings.isEmpty()) {
                Text(
                    "No items found.",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredListings) { listing ->
                        ListingGridItem(listing = listing, onClick = { onListingClick(listing) })
                    }
                }
            }
        }
    }
}
