# AuthX

AuthX é um cofre digital para Android focado em privacidade, que combina gerenciamento de senhas, armazenamento de dados sensíveis e autenticação em dois fatores (TOTP) em uma única aplicação. Todos os dados são armazenados localmente e protegidos por criptografia forte, sem uso de nuvem, rastreamento ou dependências externas.

## Funcionalidades

- Armazena logins, cartões, endereços, notas e códigos TOTP localmente
- Preenchimento automático de credenciais, cartões e dados de endereço via Android Autofill
- Geração de senhas seguras integrada ao app e ao Autofill
- Proteção com biometria e restrições de captura de tela
- Importação de arquivos CSV e backups criptografados `.authx`
- Leitura de QR Code para configuração de TOTP
- Interface minimalista em modo escuro, focada em acesso rápido

## Segurança

Os dados são armazenados em um banco criptografado com SQLCipher.  
Exportações utilizam AES-GCM com chave protegida pelo Android Keystore, tornando os arquivos `.authx` acessíveis apenas no contexto do app e do dispositivo.

## Stack

- Kotlin
- Jetpack Compose
- Room
- SQLCipher
- Android Autofill Service
- Android Keystore
- CameraX + ML Kit

## Instalação

Baixe o APK:

[Download na Uptodown](https://authx.br.uptodown.com/android)

### Build local

```bash
git clone <repo>
cd <repo>
./gradlew assembleDebug
