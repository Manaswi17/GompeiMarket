package com.wpi.gompeimarket.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.wpi.gompeimarket.R
import com.wpi.gompeimarket.ui.auth.AuthViewModel
import com.wpi.gompeimarket.ui.inbox.InboxViewModel
import androidx.compose.foundation.BorderStroke as BoxBorder

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onMyListingsClick: () -> Unit,
    onSoldItemsClick: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    inboxViewModel: InboxViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val user = FirebaseAuth.getInstance().currentUser
    val unreadCount by inboxViewModel.totalUnreadCount.collectAsState()
    val profileState by profileViewModel.state.collectAsState()
    var showEditName by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        inboxViewModel.loadConversations()
        profileViewModel.loadProfile()
    }

    val displayName = when {
        profileState.firstName.isNotBlank() && profileState.lastName.isNotBlank() ->
            "${profileState.firstName} ${profileState.lastName}"
        profileState.firstName.isNotBlank() -> profileState.firstName
        else -> user?.email?.substringBefore("@") ?: "WPI Student"
    }
    val initials = when {
        profileState.firstName.isNotBlank() && profileState.lastName.isNotBlank() ->
            "${profileState.firstName.first().uppercaseChar()}${profileState.lastName.first().uppercaseChar()}"
        profileState.firstName.isNotBlank() -> profileState.firstName.take(2).uppercase()
        else -> user?.email?.substringBefore("@")?.take(2)?.uppercase() ?: "GM"
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(Color(0xFFA6192E)))

        Column(modifier = Modifier.fillMaxWidth().offset(y = (-60).dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(120.dp), shape = CircleShape, border = BoxBorder(3.dp, Color.White), color = Color.Transparent) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFFD32F2F), Color(0xFF7B0000))), CircleShape), contentAlignment = Alignment.Center) {
                    Text(initials, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(displayName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(user?.email ?: "", fontSize = 13.sp, color = Color.Gray)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedButton(onClick = { showEditName = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA6192E)), border = BoxBorder(1.dp, Color(0xFFA6192E)), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Edit Name", fontSize = 12.sp)
                }
                OutlinedButton(onClick = { showChangePassword = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray), border = BoxBorder(1.dp, Color.LightGray), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Password", fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AmiBadge(Modifier.weight(1f), R.drawable.badge_listed, profileState.totalListed.toString(), "Listed", Color(0xFFE3F2FD))
                AmiBadge(Modifier.weight(1f), R.drawable.badge_sold, profileState.totalSold.toString(), "Sold", Color(0xFFE8F5E9))
                AmiBadge(Modifier.weight(1f), R.drawable.badge_earned, "$${profileState.totalEarned.toInt()}", "Earned", Color(0xFFFFF8E1))
                AmiBadge(Modifier.weight(1f), R.drawable.badge_messages, if (unreadCount > 0) unreadCount.toString() else "—", "Unread", Color(0xFFFCE4EC), unreadCount > 0)
            }

            Spacer(Modifier.height(24.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                ProfileMenuRow(Icons.Default.List,        "My Listings",  onClick = onMyListingsClick)
                ProfileMenuRow(Icons.Default.CheckCircle, "Sold Items",   onClick = onSoldItemsClick)
            }

            Spacer(Modifier.height(32.dp))
            Button(onClick = { authViewModel.signOut(); onSignOut() }, modifier = Modifier.fillMaxWidth(0.85f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA6192E)), shape = RoundedCornerShape(28.dp)) {
                Text("Log Out", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(48.dp))
        }
    }

    // Edit Name Dialog
    if (showEditName) {
        var fn by remember { mutableStateOf(profileState.firstName) }
        var ln by remember { mutableStateOf(profileState.lastName) }
        var saving by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showEditName = false },
            title = { Text("Edit Name") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(fn, { fn = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
                    OutlinedTextField(ln, { ln = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        saving = true
                        profileViewModel.saveName(fn.trim(), ln.trim()) { showEditName = false; saving = false }
                    },
                    enabled = !saving && fn.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA6192E))
                ) { Text(if (saving) "Saving..." else "Save") }
            },
            dismissButton = { TextButton(onClick = { showEditName = false }) { Text("Cancel") } }
        )
    }

    // Change Password Dialog
    if (showChangePassword) {
        var currentPw by remember { mutableStateOf("") }
        var newPw by remember { mutableStateOf("") }
        var confirmPw by remember { mutableStateOf("") }
        var pwError by remember { mutableStateOf<String?>(null) }
        var saving by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showChangePassword = false },
            title = { Text("Change Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(currentPw, { currentPw = it }, label = { Text("Current Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
                    OutlinedTextField(newPw, { newPw = it }, label = { Text("New Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
                    OutlinedTextField(confirmPw, { confirmPw = it }, label = { Text("Confirm Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
                    if (pwError != null) Text(pwError!!, color = Color.Red, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPw != confirmPw) { pwError = "Passwords don't match"; return@Button }
                        if (newPw.length < 6) { pwError = "Minimum 6 characters"; return@Button }
                        saving = true; pwError = null
                        profileViewModel.changePassword(currentPw, newPw) { error ->
                            if (error == null) showChangePassword = false else pwError = error
                            saving = false
                        }
                    },
                    enabled = !saving && currentPw.isNotBlank() && newPw.isNotBlank() && confirmPw.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA6192E))
                ) { Text(if (saving) "Updating..." else "Update") }
            },
            dismissButton = { TextButton(onClick = { showChangePassword = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AmiBadge(modifier: Modifier = Modifier, icon: Int, value: String, label: String, bgColor: Color, highlight: Boolean = false) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = bgColor), elevation = CardDefaults.cardElevation(if (highlight) 4.dp else 1.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(painter = painterResource(id = icon), contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.Unspecified)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (highlight) Color(0xFFA6192E) else Color.Black)
            Text(label, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ProfileMenuRow(icon: ImageVector? = null, title: String, badge: Int = 0, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = onClick, color = Color.White) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) { Icon(icon, null, tint = Color(0xFFA6192E), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)) }
                    Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    if (badge > 0) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = CircleShape, color = Color(0xFFA6192E)) {
                            Text(if (badge > 9) "9+" else badge.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
            }
            HorizontalDivider(color = Color(0xFFEEEEEE))
        }
    }
}
