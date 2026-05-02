package com.wpi.gompeimarket.data.repository

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class AIResult(
    val category: String,
    val description: String,
    val relatedSearches: List<String> = emptyList()
)

@Singleton
class AIRepository @Inject constructor(
    private val generativeModel: GenerativeModel
) {
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(0.6f).build()
    )

    suspend fun analyzeImage(bitmap: Bitmap): Result<AIResult> {
        return try {
            val labels = labeler.process(InputImage.fromBitmap(bitmap, 0)).await()
                .sortedByDescending { it.confidence }

            val topLabel = labels.firstOrNull()?.text ?: "item"
            val allLabels = labels.take(5).joinToString(", ") { it.text }
            val topTexts = labels.take(5).map { it.text.lowercase() }
            val category = detectCategory(topTexts)
            val geminiBitmap = scaleBitmap(bitmap, 1024)

            val prompt = """IMAGE ANALYSIS TASK for WPI Student Marketplace:
Identify the object in this photo. 

Contextual Hints (may be incorrect): $allLabels

STRICT RULES:
1. If the hints say one thing (e.g., 'helmet') but the image clearly shows another (e.g., 'computer mouse'), DESCRIBE WHAT YOU SEE IN THE IMAGE.
2. Be specific about brand, model, and color.
3. Keep the tone professional but helpful for students.

REQUIRED FORMAT:
DESCRIPTION: [2 sentences. Sentence 1: Identification & condition. Sentence 2: Why it's useful at WPI.]
SEARCHES: [term 1] | [term 2] | [term 3] | [term 4] | [term 5]"""

            var description = fallbackDesc(category, topLabel)
            var relatedSearches = generateFallbackSearches(topTexts, category)

            try {
                val response = generativeModel.generateContent(content { image(geminiBitmap); text(prompt) })
                val text = response.text?.trim() ?: ""
                val descLine = text.lines().firstOrNull { it.startsWith("DESCRIPTION:") }
                val searchLine = text.lines().firstOrNull { it.startsWith("SEARCHES:") }
                if (descLine != null) description = descLine.removePrefix("DESCRIPTION:").trim()
                if (searchLine != null) {
                    val parsed = searchLine.removePrefix("SEARCHES:").trim()
                        .split("|").map { it.trim() }.filter { it.isNotBlank() }.take(5)
                    if (parsed.isNotEmpty()) relatedSearches = parsed
                }
            } catch (e: Exception) {
                android.util.Log.e("AIRepo", "Gemini failed: ${e.message}")
            }

            Result.success(AIResult(category = category, description = description, relatedSearches = relatedSearches))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxPx: Int): Bitmap {
        val w = bitmap.width; val h = bitmap.height
        if (w <= maxPx && h <= maxPx) return bitmap
        val ratio = maxPx.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * ratio).toInt(), (h * ratio).toInt(), true)
    }

    private fun fallbackDesc(category: String, label: String) =
        "A $label available for WPI students. Great deal for anyone looking for $category on campus."

    private fun generateFallbackSearches(labels: List<String>, category: String): List<String> =
        (labels.take(3).map { "used $it" } + listOf("cheap $category", "$category WPI")).distinct().take(5)

    private fun detectCategory(texts: List<String>): String {
        val combined = texts.joinToString(" ")
        return when {
            combined.containsAny("phone","iphone","android","smartphone","laptop","macbook","computer","tablet","ipad","headphone","airpod","earbud","cable","charger","speaker","camera","monitor","keyboard","mouse","electronic","gadget") -> "Electronics"
            combined.containsAny("shirt","shoe","sneaker","jacket","hoodie","sweater","cloth","pant","jeans","dress","coat","boot","apparel","bag","backpack","cap","hat","wear") -> "Clothes"
            combined.containsAny("chair","table","desk","sofa","couch","furniture","shelf","bookcase","drawer","mattress","bed","cabinet") -> "Furniture"
            combined.containsAny("book","textbook","notebook","binder","study","novel","manual","workbook","literature") -> "Textbooks"
            combined.containsAny("bike","bicycle","scooter","skateboard","cycle","transport","vehicle") -> "Transportation"
            combined.containsAny("plate","cup","bowl","pot","pan","mug","bottle","kitchen","cook","cutlery","food","drink") -> "Kitchen"
            combined.containsAny("lamp","pillow","blanket","sheet","dorm","storage","bin","fan","mirror","rug","towel","organizer") -> "Dorm Essentials"
            else -> "Others"
        }
    }

    private fun String.containsAny(vararg keywords: String) = keywords.any { this.contains(it) }
}
