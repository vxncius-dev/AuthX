# AuthX

AuthX is a privacy-focused Android vault for passwords, payment cards, addresses, and two-factor authentication codes. It combines a local encrypted database, biometric lock, password generation, TOTP support, and Android Autofill integration in a clean Kotlin/Jetpack Compose app.

## Highlights

- Store logins, cards, addresses, notes, and TOTP secrets locally.
- Fill credentials, card data, and address data through Android Autofill.
- Generate strong passwords directly inside the app and from Autofill suggestions.
- Protect access with biometric authentication and secure screen restrictions.
- Import older CSV exports and create encrypted `.authx` backups.
- Scan QR codes for TOTP setup.
- Use a simple dark UI designed for quick access to sensitive data.

## Security Notes

AuthX stores vault data in an encrypted SQLCipher database. Encrypted exports use AES-GCM with a key stored in Android Keystore, which makes exported `.authx` files unreadable outside the app context. Because that key is device/app-installation bound, these backups are intended for secure local restore rather than cross-device migration.

## Tech Stack

- Kotlin
- Jetpack Compose
- Room
- SQLCipher
- Android Autofill Service
- Android Keystore
- CameraX and ML Kit for QR scanning
