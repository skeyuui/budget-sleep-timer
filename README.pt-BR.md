# Temporizador para Android TV 📺
[Read in English](README.md)

Um aplicativo minimalista e ultraleve para Android TV que desliga a sua TV em horários programados. Sem bibliotecas pesadas, sem rastreadores, apenas código Android puro.

**"Desligar toda sexta à 1:00"** · **"Desligar todo dia às 4:00"** · **"Dormir em 30 minutos"**

## Como funciona

1. Defina um horário e os dias da semana (ou use os atalhos Diário/Dias úteis/Finais de semana).
2. Pressione "Definir" — e pronto!

Quando o alarme dispara, o aplicativo se conecta ao ADB interno da TV em `localhost:5555` e envia o comando de dormir (`input keyevent 223`). Se o ADB não estiver disponível, ele tenta executar um comando `Runtime.exec()` (para TVs com root).

### Segurança
O aplicativo possui um cliente de autenticação ADB próprio. Ele gera dinamicamente uma chave RSA de 2048 bits única no seu dispositivo, para que você não compartilhe chaves fixas com outros usuários. Basta marcar "Sempre permitir deste computador" na sua TV quando solicitado.

## Requisitos

- Dispositivo Android TV (API 21+)
- **Depuração USB / Depuração de Rede ativada** nas Opções do Desenvolvedor

## Compilar e Instalar

```bash
# Compilar
./gradlew assembleDebug

# Instalar
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Licença

MIT
