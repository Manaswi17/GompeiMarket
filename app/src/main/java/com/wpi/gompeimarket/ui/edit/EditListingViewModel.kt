package com.wpi.gompeimarket.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class EditListingViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _saveResult = MutableStateFlow<Result<Unit>?>(null)
    val saveResult: StateFlow<Result<Unit>?> = _saveResult

    fun updateListing(
        listingId: String,
        title: String,
        category: String,
        description: String,
        meetupZone: String
    ) {
        _isSaving.value = true
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "title" to title,
                    "category" to category,
                    "description" to description,
                    "meetupZone" to meetupZone
                )
                firestore.collection("listings").document(listingId).update(updates).await()
                _saveResult.value = Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("EditListingViewModel", "Update failed: ${e.message}")
                _saveResult.value = Result.failure(e)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }
}
