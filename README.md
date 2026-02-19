<h1 align="center">
  <img width="60" height="60" src="https://github.com/user-attachments/assets/4d07bc76-d20d-458b-ba05-fd0963af2fc9" alt="AuthX logo" /><br/>
  AuthX
</h1>

AuthX is an all-in-one digital security application that combines a premium password manager with a TOTP-based two-factor authenticator in a single, minimalist experience.

[![Download on Uptodown](https://img.shields.io/badge/Download-Uptodown-green)](https://authx.en.uptodown.com/android)

Built with a privacy-first approach, AuthX ensures that all sensitive data remains fully local and encrypted at all times — no cloud, no tracking, no external dependencies.

## Key Features

- **Unified Security**: Password management and TOTP-based 2FA in a single app.
- **Smart Autofill**: Context-aware credential suggestions for apps and websites.
- **Biometric Protection**: Secure and instant access using fingerprint or face recognition.
- **Local Encryption**: Encrypted local database powered by SQLCipher.
- **Privacy Guaranteed**: Automatic blur overlay and screen capture protection.
- **Offline-First**: Fully functional without internet access.

## Mockups

<p align="left">
  <img src="https://github.com/user-attachments/assets/3ad69271-9bf2-4d6f-88ce-d48fafccd32e" alt="Home screen mockup" height="500" />
  <img src="https://github.com/user-attachments/assets/7ad26dfc-9d29-4fb9-8b0c-d780c0a5845a" alt="Settings screen mockup" height="500" />
</p>

## Installation

Download the prebuilt APK from Uptodown:
https://authx.uptodown.com/android

### Build from source

To build and run AuthX locally:

1. Clone this repository.
2. Open the project in Android Studio.
3. Build the debug APK using Gradle:
   ```bash
   ./gradlew assembleDebug

4. Install the generated APK on your Android device.

## Security Architecture

AuthX uses SQLCipher for database-level encryption and Android's secure APIs for biometric authentication.
All sensitive operations are performed locally to reduce the attack surface and eliminate external data exposure.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
