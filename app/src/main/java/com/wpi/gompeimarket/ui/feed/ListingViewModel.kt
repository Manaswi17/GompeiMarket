package com.wpi.gompeimarket.ui.feed

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.wpi.gompeimarket.data.model.Listing
import com.wpi.gompeimarket.data.repository.ListingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val listings: List<Listing> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class ListingViewModel @Inject constructor(
    private val listingRepository: ListingRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _feedState = MutableStateFlow(FeedUiState())
    val feedState: StateFlow<FeedUiState> = _feedState

    private val _postResult = MutableStateFlow<String?>(null)
    val postResult: StateFlow<String?> = _postResult

    val currentUserId: String? get() = auth.currentUser?.uid
    val currentUserEmail: String? get() = auth.currentUser?.email

    init {
        loadFeed()
    }

    private fun loadFeed() {
        viewModelScope.launch {
            listingRepository.getListings()
                .catch { e -> _feedState.value = FeedUiState(isLoading = false, errorMessage = e.message) }
                .collect { listings ->
                    _feedState.value = FeedUiState(listings = listings, isLoading = false)
                }
        }
    }

    fun getMyListings(): Flow<List<Listing>> {
        val uid = currentUserId ?: return flowOf(emptyList())
        return listingRepository.getListingsBySeller(uid)
    }

    fun postListing(listing: Listing, imageUri: Uri) {
        viewModelScope.launch {
            val result = listingRepository.postListing(listing, imageUri)
            if (result.isSuccess) {
                _postResult.value = result.getOrNull()
            } else {
                _feedState.value = _feedState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun markAsSold(listingId: String) {
        viewModelScope.launch { listingRepository.markAsSold(listingId) }
    }

    fun deleteListing(listing: Listing) {
        viewModelScope.launch { listingRepository.deleteListing(listing) }
    }

    fun clearPostResult() { _postResult.value = null }
}
