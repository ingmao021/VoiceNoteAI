# VoiceNoteAI

Smartwatch app (Wear OS) that captures notes by voice, structures them with AI and saves them to Google Tasks.
Product definition: [`docs/PRD.md`](docs/PRD.md).

## Layout

```
wear/      Wear OS app — Gradle module `:wear` (Kotlin, Compose for Wear OS)
backend/   Fastify service (TypeScript) — Gemini + Google Tasks (PRD stages 2–3)
docs/      PRD and research
```

Current state: PRD stages 2 and 3. The watch sends the dictated text to `POST /v1/notes`; the backend
structures it with Gemini (title, cleaned text, subtasks, due date) and creates the task with **your** refresh token.
Account linking and PostgreSQL (stage 4) are not built yet; watch and backend share a fixed dev token.

## Running `wear/`

The Gradle build lives at the repo root (`settings.gradle.kts`, `gradlew`); the app is the `:wear` module.

1. In Android Studio: **File → Open** and select the repo root.
2. Create a Wear OS emulator (API 30+). The PRD asks to test a small round one (192 dp) and a large one (≥ 225 dp).
3. Run the `wear` configuration.

From the terminal:

```sh
./gradlew :wear:testDebugUnitTest   # unit tests
./gradlew :wear:installDebug        # install on the connected emulator/watch
```

### Backend settings (`local.properties`)

```properties
voicenoteai.devApiToken=<same value as DEV_API_TOKEN in backend/.env>
# optional, default http://10.0.2.2:3000 (your PC as seen from the emulator)
voicenoteai.backendUrl=http://10.0.2.2:3000
```

Without `voicenoteai.devApiToken` nothing is sent: ÉXITO shows the recognized text as the title, as in stage 1.
Debug builds allow plain HTTP to `10.0.2.2` only (`wear/src/debug`); release requires HTTPS.

## Running `backend/`

Requires Node 22+.

```sh
cd backend
npm install
cp .env.example .env    # then fill it in — see the two sections below
npm run refresh-token   # once: prints GOOGLE_REFRESH_TOKEN for .env
npm run dev             # http://localhost:3000
npm test                # Vitest, no network
```


