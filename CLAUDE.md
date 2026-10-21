# CLAUDE.md — Vira per a Android

App nativa d'**Android** de Vira (adolescents amb TDAH, 12–21 anys; mascota **Viri**), **calcada
pantalla a pantalla de l'app d'iPhone**. L'app d'iPhone és l'espec i no es toca mai.
Pla sencer: https://claude.ai/code/artifact/aae6ec1b-c593-437b-acf9-5198e8b7b5f7

```bash
./gradlew assembleDebug testDebugUnitTest   # el que fa la CI a cada push
./gradlew installDebug                      # al mòbil connectat
```

Cal `ANDROID_HOME` (o `local.properties` amb `sdk.dir=`) amb la plataforma 35 i build-tools 35.

## L'espec és l'iPhone

- Repositori `arnautxu/VIRA-APP-I-WEB`: les pantalles (`Vira/Features/…`), el seu `CLAUDE.md`,
  els 24 documents de `docs/context/` i les 475 proves de `ViraTests/`. **Només es llegeix**:
  cap push, cap PR, cap canvi. Un error trobat a l'iPhone es diu i prou.
- Mateix aspecte i mateixes mecàniques: cap pantalla nova, cap color nou, cap regla nova. El que
  no és a l'iPhone ni a `docs/context/` no es fa; si falta una decisió, es pregunta a l'Arnau.
- Les mateixes normes dures que a l'iPhone, sense excepció: `PRIVACY_AND_SAFETY.md` (emocions i
  medicació no surten del telèfon sense el permís del tutor; cap analítica de tercers, per tant
  **res de Firebase**), `ACCESSIBILITY_REQUIREMENTS.md` (TalkBack a tot, lletra al 200 %,
  contrast 4,5:1, tàctils ≥ 48 dp, mai informació només per color), `VIRI_AND_GAMIFICATION.md`,
  `CONTENT_STRATEGY.md` i `DESIGN_RULES.md`.
- Els textos **no s'escriuen a mà**: surten de `Localizable.xcstrings` de l'iPhone amb
  `scripts/export-strings.py` (`values-ca`, `values-es` i, a `values/`, l'anglès, que és l'idioma
  de reserva com a l'iPhone; `ViraLanguage` el dona al que no és un recurs). Un text que no hi és,
  es demana.
- Cap color literal a les pantalles: sempre tokens de `core/design`.

## Estructura i amos

Un sol mòdul Gradle (`app/`), paquets dins de `cat.vira.android`. **Cada fil és amo d'una carpeta
i només hi escriu allà**; un canvi al que és comú es demana al fil amo, mai es fa pel compte propi.

| Carpeta | Amo (fil) | Calca de l'iPhone |
| --- | --- | --- |
| arrel, `app/` (arrencada, 4 pestanyes), `.github/` | Esquelet | `Vira/App/` |
| `core/design/` | Sistema de disseny | `Vira/Core/DesignSystem/` (tokens i components) |
| `core/data/`, `core/backend/` | Dades i sincronització | `Vira/Models/`, `Vira/Core/Backend/`, `Vira/Core/Persistence/` |
| `core/resources/`, `scripts/` | Textos i recursos | `Vira/Resources/` |
| `core/notifications/` | Notificacions | `Vira/Core/Services/*Notification*` |
| `features/entry/` | Entrada | `Vira/Features/Onboarding/` |
| `features/today/` | Avui | `Vira/Features/Today/` |
| `features/plan/` | Pla | `Vira/Features/Plan/` |
| `features/emotions/` | Emocions | `Vira/Features/Emotions/` |
| `features/progress/` | Viri i ratxa | `Vira/Features/Progress/` (progrés, ratxa, reptes) |
| `features/shop/` | Botiga i premis | `Vira/Features/Progress/` (botiga, skins, premis, cartes) |
| `features/focus/` | Concentra't | `Vira/Features/Focus/` |
| `features/settings/` | Ajustos i compte | `Vira/Features/Settings/`, `Account/` |
| `features/help/` | Ajuda ara | `Vira/Features/Help/` |
| `features/parent/` | Panell de pares | `Vira/Features/ParentPanel/` |

## Decisions tècniques (preses; es canvien una vegada i s'anoten aquí)

- Kotlin + Jetpack Compose. Els components són els de Vira, calcats; Material només per sota.
- Android 10 mínim (`minSdk 29`), `compileSdk`/`targetSdk` 35. Identificador `cat.vira.android`.
- Dades locals amb **Room**, les 21 taules i els mateixos noms de camp que SwiftData, perquè
  les files que es sincronitzen siguin idèntiques a les de l'iPhone.
- Compte amb Supabase Auth via l'SDK oficial (`supabase-kt`, només Auth); Google amb Credential
  Manager; Apple per web. **Sincronització amb HTTP pla contra PostgREST**, com l'iPhone amb
  `URLSession`: mateixes files, cursors, làpides i «guanya l'últim»; `ViraSyncScope` amb el
  permís del tutor com a condició. Segon pla amb WorkManager.
- Notificacions locals amb alarmes exactes, un canal per categoria (5), accions als avisos,
  avís de Viri a l'hora triada. Cap push: la Live Activity de ratxa aquí es programa en local.
- Concentra't: servei en primer pla amb avís fix i cronòmetre, Pausa i Acaba com a accions.
- Vídeos i sons amb Media3; els `.mov` a MP4 H.264 i els `.caf` a OGG, convertits per script.
- Captures amb les proves de captura de Compose: cada pantalla en clar, fosc i lletra al 200 %.
  Serveixen per a l'aprovació de l'Arnau i com a prova de regressió. Com que cap fil pot executar
  Gradle (el contenidor no arriba a `dl.google.com`), les fa la CI: a cada push d'una branca les
  publica a `captures/<branca>`, el fil les revisa i, aprovades, les copia a
  `app/src/screenshotTestDebug/reference`; des d'aleshores la CI falla si una captura canvia.
  La galeria del sistema de disseny és a `app/src/screenshotTest/…/core/design/`.
- Privacitat: testimonis al Keystore, `allowBackup=false`, cap analítica, medicació i emocions
  mai als widgets ni a cap superfície fora de l'app.
- Widgets amb Glance; botons del panell ràpid (Concentra't, Respira); accessos de la icona.
- **No es fa**: bloqueig d'apps (cap API sense servei d'accessibilitat), Siri, Spotlight,
  importació de Recordatoris. A l'iPhone de Release tampoc hi ha bloqueig d'apps.

## Com es treballa

- Cada fil a la seva branca; push sovint; `main` sempre compila.
- Cap pantalla va a `main` sense **les dues captures de costat** (iPhone i Android, en clar, fosc i
  lletra gran) al fil i **l'OK de l'Arnau**. Amb l'OK i la CI verda, el fil obre la seva PR i la
  fusiona ell mateix.
- Les captures de l'iPhone surten de `captures/` del projecte Vira App; si en falta una, es
  demana a l'Arnau. **Cap fil obre el simulador d'iPhone.**
- L'Arnau no és programador: respostes en català i planeres, una imatge a cada proposta, i cap
  pantalla que no sigui a l'iPhone sense ensenyar-la-hi abans.
- La CI deixa l'APK de cada branca com a artefacte i, a `main`, com a release (`build-N`) perquè
  l'Arnau l'instal·li al mòbil.
