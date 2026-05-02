package com.wpi.gompeimarket.util

object Constants {
    const val WPI_EMAIL_DOMAIN = "@wpi.edu"
    const val FIRESTORE_LISTINGS_COLLECTION = "listings"
    const val STORAGE_LISTINGS_PATH = "listings/"

    data class MeetupZone(val name: String, val lat: Double, val lng: Double)

    // Verified WPI campus coordinates (Worcester, MA 01609)
    val MEETUP_ZONES = listOf(
        MeetupZone("Campus Center Atrium",    42.27482, -71.80808),
        MeetupZone("Gordon Library Lobby",    42.27434, -71.80638),
        MeetupZone("Higgins Labs Lobby",      42.27402, -71.80833),
        MeetupZone("Rec Center Entrance",     42.27401, -71.80946),
        MeetupZone("Fuller Labs Lobby",       42.27494, -71.80658),
        MeetupZone("Salisbury Labs Entrance", 42.27354, -71.80864),
        MeetupZone("Olin Hall Entrance",      42.27434, -71.80745),
        MeetupZone("Washburn Shops",          42.27311, -71.80851)
    )

    val CATEGORIES = listOf(
        "Electronics", "Clothes", "Furniture", "Dorm Essentials",
        "Textbooks", "Transportation", "Kitchen", "Others"
    )

    const val GEMINI_PROMPT = """Look at this image. Write exactly 2 sentences describing this item for a student marketplace listing.
Sentence 1: Describe what the item is, its color, brand if visible, and condition.
Sentence 2: One practical reason a college student would want this.
Be specific and factual. Max 40 words. No price or location mention."""
}
