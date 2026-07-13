# Temporizador para Android TV 📺
[Read in English](README.md)

Um aplicativo minimalista e ultraleve para Android TV que desliga a sua TV em horários programados. Sem bibliotecas pesadas, sem rastreadores, apenas código Android puro.

**"Desligar toda sexta à 1:00"** · **"Desligar todo dia às 4:00"** · **"Dormir em 30 minutos"**

## Funcionalidades

- **Zero Memória em Segundo Plano:** Usa  `AlarmManager` nativo do Android. Quando fechado, o app não consome memória até o exato minuto em que o timer dispara.
- **Múltiplas Ações:** Escolha entre **Power (26)**, **Standby (223)**, ou **Hibernate (276)** dependendo do suporte do modelo da sua TV.
- **Chaves ADB Embutidas:** Gera e gerencia automaticamente suas próprias chaves RSA para execução local do ADB—sem necessidade de apps externos.
- **Timers Rápidos Acumulativos:** Aperte +5, +15, +30 para empilhar rapidamente um timer regressivo.

## Como funciona

1. Ao iniciar pela primeira vez, surgirá um aviso pedindo permissão de depuração ADB. Marque "Sempre permitir deste computador" e pressione OK.
2. Defina um horário e os dias da semana (ou use os atalhos Diário/Dias úteis/Finais de semana).
3. Selecione a ação de desligamento desejada (Power, Standby ou Hibernate).
4. Pressione "Definir" — e pronto!

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
