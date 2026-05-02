package com.wpi.gompeimarket.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.wpi.gompeimarket.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ProfileUiState(
    val firstName: String = "",
    val lastName: String = "",
    val totalListed: Int = 0,
    val totalSold: Int = 0,
    val totalEarned: Double = 0.0,
    val isLoaded: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state

    init { loadProfile() }

    fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(uid).get().await()
                val fn = userDoc.getString("firstName") ?: ""
                val ln = userDoc.getString("lastName") ?: ""
                val docs = firestore.collection("listings").whereEqualTo("sellerId", uid).get().await()
                val sold = docs.filter { it.getString("status") == "sold" }

                _state.value = ProfileUiState(
                    firstName = fn,
                    lastName = ln,
                    totalListed = docs.size(),
                    totalSold = sold.size,
                    totalEarned = sold.sumOf { it.getDouble("price") ?: 0.0 },
                    isLoaded = true
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoaded = true)
            }
        }
    }

    fun saveName(firstName: String, lastName: String, onDone: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                UserRepository(auth, firestore).saveProfile(uid, firstName, lastName)
                _state.value = _state.value.copy(firstName = firstName, lastName = lastName)
            } catch (_: Exception) {}
            onDone()
        }
    }

    fun changePassword(current: String, new: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val result = UserRepository(auth, firestore).changePassword(current, new)
            onResult(if (result.isSuccess) null else result.exceptionOrNull()?.message ?: "Failed")
        }
    }
}
