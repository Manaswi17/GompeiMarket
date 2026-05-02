package com.wpi.gompeimarket.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class DailyRevenue(val label: String, val revenue: Double, val itemsSold: Int)

data class AnalyticsData(
    val firstName: String = "", // Added this
    val totalListed: Int = 0,
    val totalSold: Int = 0,
    val totalActive: Int = 0,
    val totalEarned: Double = 0.0,
    val categoryBreakdown: Map<String, Int> = emptyMap(),
    val categoryRevenue: Map<String, Double> = emptyMap(),
    val soldItems: List<SoldItem> = emptyList(),
    val dailyTrend: List<DailyRevenue> = emptyList(),
    val bestCategory: String = "",
    val bestItem: SoldItem? = null,
    val unsoldItems: List<UnsoldItem> = emptyList()
)

data class SoldItem(val title: String, val price: Double, val category: String)
data class UnsoldItem(val title: String, val daysListed: Long, val category: String)

enum class DateFilter(val label: String, val days: Int) {
    WEEK("7 Days", 7),
    MONTH("30 Days", 30),
    ALL("All Time", Int.MAX_VALUE)
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _analytics = MutableStateFlow<AnalyticsData?>(null)
    val analytics: StateFlow<AnalyticsData?> = _analytics

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _dateFilter = MutableStateFlow(DateFilter.MONTH)
    val dateFilter: StateFlow<DateFilter> = _dateFilter

    private var listener: ListenerRegistration? = null

    init { loadAnalytics() }

    fun setFilter(filter: DateFilter) {
        _dateFilter.value = filter
        loadAnalytics()
    }

    private fun loadAnalytics() {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.value = true
        listener?.remove()

        viewModelScope.launch {
            val userDoc = try {
                firestore.collection("users").document(uid).get().await()
            } catch (e: Exception) { null }
            val firstName = userDoc?.getString("firstName") ?: ""

            listener = firestore.collection("listings")
                .whereEqualTo("sellerId", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        _isLoading.value = false
                        return@addSnapshotListener
                    }

                    val filterDays = _dateFilter.value.days
                    val cutoff = if (filterDays == Int.MAX_VALUE) null else {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, -filterDays)
                        Timestamp(cal.time)
                    }

                    val allDocs = snapshot.documents
                    val filteredDocs = if (cutoff != null) {
                        allDocs.filter { (it.getTimestamp("createdAt") ?: Timestamp.now()) >= cutoff }
                    } else allDocs

                    val sold = filteredDocs.filter { it.getString("status") == "sold" }
                    val active = filteredDocs.filter { it.getString("status") == "active" }
                    val allActive = allDocs.filter { it.getString("status") == "active" } // for unsold

                    val earned = sold.sumOf { it.getDouble("price") ?: 0.0 }

                    val catBreak = sold.groupBy { it.getString("category") ?: "Others" }
                        .mapValues { (_, d) -> d.size }
                    val catRevenue = sold.groupBy { it.getString("category") ?: "Others" }
                        .mapValues { (_, d) -> d.sumOf { it.getDouble("price") ?: 0.0 } }

                    val bestCat = catBreak.maxByOrNull { it.value }?.key ?: ""
                    val soldItems = sold.map { doc ->
                        SoldItem(doc.getString("title") ?: "", doc.getDouble("price") ?: 0.0, doc.getString("category") ?: "Others")
                    }.sortedByDescending { it.price }
                    val bestItem = soldItems.firstOrNull()

                    // Unsold items with days listed
                    val now = Date()
                    val unsoldItems = allActive.map { doc ->
                        val created = doc.getTimestamp("createdAt")?.toDate() ?: now
                        val days = ((now.time - created.time) / 86400000L)
                        UnsoldItem(doc.getString("title") ?: "", days, doc.getString("category") ?: "Others")
                    }.sortedByDescending { it.daysListed }

                    // Daily trend
                    val sdf = if (filterDays <= 7) SimpleDateFormat("EEE", Locale.getDefault())
                              else SimpleDateFormat("MMM d", Locale.getDefault())
                    val trendMap = mutableMapOf<String, DailyRevenue>()

                    val calendarDays = if (filterDays == Int.MAX_VALUE) 30 else minOf(filterDays, 30)
                    for (i in calendarDays - 1 downTo 0) {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, -i)
                        val label = sdf.format(cal.time)
                        trendMap[label] = DailyRevenue(label, 0.0, 0)
                    }

                    sold.forEach { doc ->
                        val date = doc.getTimestamp("createdAt")?.toDate() ?: return@forEach
                        val label = sdf.format(date)
                        val price = doc.getDouble("price") ?: 0.0
                        val prev = trendMap[label] ?: return@forEach
                        trendMap[label] = prev.copy(revenue = prev.revenue + price, itemsSold = prev.itemsSold + 1)
                    }

                    // Show only last N days based on filter
                    val trendList = trendMap.values.toList()
                        .takeLast(if (filterDays <= 7) 7 else 14)

                    _analytics.value = AnalyticsData(
                        firstName = firstName,
                        totalListed = allDocs.size,
                        totalSold = sold.size,
                        totalActive = active.size,
                        totalEarned = earned,
                        categoryBreakdown = catBreak,
                        categoryRevenue = catRevenue,
                        soldItems = soldItems,
                        dailyTrend = trendList,
                        bestCategory = bestCat,
                        bestItem = bestItem,
                        unsoldItems = unsoldItems
                    )
                    _isLoading.value = false
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
