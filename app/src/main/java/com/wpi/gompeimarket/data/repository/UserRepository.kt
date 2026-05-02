package com.wpi.gompeimarket.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.wpi.gompeimarket.data.model.UserProfile
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val usersRef = firestore.collection("users")

    suspend fun getProfile(uid: String): UserProfile? {
        return try {
            val doc = usersRef.document(uid).get().await()
            if (doc.exists()) {
                UserProfile(
                    uid = uid,
                    email = doc.getString("email") ?: "",
                    firstName = doc.getString("firstName") ?: "",
                    lastName = doc.getString("lastName") ?: "",
                    displayName = doc.getString("displayName") ?: ""
                )
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun saveProfile(uid: String, firstName: String, lastName: String): Result<Unit> {
        return try {
            val displayName = "$firstName $lastName".trim()
            val data = mapOf(
                "firstName" to firstName.trim(),
                "lastName" to lastName.trim(),
                "displayName" to displayName,
                "email" to (auth.currentUser?.email ?: ""),
                "uid" to uid
            )
            usersRef.document(uid).set(data).await()

            // Also update all listings by this seller
            val listings = firestore.collection("listings")
                .whereEqualTo("sellerId", uid).get().await()
            listings.documents.forEach { doc ->
                doc.reference.update("sellerName", displayName)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Not logged in"))
            val email = user.email ?: return Result.failure(Exception("No email"))
            // Re-authenticate first
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Password change failed"))
        }
    }
}
