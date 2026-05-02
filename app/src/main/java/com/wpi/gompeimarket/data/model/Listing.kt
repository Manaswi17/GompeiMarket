package com.wpi.gompeimarket.data.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class Listing(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val sellerId: String = "",
    val sellerEmail: String = "",
    val sellerName: String = "",        // First + Last name from user profile
    val meetupZone: String = "",
    val relatedSearches: List<String> = emptyList(),
    val status: String = "active",
    val createdAt: @RawValue Timestamp = Timestamp.now()
) : Parcelable
