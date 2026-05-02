package com.wpi.gompeimarket.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.wpi.gompeimarket.util.Constants
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    val currentUser: FirebaseUser? get() = firebaseAuth.currentUser

    fun isLoggedIn(): Boolean = currentUser != null
    suspend fun signIn(email: String, password: String): AuthResult {
        if (!email.endsWith(Constants.WPI_EMAIL_DOMAIN)) {
            return AuthResult.Error("Only @wpi.edu email addresses are allowed.")
        }
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Error("Sign in failed.")
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun register(email: String, password: String): AuthResult {
        if (!email.endsWith(Constants.WPI_EMAIL_DOMAIN)) {
            return AuthResult.Error("Only @wpi.edu email addresses are allowed.")
        }
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Error("Registration failed.")
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Unknown error")
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
