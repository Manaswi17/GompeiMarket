package com.wpi.gompeimarket.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.wpi.gompeimarket.data.model.Listing
import com.wpi.gompeimarket.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val listingsRef = firestore.collection(Constants.FIRESTORE_LISTINGS_COLLECTION)

    fun getListings(): Flow<List<Listing>> = callbackFlow {
        val listener = listingsRef
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val listings = snapshot?.documents?.mapNotNull {
                    it.toObject(Listing::class.java)?.copy(id = it.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(listings)
            }
        awaitClose { listener.remove() }
    }

    fun getListingsBySeller(sellerId: String): Flow<List<Listing>> = callbackFlow {
        val listener = listingsRef
            .whereEqualTo("sellerId", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val listings = snapshot?.documents?.mapNotNull {
                    it.toObject(Listing::class.java)?.copy(id = it.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(listings)
            }
        awaitClose { listener.remove() }
    }

    suspend fun postListing(listing: Listing, imageUri: Uri): Result<String> {
        return try {
            // Fetch sellerName from user profile if not already set
            val sellerName = listing.sellerName.takeIf { it.isNotBlank() } ?: run {
                try {
                    val userDoc = firestore.collection("users").document(listing.sellerId).get().await()
                    userDoc.getString("displayName") ?: listing.sellerEmail.substringBefore("@")
                } catch (e: Exception) {
                    listing.sellerEmail.substringBefore("@")
                }
            }

            val imageFileName = "${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child("${Constants.STORAGE_LISTINGS_PATH}$imageFileName")
            storageRef.putFile(imageUri).await()
            val imageUrl = storageRef.downloadUrl.await().toString()

            val listingWithImage = listing.copy(imageUrl = imageUrl, sellerName = sellerName)
            val docRef = listingsRef.add(listingWithImage).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsSold(listingId: String): Result<Unit> {
        return try {
            listingsRef.document(listingId).update("status", "sold").await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteListing(listing: Listing): Result<Unit> {
        return try {
            listingsRef.document(listing.id).delete().await()
            if (listing.imageUrl.isNotEmpty()) {
                storage.getReferenceFromUrl(listing.imageUrl).delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}
