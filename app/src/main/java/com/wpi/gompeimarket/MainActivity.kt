package com.wpi.gompeimarket

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.wpi.gompeimarket.ui.analytics.AnalyticsScreen
import com.wpi.gompeimarket.ui.auth.LoginScreen
import com.wpi.gompeimarket.ui.chat.ChatScreen
import com.wpi.gompeimarket.ui.chat.ChatScreenFromInbox
import com.wpi.gompeimarket.ui.detail.DetailScreen
import com.wpi.gompeimarket.ui.edit.EditListingScreen
import com.wpi.gompeimarket.ui.feed.FeedScreen
import com.wpi.gompeimarket.ui.inbox.InboxScreen
import com.wpi.gompeimarket.ui.post.PostItemScreen
import com.wpi.gompeimarket.ui.profile.ProfileScreen
import com.wpi.gompeimarket.ui.userlistings.UserListingsScreen
import dagger.hilt.android.AndroidEntryPoint

object Routes {
    const val LOGIN        = "login"
    const val FEED         = "feed"
    const val DETAIL       = "detail"
    const val POST         = "post"
    const val PROFILE      = "profile"
    const val MY_LISTINGS  = "my_listings"
    const val SOLD_ITEMS   = "sold_items"
    const val CHAT         = "chat"
    const val CHAT_INBOX   = "chat_inbox"
    const val EDIT         = "edit"
    const val INBOX        = "inbox"
    const val ANALYTICS    = "analytics"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {}

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val bottomBarRoutes = listOf(Routes.FEED, Routes.INBOX, Routes.ANALYTICS, Routes.PROFILE)
                val showBottomBar = currentDestination?.route in bottomBarRoutes

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(containerColor = Color.White) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, null) },
                                    label = { Text("Home") },
                                    selected = currentDestination?.hierarchy?.any { it.route == Routes.FEED } == true,
                                    onClick = {
                                        navController.navigate(Routes.FEED) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true; restoreState = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFFA6192E), selectedTextColor = Color(0xFFA6192E),
                                        indicatorColor = Color(0xFFFDE7E9)
                                    )
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, null) },
                                    label = { Text("Messages") },
                                    selected = currentDestination?.hierarchy?.any { it.route == Routes.INBOX } == true,
                                    onClick = {
                                        navController.navigate(Routes.INBOX) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true; restoreState = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFFA6192E), selectedTextColor = Color(0xFFA6192E),
                                        indicatorColor = Color(0xFFFDE7E9)
                                    )
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.BarChart, null) },
                                    label = { Text("Analytics") },
                                    selected = currentDestination?.hierarchy?.any { it.route == Routes.ANALYTICS } == true,
                                    onClick = {
                                        navController.navigate(Routes.ANALYTICS) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true; restoreState = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFFA6192E), selectedTextColor = Color(0xFFA6192E),
                                        indicatorColor = Color(0xFFFDE7E9)
                                    )
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.AccountCircle, null) },
                                    label = { Text("Profile") },
                                    selected = currentDestination?.hierarchy?.any { it.route == Routes.PROFILE } == true,
                                    onClick = {
                                        navController.navigate(Routes.PROFILE) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true; restoreState = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFFA6192E), selectedTextColor = Color(0xFFA6192E),
                                        indicatorColor = Color(0xFFFDE7E9)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(navController = navController, startDestination = Routes.LOGIN,
                        modifier = Modifier.padding(innerPadding)) {

                        composable(Routes.LOGIN) {
                            LoginScreen(onLoginSuccess = {
                                navController.navigate(Routes.FEED) { popUpTo(Routes.LOGIN) { inclusive = true } }
                            })
                        }

                        composable(Routes.FEED) {
                            FeedScreen(
                                onListingClick = { listing ->
                                    navController.currentBackStackEntry?.savedStateHandle?.set("listing", listing)
                                    navController.navigate(Routes.DETAIL)
                                },
                                onPostClick = { navController.navigate(Routes.POST) }
                            )
                        }

                        composable(Routes.POST) {
                            PostItemScreen(onBack = { navController.popBackStack() }, onPosted = { navController.popBackStack() })
                        }

                        composable(Routes.DETAIL) { backStackEntry ->
                            val listing = remember(backStackEntry) {
                                navController.previousBackStackEntry?.savedStateHandle?.get<com.wpi.gompeimarket.data.model.Listing>("listing")
                            }
                            if (listing != null) {
                                DetailScreen(
                                    listing = listing,
                                    onBack = { navController.popBackStack() },
                                    onChat = { l ->
                                        navController.currentBackStackEntry?.savedStateHandle?.set("listing", l)
                                        navController.navigate(Routes.CHAT)
                                    },
                                    onEdit = { l ->
                                        navController.currentBackStackEntry?.savedStateHandle?.set("listing", l)
                                        navController.navigate(Routes.EDIT)
                                    }
                                )
                            }
                        }

                        composable(Routes.CHAT) { backStackEntry ->
                            val listing = remember(backStackEntry) {
                                navController.previousBackStackEntry?.savedStateHandle?.get<com.wpi.gompeimarket.data.model.Listing>("listing")
                            }
                            if (listing != null) {
                                ChatScreen(listing = listing, onBack = {
                                    navController.navigate(Routes.INBOX) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                })
                            }
                        }

                        composable(Routes.INBOX) {
                            InboxScreen(
                                onChatClick = { roomId, otherEmail, otherName, listingTitle, listingId, sellerId, otherUserId ->
                                    navController.currentBackStackEntry?.savedStateHandle?.let { h ->
                                        h.set("roomId", roomId); h.set("otherEmail", otherEmail)
                                        h.set("otherName", otherName)
                                        h.set("listingTitle", listingTitle); h.set("listingId", listingId)
                                        h.set("sellerId", sellerId); h.set("otherUserId", otherUserId)
                                    }
                                    navController.navigate(Routes.CHAT_INBOX)
                                }
                            )
                        }

                        composable(Routes.CHAT_INBOX) { backStackEntry ->
                            val h = remember(backStackEntry) { navController.previousBackStackEntry?.savedStateHandle }
                            val roomId = h?.get<String>("roomId") ?: ""
                            if (roomId.isNotEmpty()) {
                                ChatScreenFromInbox(
                                    roomId = roomId,
                                    otherUserEmail = h?.get<String>("otherEmail") ?: "",
                                    otherUserName = h?.get<String>("otherName") ?: "",
                                    listingTitle = h?.get<String>("listingTitle") ?: "",
                                    listingId = h?.get<String>("listingId") ?: "",
                                    sellerId = h?.get<String>("sellerId") ?: "",
                                    otherUserId = h?.get<String>("otherUserId") ?: "",
                                    onBack = {
                                        navController.navigate(Routes.INBOX) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }

                        composable(Routes.EDIT) { backStackEntry ->
                            val listing = remember(backStackEntry) {
                                navController.previousBackStackEntry?.savedStateHandle?.get<com.wpi.gompeimarket.data.model.Listing>("listing")
                            }
                            if (listing != null) {
                                EditListingScreen(listing = listing, onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
                            }
                        }

                        composable(Routes.ANALYTICS) {
                            AnalyticsScreen()
                        }

                        composable(Routes.PROFILE) {
                            ProfileScreen(
                                onSignOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } } },
                                onMyListingsClick = { navController.navigate(Routes.MY_LISTINGS) },
                                onSoldItemsClick = { navController.navigate(Routes.SOLD_ITEMS) }
                            )
                        }

                        composable(Routes.MY_LISTINGS) {
                            UserListingsScreen(
                                title = "My Listings", showSoldOnly = false,
                                onBack = { navController.popBackStack() },
                                onListingClick = { listing ->
                                    navController.currentBackStackEntry?.savedStateHandle?.set("listing", listing)
                                    navController.navigate(Routes.DETAIL)
                                }
                            )
                        }

                        composable(Routes.SOLD_ITEMS) {
                            UserListingsScreen(
                                title = "Sold Items", showSoldOnly = true,
                                onBack = { navController.popBackStack() },
                                onListingClick = { listing ->
                                    navController.currentBackStackEntry?.savedStateHandle?.set("listing", listing)
                                    navController.navigate(Routes.DETAIL)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
