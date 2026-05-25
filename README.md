# GompeiMarket
**A WPI-exclusive Campus Marketplace**

GompeiMarket is a specialized mobile marketplace application designed specifically for the Worcester Polytechnic Institute (WPI) community. It facilitates safe and efficient buying and selling of items among students and faculty, leveraging modern Android technologies and AI integrations.

---

## Project Structure

The project follows a clean architecture pattern with MVVM (Model-View-ViewModel).

### `app/src/main/java/com/wpi/gompeimarket/`

* **`ui/`**: Contains the Jetpack Compose UI components, organized by feature:
    * `auth/`: Login and Registration screens with @wpi.edu enforcement.
    * `feed/`: The main marketplace feed showing all active listings.
    * `post/`: Screen for creating new listings, featuring CameraX integration.
    * `detail/`: Detailed view of a single listing with Google Maps for meetup locations.
    * `profile/`: User profile management.
    * `analytics/`: Insights and statistics about marketplace usage.
    * `inbox/` & `chat/`: Messaging system for buyer-seller communication.
    * `userlistings/`: View for managing your own posted items.
    * `edit/`: Functionality to modify existing listings.
* **`data/`**: Handles data operations:
    * `model/`: Data classes like `Listing` and `UserProfile`.
    * `repository/`: Orchestrates data flow between Firebase and the UI.
        * `AuthRepository`: Handles Firebase Authentication.
        * `ListingRepository`: Manages Firestore and Storage for items.
        * `UserRepository`: Manages user profiles.
        * `AIRepository`: Interfaces with Gemini for AI descriptions.
* **`di/`**: Hilt Dependency Injection modules (`AppModule.kt`).
* **`util/`**: Utility classes and constants (e.g., safe meetup zones).
* **`MainActivity.kt`**: The entry point of the application.
* **`GompeiMarketApp.kt`**: Application class for Hilt initialization.

---

## Tech Stack

| Component | Technology |
| :--- | :--- |
| **Language** | Kotlin |
| **UI** | Jetpack Compose |
| **Architecture** | MVVM + Repository Pattern |
| **DI** | Dagger Hilt |
| **Backend** | Firebase (Auth, Firestore, Storage) |
| **AI (Generative)** | Gemini API (Automatic Description Generation) |
| **AI (Vision)** | MLKit Image Labeling (Automatic Categorization) |
| **Maps** | Google Maps SDK for Android |
| **Images** | Coil-compose |
| **Camera** | CameraX |

---

## How to Compile and Run

### 1. Prerequisites
* **Android Studio** (Ladybug or newer recommended).
* **JDK 17**.
* **Android Device/Emulator** (API level 26 or higher).

### 2. Setup Configuration Files
The app requires two sensitive configuration files to build and run correctly:

* **`app/google-services.json`**: Contains Firebase configuration. You must provide your own file from the Firebase Console.
* **`app/src/main/res/values/secrets.xml`**: Contains API keys.
    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <string name="gemini_api_key">YOUR_GEMINI_API_KEY_HERE</string>
        <string name="google_maps_key">YOUR_GOOGLE_MAPS_API_KEY_HERE</string>
    </resources>
    ```

### 3. Build & Run
1.  Open the project in Android Studio.
2.  Click **"Sync Project with Gradle Files"** and wait for completion.
3.  Select your device/emulator.
4.  Click the **Run** button (green play icon) or press `Shift + F10`.

---

## Key Features

* **Secure WPI Login**: Restricts access to users with a valid `@wpi.edu` email address via Firebase Auth.
* **Smart Listing Creation**:
    * **Auto-Categorization**: Uses MLKit to recognize items from photos and suggest categories.
    * **AI Descriptions**: Automatically generates catchy item descriptions using the Gemini API.
* **Safe Meetup Zones**: Integrated Google Maps showing verified, well-lit safe zones on the WPI campus for exchanging items.
* **Real-time Analytics**: Tracks trends and provides users with insights into their marketplace activity.
* **Integrated Messaging**: Chat directly with sellers/buyers within the app.

---

## Firebase Security Rules

To ensure data privacy and restrict platform access to authorized university users, the backend enforces the following production-ready security rules:

### 1. Cloud Storage Rules
Restricts image uploads (such as listing photos) exclusively to authenticated accounts possessing a verified `@wpi.edu` email address.

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /listings/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
                    request.auth.token.email.matches('.*@wpi\\.edu');
    }
  }
}
```

### 2. Firestore Database Rules
Configures precise, document-level data constraints across collections to manage marketplace items, user profiles, and secure messaging threads:
``` javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Listings — anyone authenticated can read; only owner can write
    match /listings/{listingId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && request.auth.uid == request.resource.data.sellerId;
      allow update, delete: if request.auth != null && request.auth.uid == resource.data.sellerId;
    }

    // Users — read own profile; write own profile only
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }

    // Chats — participants can read and write their own room
    match /chats/{chatId} {
      allow read, write: if request.auth != null &&
        request.auth.uid in resource.data.participants;

      // Allow creating a new chat room if you're one of the participants
      allow create: if request.auth != null &&
        request.auth.uid in request.resource.data.participants;

      // Messages sub-collection
      match /messages/{messageId} {
        allow read, write: if request.auth != null;
      }
    }
  }
}
```
