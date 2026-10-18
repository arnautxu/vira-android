# Vira per a Android

App nativa d'Android (Kotlin + Jetpack Compose) de **Vira**, calcada de l'app d'iPhone
(`arnautxu/VIRA-APP-I-WEB`), que és l'espec i no es toca.

- Com es treballa, l'estructura i les decisions: `CLAUDE.md`.
- Pla sencer (fases, fils, regles): https://claude.ai/code/artifact/aae6ec1b-c593-437b-acf9-5198e8b7b5f7

```bash
./gradlew assembleDebug testDebugUnitTest
```

La CI (`.github/workflows/android.yml`) compila a cada push, deixa l'APK com a artefacte i, a
`main`, el publica com a release per instal·lar-lo al mòbil.
