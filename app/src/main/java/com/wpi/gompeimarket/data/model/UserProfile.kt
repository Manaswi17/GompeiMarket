package com.wpi.gompeimarket.data.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val displayName: String = ""     // firstName + " " + lastName
)
