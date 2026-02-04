# AuthX

AuthX is an all-in-one digital security solution that combines a premium password manager and a TOTP-based two-factor authenticator in a single, minimalist application.

Developed with a privacy-first mindset, AuthX ensures that your sensitive data remains local and encrypted at all times.

## Key Features

- **Unified Security**: Password management and 2FA codes in one place.
- **Smart Autofill**: Context-aware credential suggestions for apps and websites.
- **Biometric Protection**: Instant access via Fingerprint or Face Recognition.
- **Local Encryption**: Secure local database using SQLCipher.
- **Privacy Guaranteed**: Automatic blur overlay and screen capture protection.
- **Offline First**: No cloud synchronization, giving you total control over your data.

## Mockups

[Insert Mockup Image 1 Here]
[Insert Mockup Image 2 Here]
[Insert Mockup Image 3 Here]

## Installation

To build and run AuthX from source:

1. Clone the repository.
2. Open the project in Android Studio.
3. Build the APK using Gradle: `./gradlew assembleDebug`.
4. Install on your device.

## Security Architecture

AuthX utilizes SQLCipher for database-level encryption. The application logic is designed to minimize the attack surface by keeping all sensitive information within the device's secure storage.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
