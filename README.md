# AuthX

AuthX é um cofre digital para Android focado em privacidade. Ele combina gerenciamento de senhas, armazenamento de dados sensíveis (cartões, endereços, notas) e autenticação em dois fatores (TOTP) em um único app, com uma interface limpa em Jetpack Compose.

**Sem nuvem. Sem conta. Sem rastreamento.** Todos os dados ficam exclusivamente no seu dispositivo, protegidos por criptografia forte. O AuthX funciona 100% offline e não depende de nenhum serviço de nuvem ou servidor externo para armazenar ou sincronizar seus dados.

## Funcionalidades

- 🔐 **Cofre local criptografado** — logins, cartões, endereços, notas e segredos TOTP em um banco SQLCipher protegido por passphrase + biometria
- ⏱️ **TOTP / 2FA** — códigos de autenticação em dois fatores com configuração por leitura de QR Code e suporte a SHA1/SHA256/SHA512, dígitos e período configuráveis
- 🖊️ **Android Autofill** — preenchimento automático de credenciais, cartões e endereços diretamente no sistema
- 🎲 **Gerador de senhas** — senhas fortes geradas no app e nas sugestões do Autofill
- 📦 **Backups `.authx`** — exportação e importação com criptografia portátil (senha + AES-256-GCM), além da importação de CSVs antigos
- 👁️ **Biometria e tela segura** — desbloqueio por biometria/passphrase e bloqueio de captura de tela
- 🌑 **UI escura minimalista** — design system próprio (escala de cinza + acentos âmbar/teal), tipografia Poppins e animação Lottie no splash

## Segurança

A arquitetura segue o princípio de *defesa em profundidade*:

1. **Banco local**: o Room usa SQLCipher (AES-256) com uma passphrase gerada/protegida no dispositivo. Nada é gravado em texto puro.
2. **Backups `.authx` (formato V2)**: arquivos criptografados com **PBKDF2-HMAC-SHA256 (600.000 iterações) + AES-256-GCM**, com sal e IV aleatórios e cabeçalho autenticado (AAD). Diferente da versão antiga, o backup **não depende do Android Keystore**: ele é portátil entre dispositivos/instalações desde que você saiba a senha escolhida na exportação.
3. **Legado V1**: backups antigos (chave no Android Keystore) ainda podem ser **lidos** para importação, mas não são mais gerados.
4. **Autenticação**: biometria + passphrase para desbloquear o cofre; `FLAG_SECURE` impede captura de tela.

> O app não envia nenhum dado para fora do dispositivo. Os únicos componentes opcionais que dependem do Google Play Services instalado no aparelho são a fonte via **Google Fonts** e o modelo *on-device* de leitura de QR (**ML Kit**) — ambos operam localmente e não transmitem dados seus.

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

```
app/src/main/java/com/vxncius/authx/
├── MainActivity.kt        # Navegação, splash, desbloqueio e orquestração das telas
├── data/                  # Camada de dados (Room + SQLCipher)
│   ├── AppDatabase.kt     # Banco criptografado (SupportFactory do SQLCipher)
│   ├── VaultDao.kt        # DAO dos itens do cofre
│   └── VaultItem.kt       # Entidade (logins, cartões, endereços, TOTP…)
├── logic/                 # Regras de negócio puras e testáveis (JVM)
│   ├── AuthxFileCrypto.kt # Formato .authx V2 (PBKDF2 + AES-GCM) e parser CSV
│   ├── CsvHandler.kt      # I/O via SAF + leitura do formato legado V1
│   ├── ImportValidator.kt # Deduplicação na importação de backups
│   ├── TotpManager.kt     # Geração de códigos TOTP
│   └── BiometricHelper.kt # Biometria / passphrase
├── service/
│   └── MyAutofillService.kt # Android Autofill
└── ui/                    # Telas e componentes Jetpack Compose
    ├── HomeScreen.kt      # Lista do cofre + anel de progresso do TOTP
    ├── SettingsScreen.kt  # Configurações, export/import de backups
    ├── SplashScreen.kt    # Animação Lottie
    ├── AddItemScreen.kt / AddCardScreen.kt / AddAddressScreen.kt
    ├── ItemDetailScreen.kt / PasswordGeneratorScreen.kt
    └── theme/             # Design system (AuthXDesign.kt)

app/src/test/              # Testes unitários (cripto, importação, validação)
```

A lógica de criptografia e importação (`logic/`) é **pura JVM** e coberta por testes unitários (`app/src/test`), mantendo o núcleo sensível independente do Android e verificável em CI.

## Instalação

Baixe o APK publicado:

- [GitHub Releases (v1.22.0)](https://github.com/vxncius-dev/AuthX/releases/download/v1.22.0/app-release.apk)
- [Uptodown](https://authx.br.uptodown.com/android)

### Build local

```bash
git clone https://github.com/vxncius-dev/AuthX.git
cd AuthX
./gradlew assembleDebug          # APK de debug
./gradlew testDebugUnitTest      # testes unitários
```

Para o **release**, as credenciais de assinatura são lidas de `local.properties` (ignorado pelo git) ou de variáveis de ambiente:
`AUTHX_RELEASE_STORE_FILE`, `AUTHX_RELEASE_STORE_PASSWORD`, `AUTHX_RELEASE_KEY_ALIAS`, `AUTHX_RELEASE_KEY_PASSWORD`.

## Licenças

- O código do AuthX é distribuído sob a licença **Source-Available, Não Comercial** — veja [LICENSE](LICENSE).
- As dependências de terceiros e suas licenças estão documentadas em [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
