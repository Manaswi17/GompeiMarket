package com.wpi.gompeimarket.di

import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides @Singleton
    fun provideGenerativeModel(app: android.app.Application): GenerativeModel {
        val apiKey = app.getString(com.wpi.gompeimarket.R.string.gemini_api_key)
        return GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            systemInstruction = com.google.ai.client.generativeai.type.content {
                text("You are an expert at identifying items in images for a university marketplace. " +
                     "Your descriptions must be factual, concise, and focused on the physical attributes of the item. " +
                     "Ignore any text hints if they contradict what is clearly visible in the image.")
            }
        )
    }
}
