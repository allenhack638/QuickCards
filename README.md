# QuickCards - Secure Card Management Android App

QuickCards is a secure, local-first Android application that allows users to safely store, manage, and quickly access their credit/debit card details with robust biometric security and a clean, intuitive user interface.

## Features

### 🔒 Security
- **Biometric Authentication**: Fingerprint/face unlock and device lock integration
- **AES-256 GCM Encryption**: All sensitive card data encrypted at rest
- **Local Storage Only**: No cloud sync, all data stays on your device
- **Auto-lock**: Automatic app locking after inactivity
- **CVV Protection**: CVV hidden by default, requires authentication to view
- **Screenshot Prevention**: App prevents screenshots for security

### 📱 User Interface
- **Two-Tab Navigation**: Clean bottom navigation between Cards and Settings
- **Material Design 3**: Modern, consistent UI following Material Design guidelines
- **Search Functionality**: Search cards by number, bank, tags, or description
- **Card Management**: Add, edit, delete, and view card details
- **Tag System**: Organize cards with customizable tags
- **Copy Functionality**: One-tap copying of card numbers and expiry dates

### 🛠 Technical Features
- **System Card Scanning**: Integration with Android's built-in card scanning capabilities
- **Room Database**: Local SQLite database with encryption
- **MVVM Architecture**: Clean architecture with ViewModels and LiveData
- **Jetpack Compose**: Modern Android UI toolkit
- **Kotlin**: 100% Kotlin codebase

## Technology Stack

- **Language**: Kotlin
- **Platform**: Android (API 26+)
- **Architecture**: MVVM with Jetpack Compose
- **Database**: Room with SQLCipher encryption
- **Authentication**: Android BiometricPrompt API
- **UI Framework**: Jetpack Compose with Material Design 3

## Project Structure

```
app/
├── src/main/java/com/quickcards/app/
│   ├── data/
│   │   ├── dao/           # Database access objects
│   │   ├── database/      # Room database setup
│   │   └── model/         # Data models (Card, Bank, Tag)
│   ├── security/          # Security components (encryption, biometric auth)
│   ├── ui/
│   │   ├── components/    # Reusable UI components
│   │   ├── screens/       # App screens (Cards, Settings, etc.)
│   │   └── theme/         # Material Design theme
│   ├── viewmodel/         # ViewModels for MVVM architecture
│   └── MainActivity.kt    # Main activity with authentication
└── src/main/res/          # Android resources (layouts, strings, etc.)
```

## Setup and Installation

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API 26 or higher
- Device with biometric authentication (recommended)

### Build Instructions

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd QuickCards
   ```

2. **Open in Android Studio**:
   - Open Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the QuickCards directory and select it

3. **Sync dependencies**:
   - Android Studio will automatically sync Gradle dependencies
   - If not, click "Sync Now" in the notification bar

4. **Build the project**:
   ```bash
   ./gradlew build
   ```

5. **Run on device/emulator**:
   - Connect an Android device with USB debugging enabled, or start an emulator
   - Click the "Run" button in Android Studio or use:
   ```bash
   ./gradlew installDebug
   ```

### Dependencies

Key dependencies used in this project:

```gradle
// Jetpack Compose
implementation 'androidx.compose.ui:ui'
implementation 'androidx.compose.material3:material3'
implementation 'androidx.navigation:navigation-compose'

// Room Database
implementation 'androidx.room:room-runtime:2.5.0'
implementation 'androidx.room:room-ktx:2.5.0'

// Biometric Authentication
implementation 'androidx.biometric:biometric:1.1.0'

// Security & Encryption
implementation 'androidx.security:security-crypto:1.1.0-alpha06'

// Other Jetpack components
implementation 'androidx.lifecycle:lifecycle-viewmodel-compose'
implementation 'androidx.activity:activity-compose'
```

## Security Features

### Data Encryption
- All sensitive card data (number, expiry, CVV) is encrypted using AES-256 GCM
- Encryption keys are stored in Android Keystore for hardware-backed security
- Database files are not backed up or synchronized

### Authentication
- App requires biometric authentication on launch
- CVV viewing requires separate authentication
- Auto-lock after 30 seconds of inactivity (configurable)
- App locks when backgrounded and returns to foreground

### Privacy
- No network permissions - app works entirely offline
- Screenshots and screen recording blocked
- App excluded from recent apps preview
- No cloud backup or data extraction allowed

## Usage

### Adding Cards
1. Tap the "+" button on the Cards tab
2. Use "Scan Card" to automatically detect card number and expiry
3. Manually enter CVV (never auto-detected for security)
4. Select bank from dropdown list
5. Add tags and description as needed

### Viewing Card Details
1. Tap any card from the list
2. View masked card information
3. Copy card number or expiry date with one tap
4. Authenticate to view CVV (auto-hides after 10 seconds)

### Managing Data
1. Go to Settings tab
2. Manage banks and tags for better organization
3. View app statistics and security information
4. Clear all data (requires authentication)

## Contributing

This is a security-focused application. When contributing:

1. Follow Android security best practices
2. Ensure all sensitive data remains encrypted
3. Test biometric authentication thoroughly
4. Validate that no data leaks through logs or unencrypted storage
5. Maintain the offline-first approach

## Security Considerations

⚠️ **Important Security Notes**:

- This app stores financial information - security is paramount
- Regular security audits and updates are recommended
- Users should keep their device OS updated for latest security patches
- Biometric authentication should be properly configured on the device
- App should only be installed from trusted sources

## License

This project is created for educational and personal use. Please ensure compliance with local regulations regarding financial data storage and handling.

## Disclaimer

This application is designed for personal card management. Users are responsible for:
- Keeping their devices secure
- Using strong device authentication
- Regular app updates
- Compliance with financial regulations in their jurisdiction

The developers are not responsible for any data loss or security breaches resulting from improper use or device compromise.