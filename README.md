# AuthX

AuthX é um cofre digital para Android focado em privacidade. Ele combina gerenciamento de senhas, armazenamento de dados sensíveis (cartões, endereços e notas) e autenticação em dois fatores (TOTP) em um único app, com uma interface limpa em Jetpack Compose.

**Sem nuvem. Sem conta. Sem rastreamento.** Todos os dados ficam exclusivamente no dispositivo, protegidos por criptografia forte. O AuthX funciona offline e não depende de servidor próprio ou serviço de nuvem para armazenar ou sincronizar seus dados.

## Funcionalidades

- **Cofre local criptografado** — logins, cartões, endereços, notas e segredos TOTP em um banco SQLCipher protegido por passphrase e biometria
- **TOTP / 2FA** — códigos de autenticação em dois fatores com configuração por leitura de QR Code e suporte a SHA1, SHA256 e SHA512, além de dígitos e período configuráveis
- **Android Autofill** — preenchimento automático de credenciais, cartões e endereços diretamente pelo sistema
- **Gerador de senhas** — senhas fortes geradas no app e nas sugestões do Autofill
- **Backups `.authx`** — exportação e importação com criptografia portátil (senha + AES-256-GCM), além da importação de CSVs antigos
- **Biometria e tela segura** — desbloqueio por biometria/passphrase e bloqueio de captura de tela
- **Interface minimalista** — design system próprio com escala de cinza e acentos âmbar/teal, tipografia Poppins e animação Lottie no splash

## Segurança

A arquitetura segue o princípio de *defesa em profundidade*:

1. **Banco local:** o Room usa SQLCipher (AES-256) com uma passphrase gerada e protegida no dispositivo. Os dados do cofre não são armazenados em texto puro.
2. **Backups `.authx` (formato V2):** arquivos criptografados com **PBKDF2-HMAC-SHA256 (600.000 iterações) + AES-256-GCM**, com sal e IV aleatórios e cabeçalho autenticado (AAD). Diferentemente da versão antiga, o backup não depende do Android Keystore e é portátil entre dispositivos e instalações, desde que a senha escolhida durante a exportação esteja disponível.
3. **Legado V1:** backups antigos, cuja chave dependia do Android Keystore, ainda podem ser lidos para importação, mas não são mais gerados.
4. **Autenticação:** biometria + passphrase para desbloquear o cofre. `FLAG_SECURE` impede a captura de tela enquanto o app está protegido.

> O app não envia dados do cofre para fora do dispositivo. Os únicos componentes opcionais que dependem do Google Play Services instalado no aparelho são a fonte via Google Fonts e o modelo *on-device* de leitura de QR do ML Kit. Esses componentes não são usados para transmitir os dados armazenados no cofre.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin 1.9.22 |
| UI | Jetpack Compose (BOM 2023.10.01) + Material 3 |
| Persistência | Room 2.6.1 + SQLCipher 4.5.3 |
| Criptografia | javax.crypto (AES-GCM), PBKDF2, commons-codec |
| TOTP | kotlin-onetimepassword 2.1.0 |
| QR Code | CameraX 1.3.1 + ML Kit Barcode Scanning 17.2.0 |
| Autofill | androidx.autofill |
| Animações | Lottie 6.2.0 |
| Imagens | Coil 2.5.0 |
| Preferências | DataStore 1.0.0 |
| Build | AGP 8.6.0, minSdk 24, target/compileSdk 35 |

## Estrutura do projeto

```text
app/src/main/java/com/vxncius/authx/
├── MainActivity.kt
├── data/
│   ├── AppDatabase.kt
│   ├── VaultDao.kt
│   └── VaultItem.kt
├── logic/
│   ├── AuthxFileCrypto.kt
│   ├── CsvHandler.kt
│   ├── ImportValidator.kt
│   ├── TotpManager.kt
│   └── BiometricHelper.kt
├── service/
│   └── MyAutofillService.kt
└── ui/
    ├── HomeScreen.kt
    ├── SettingsScreen.kt
    ├── SplashScreen.kt
    ├── AddItemScreen.kt
    ├── AddCardScreen.kt
    ├── AddAddressScreen.kt
    ├── ItemDetailScreen.kt
    ├── PasswordGeneratorScreen.kt
    └── theme/

app/src/test/
```

A lógica de criptografia e importação (`logic/`) é **pura JVM** e coberta por testes unitários (`app/src/test`), mantendo o núcleo sensível independente do Android e verificável em CI.

## Instalação

A versão publicada do AuthX está disponível no GitHub Releases:

* [GitHub Releases — v1.22.0](https://github.com/vxncius-dev/AuthX/releases/download/v1.22.0/app-release.apk)

### Build local

```bash
git clone https://github.com/vxncius-dev/AuthX.git
cd AuthX
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Para builds de release, as credenciais de assinatura são lidas de `local.properties` (ignorado pelo Git) ou de variáveis de ambiente:

```text
AUTHX_RELEASE_STORE_FILE
AUTHX_RELEASE_STORE_PASSWORD
AUTHX_RELEASE_KEY_ALIAS
AUTHX_RELEASE_KEY_PASSWORD
```

## Licença

O código do AuthX é distribuído sob uma licença **Source-Available, Não Comercial** — código-fonte aberto para consulta, estudo e contribuição, sem uso comercial permitido sem autorização prévia. Consulte [LICENSE](LICENSE) para o texto completo da licença.

As dependências de terceiros e suas respectivas licenças estão documentadas em [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES.md).