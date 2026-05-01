# AuthX

AuthX é um cofre digital para Android focado em armazenamento seguro de dados sensíveis, combinando gerenciamento de senhas, dados pessoais e autenticação TOTP em uma única aplicação.

Todos os dados são mantidos localmente e protegidos por criptografia forte, sem uso de nuvem, rastreamento ou dependências externas.

## Funcionalidades

- Armazenamento local de logins, cartões, endereços, notas e códigos TOTP
- Integração com Android Autofill para preenchimento seguro
- Geração de senhas integrada ao app e ao Autofill
- Proteção por biometria e restrição de captura de tela
- Importação de CSV e exportação de backups criptografados `.authx`
- Leitura de QR Code para configuração de TOTP
- Interface minimalista focada em acesso rápido

## Segurança

- Banco de dados criptografado com SQLCipher
- Exportações protegidas com AES-GCM
- Chaves armazenadas no Android Keystore (binding ao dispositivo)
- Arquitetura offline-first para reduzir superfície de ataque

## Decisions

- Uso de SQLCipher para controle e granularidade na criptografia
- Keystore-bound keys para impedir acesso externo aos dados exportados
- Ausência de cloud para garantir privacidade e previsibilidade
- Processamento local para eliminar dependências externas

## Stack

- Kotlin
- Jetpack Compose
- Room
- SQLCipher
- Android Autofill Service
- Android Keystore
- CameraX + ML Kit

## Instalação

Baixe para Android [aqui](https://github.com/vxncius-dev/AuthX/releases/download/v1.21.5/app-release.apk) ou faça o build local:

```bash
git clone <repo>
cd <repo>
./gradlew assembleDebug
```
