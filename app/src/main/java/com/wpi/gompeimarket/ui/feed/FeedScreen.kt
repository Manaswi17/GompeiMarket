package com.wpi.gompeimarket.ui.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.wpi.gompeimarket.R
import com.wpi.gompeimarket.data.model.Listing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onListingClick: (Listing) -> Unit,
    onPostClick: () -> Unit,
    viewModel: ListingViewModel = hiltViewModel()
) {
    val feedState by viewModel.feedState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val filteredListings = remember(feedState.listings, searchQuery) {
        if (searchQuery.isEmpty()) feedState.listings
        else feedState.listings.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            if (isSearching) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery, onValueChange = { searchQuery = it },
                            placeholder = { Text("Search...", color = Color.White.copy(alpha = 0.7f)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color.White, focusedTextColor = Color.White, unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { isSearching = false; searchQuery = "" }) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFA6192E))
                )
            } else {
                TopAppBar(
                    title = {
                        // Gompei goat logo + "WPI" text together
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "WPI",
                                modifier = Modifier.height(32.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "WPI",
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFA6192E))
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onPostClick, containerColor = Color(0xFFA6192E), contentColor = Color.White) {
                Icon(Icons.Default.Add, contentDescription = "Post item")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when {
                feedState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFFA6192E))
                filteredListings.isEmpty() -> Text(
                    if (searchQuery.isEmpty()) "No listings yet. Be the first to post!" else "No results for \"$searchQuery\"",
                    modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyVerticalGrid(
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

@Composable
fun ListingGridItem(listing: Listing, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Box {
                AsyncImage(model = listing.imageUrl, contentDescription = listing.title, modifier = Modifier.fillMaxWidth().height(140.dp), contentScale = ContentScale.Crop)
                Surface(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), color = Color(0xFFA6192E), shape = RoundedCornerShape(4.dp)) {
                    Text("$${listing.price.toInt()}", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(listing.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 18.sp)
                Spacer(Modifier.height(4.dp))
                Surface(color = Color(0xFFFDE7E9), shape = RoundedCornerShape(4.dp)) {
                    Text(listing.category, color = Color(0xFFA6192E), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                if (listing.status == "sold") {
                    Spacer(Modifier.height(4.dp))
                    Text("SOLD", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
