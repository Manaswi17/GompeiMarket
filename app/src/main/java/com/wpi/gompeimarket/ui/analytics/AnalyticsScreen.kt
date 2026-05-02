package com.wpi.gompeimarket.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val analytics by viewModel.analytics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    
    val data = analytics ?: AnalyticsData()
    val userName = if (data.firstName.isNotBlank()) data.firstName 
                   else FirebaseAuth.getInstance().currentUser?.email?.substringBefore("@") ?: "Student"
    val wpiRed = Color(0xFFA6192E)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Analytics", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = wpiRed)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = wpiRed)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .background(Color(0xFFF4F4F4))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Greeting
            Text("Hey $userName!", fontSize = 22.sp, fontWeight = FontWeight.Bold)

            // Date filter
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = dateFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = wpiRed, selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), "Listed",  data.totalListed.toString(), Color(0xFF1565C0))
                StatCard(Modifier.weight(1f), "Active",  data.totalActive.toString(), Color(0xFF2E7D32))
                StatCard(Modifier.weight(1f), "Sold",    data.totalSold.toString(),   wpiRed)
            }

            // Earnings
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = wpiRed)) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Total Earned", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        Text("$${"%.2f".format(data.totalEarned)}", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                        if (data.totalSold > 0)
                            Text("$${"%,.0f".format(data.totalEarned / data.totalSold)} avg per sale", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        else
                            Text("No sales in this period", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    Icon(Icons.Default.TrendingUp, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                }
            }

            // Sales trend chart
            if (data.dailyTrend.any { it.revenue > 0 }) {
                SectionCard("Revenue by Day") {
                    BarChart(
                        bars = data.dailyTrend.map { it.label to it.revenue.toFloat() },
                        barColor = wpiRed, modifier = Modifier.fillMaxWidth().height(150.dp)
                    )
                }
            }

            // Items sold per day
            if (data.dailyTrend.any { it.itemsSold > 0 }) {
                SectionCard("Items Sold by Day") {
                    BarChart(
                        bars = data.dailyTrend.map { it.label to it.itemsSold.toFloat() },
                        barColor = Color(0xFF1565C0), modifier = Modifier.fillMaxWidth().height(150.dp)
                    )
                }
            }

            // Top performer - simple card, no over-the-top text
            if (data.bestCategory.isNotBlank() || data.bestItem != null) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFF57F17), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Top Performers", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                        if (data.bestCategory.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Best Category", color = Color.Gray, fontSize = 13.sp)
                                Text(data.bestCategory, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = wpiRed)
                            }
                        }
                        data.bestItem?.let {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Highest Sale", color = Color.Gray, fontSize = 13.sp)
                                Text("${it.title}  ·  $${it.price.toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Category breakdown
            if (data.categoryBreakdown.isNotEmpty()) {
                SectionCard("Sales by Category") {
                    val maxCount = data.categoryBreakdown.values.maxOrNull() ?: 1
                    data.categoryBreakdown.entries.sortedByDescending { it.value }.forEach { (cat, count) ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(cat, fontSize = 13.sp, color = Color.DarkGray)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("$count sold", fontSize = 12.sp, color = wpiRed, fontWeight = FontWeight.SemiBold)
                                    data.categoryRevenue[cat]?.let { rev ->
                                        Text("· $${rev.toInt()}", fontSize = 12.sp, color = Color(0xFF2E7D32))
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(7.dp).background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp))) {
                                Box(modifier = Modifier.fillMaxWidth(count.toFloat() / maxCount).height(7.dp).background(wpiRed, RoundedCornerShape(4.dp)))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            // Unsold items — plain, no dramatic language
            if (data.unsoldItems.isNotEmpty()) {
                SectionCard("Still Active — ${data.unsoldItems.size} item${if (data.unsoldItems.size == 1) "" else "s"}") {
                    data.unsoldItems.take(5).forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text(
                                    when {
                                        item.daysListed >= 14 -> "${item.daysListed}d listed — consider a price drop"
                                        item.daysListed >= 7  -> "${item.daysListed}d listed"
                                        else                  -> "${item.daysListed}d listed"
                                    },
                                    fontSize = 11.sp,
                                    color = when {
                                        item.daysListed >= 14 -> Color(0xFFB71C1C)
                                        item.daysListed >= 7  -> Color(0xFFE65100)
                                        else                  -> Color.Gray
                                    }
                                )
                            }
                            Text(item.category, fontSize = 11.sp, color = Color.Gray)
                        }
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                    }
                }
            }

            // Empty
            if (data.totalListed == 0) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📦", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No listings yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Post something to start tracking your activity.", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.DarkGray)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun BarChart(bars: List<Pair<String, Float>>, barColor: Color, modifier: Modifier = Modifier) {
    if (bars.isEmpty()) return
    val maxVal = bars.maxOfOrNull { it.second }?.takeIf { it > 0f } ?: 1f
    val labelColorArgb = Color.Gray.toArgb()
    val barColorArgb = barColor.toArgb()
    Canvas(modifier = modifier.padding(vertical = 8.dp)) {
        val w = size.width; val h = size.height
        val count = bars.size
        val gap = 6f
        val barW = ((w - gap * (count - 1)) / count).coerceAtLeast(6f)
        val textSz = 22f
        bars.forEachIndexed { i, (label, value) ->
            val x = i * (barW + gap)
            val barH = (value / maxVal) * (h - textSz - 8f)
            val y = h - textSz - barH - 4f
            drawRoundRect(
                color = if (value > 0f) barColor else Color(0xFFEEEEEE),
                topLeft = Offset(x, y), size = Size(barW, barH.coerceAtLeast(4f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
            )
            drawContext.canvas.nativeCanvas.drawText(
                label, x + barW / 2f, h,
                android.graphics.Paint().apply { color = labelColorArgb; textSize = textSz; textAlign = android.graphics.Paint.Align.CENTER }
            )
            if (value > 0f) {
                drawContext.canvas.nativeCanvas.drawText(
                    if (value >= 100) "$${value.toInt()}" else "${value.toInt()}",
                    x + barW / 2f, (y - 4f).coerceAtLeast(16f),
                    android.graphics.Paint().apply { color = barColorArgb; textSize = 20f; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
                )
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, label: String, value: String, color: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
