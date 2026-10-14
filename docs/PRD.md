# PRD — Aplicación de smartwatch para captura de notas por voz

**Nombre de trabajo:** VozNota *(PENDIENTE DE DEFINIR)*
**Versión del documento:** 2.2
**Fecha:** 3 de octubre de 2026
**Autor:** Anderson — Aprendiz SENA, ficha 3336175
**Estado:** Definición de producto cerrada. Listo para implementación.

---

## 0. Control de cambios

### 0.1 Respecto a la propuesta original

Dos desviaciones del planteamiento inicial, ambas derivadas de hallazgos documentados en la fase de investigación, no de preferencias de diseño.

| # | Planteamiento original | Cambio aplicado | Causa |
|---|---|---|---|
| 1 | La nota se guarda en **Google Keep** | La nota se guarda en **Google Tasks** | La API de Google Keep está restringida a administradores de Google Workspace mediante cuenta de servicio con delegación de dominio. No existe flujo OAuth de usuario final; una cuenta personal no puede usarla. |
| 2 | El audio se procesa con un servicio de IA para convertirlo en texto | El **reconocedor de voz del dispositivo** produce el texto mediante `SpeechRecognizer`; la IA se encarga **solo de estructurarlo** | Wear OS expone reconocimiento de voz en el dispositivo. Elimina la grabación, el almacenamiento y la transmisión de audio, con beneficios en latencia, batería, costo y privacidad. |

**Consecuencia del cambio 2:** la aplicación **nunca graba, almacena ni transmite audio**. Esto simplifica radicalmente los requisitos de privacidad y es un punto fuerte del diseño, no una limitación.

### 0.2 Cambios de la versión 1.0 a la 2.0

| # | Qué cambió | Por qué |
|---|---|---|
| 1 | `RecognizerIntent` → **`SpeechRecognizer` + `RecognitionListener`** | `RecognizerIntent` lanza la pantalla de escucha del sistema, que no se puede personalizar y rompería el diseño propio. `SpeechRecognizer` mantiene la interfaz de la app durante todo el flujo y expone `onRmsChanged()`, que alimenta el anillo de nivel de voz con amplitud real. Resuelve el riesgo R5 de la v1.0. |
| 2 | Duración máxima fijada en **60 s como guarda no destructiva** | Ver sección 4.3. Deja de ser un valor pendiente. |
| 3 | Idioma fijado en **`es-CO`** | Decisión del autor. |
| 4 | Infraestructura fijada: **Render (web service) + Render PostgreSQL** | Decisión del autor. Obliga a dos ajustes técnicos, secciones 3.5 y 8.1. |
| 5 | Vinculación definida: **código en el reloj + navegador** | Ver sección 3.4. Única opción que funciona igual en emulador y en reloj físico, sin teléfono emparejado y sin teclear en el reloj. |
| 6 | Añadido el manejo del **arranque en frío** de Render | El plan gratuito suspende el servicio tras 15 min sin tráfico y tarda ~1 min en volver. Contradecía los RNF-03 y RNF-04 de la v1.0. |
| 7 | Añadido requisito de **persistencia en base de datos gestionada** | El sistema de archivos de Render es efímero: un SQLite local perdería el refresh token en cada suspensión. |

### 0.3 Cambios de la versión 2.0 a la 2.1

| # | Qué cambió | Por qué |
|---|---|---|
| 1 | Framework del backend fijado en **Fastify**; stack completo documentado en 2.1 y 2.2 | Decisión del autor. El lenguaje y el framework del backend no estaban definidos en la v2.0 — era un hueco del documento. |
| 2 | `EncryptedSharedPreferences` → **Android Keystore directo** | `androidx.security:security-crypto` deprecó **todas** sus APIs en la versión 1.1.0-beta01 (junio de 2025), textualmente *"in favour of existing platform APIs and direct use of Android Keystore"*. |
| 3 | Artefacto de UI fijado en `androidx.wear.compose:compose-material3:1.5.0` | El informe de investigación listaba `compose-material`, hoy obsoleto. La documentación advierte expresamente de no mezclar ambas librerías. |
| 4 | Añadido el riesgo de **caducidad del refresh token a los 7 días** en modo Testing | Rompería la vinculación cada semana y haría fallar el CA-09 de forma intermitente y difícil de diagnosticar. |
| 5 | Decidido **un solo repositorio** con `wear/`, `backend/` y `docs/` — sección 2.3 | Desarrollador único y un contrato de API que cambia a lo largo de varias etapas. Render soporta *Root Directory* y *build filters*, así que el único argumento técnico para separar desaparece. |

### 0.4 Cambios de la versión 2.1 a la 2.2

| # | Qué cambió | Por qué |
|---|---|---|
| 1 | `targetSdk`/`compileSdk` **37** y `compose-material3` **1.5.6** | Son los valores de la plantilla de Android Studio con que arrancó el proyecto. 1.5.6 es un parche de la misma línea 1.5; un `targetSdk` más reciente no cambia nada del diseño. |
| 2 | `compose-navigation` **fuera** del stack del reloj | Hay una sola pantalla con una máquina de estados (§7.5). Se añadirá solo si aparece una segunda pantalla. |
| 3 | La build de Gradle vive en la **raíz** del repositorio; `wear/` es el módulo `:wear` — sección 2.3 | Permite abrir la raíz en Android Studio y ejecutar la app sin cambiar de carpeta. El despliegue del backend no se ve afectado: Render usa *Root Directory* = `backend`. |
| 4 | Reconocedor: **solo `createSpeechRecognizer`**, sin la rama de `createOnDeviceSpeechRecognizer` — sección 3.3 | La rama en el dispositivo resolvía R4, aún no validado, a cambio de un fallo confirmado: los modelos locales pueden no incluir `es-CO` y darían «Idioma no disponible» donde el reconocedor estándar funciona. El estándar ya usa modelos locales cuando existen. |
| 5 | FA-04: la cancelación **descarta y cierra la app**, en lugar de volver a INICIAL — secciones 4.1, 5 y 7.5 | En Wear OS el gesto de retroceso es deslizar para cerrar y el usuario espera que saque de la app; retenerlo en INICIAL contradice la plataforma. La captura se cancela en `onStop()`, que cubre también el botón de encendido y las notificaciones. |
| 6 | Nuevo caso FA-21: **reconocedor no disponible**, distinto de FA-07 — sección 5 | Es un fallo de configuración del dispositivo (típicamente una imagen de emulador sin servicios de Google), no de captura. Mostrarlo como «No pude entender» llevaría a depurar el código equivocado. |

---

## 1. Resumen del producto

### 1.1 Propuesta de valor en una frase

> Presionar un botón en el reloj, hablar, y que la idea quede guardada —limpia y con título— en la cuenta de Google del usuario, sin tocar el teléfono.

### 1.2 Problema

Las ideas aparecen en momentos en que sacar el teléfono es inconveniente o imposible: caminando, conduciendo, en una reunión, haciendo ejercicio, cocinando. El costo de capturarlas —desbloquear, buscar la app, abrir una nota, escribir— es tan alto que la mayoría se pierden.

### 1.3 Solución

Una aplicación de Wear OS de **una sola función** y **un solo control**. El reloj ya está en la muñeca, siempre accesible. Dos toques y hablar.

### 1.4 Diferenciador frente a lo que ya existe

Google Keep ya ofrece una app de Wear OS con captura de nota por voz. **La diferencia de este producto es el procesamiento por IA**: la nota no se guarda como se dijo, sino limpia —sin muletillas, con puntuación, con un título generado— y convertida en lista de tareas cuando el contenido lo amerita. Eso es lo que justifica que el producto exista.

### 1.5 No objetivos (restricciones duras)

Esta aplicación **no** es ni será: un asistente virtual, una aplicación de notas con biblioteca o búsqueda, una aplicación de mensajería, una grabadora de audio, una suite con múltiples herramientas, un sistema con cuentas propias, ni una aplicación con funciones sociales.

Y **no** tendrá: menús, configuración, listado de notas creadas, edición posterior, etiquetas, recordatorios, adjuntos ni modo multiusuario.

---

## 2. Decisiones técnicas

Todas confirmadas salvo donde se indica.

| Decisión | Valor | Estado |
|---|---|---|
| Plataforma | Wear OS 3 o superior | ✅ |
| `minSdk` / `targetSdk` / `compileSdk` | 30 / 37 / 37 | ✅ |
| Lenguaje | Kotlin | ✅ |
| UI | Compose for Wear OS (Material 3 Expressive) | ✅ |
| Transcripción | `SpeechRecognizer` + `RecognitionListener`, en el dispositivo | ✅ |
| Idioma de reconocimiento | `es-CO` | ✅ |
| Duración máxima de captura | 60 s, como guarda no destructiva | ✅ |
| Procesamiento IA | LLM en backend (estructuración del texto) | ✅ |
| Destino de la nota | Google Tasks API, scope `auth/tasks` | ✅ |
| Autenticación | Cuenta de Google, exclusivamente | ✅ |
| Vinculación | Código en el reloj + navegador (sección 3.4) | ✅ |
| Arquitectura | Reloj + Backend (Opción C) | ✅ |
| Hosting del backend | Render — web service | ✅ |
| Persistencia | Render PostgreSQL | ✅ |
| Entorno de desarrollo | Emulador Wear OS en Android Studio | ✅ |
| Alcance | Formativo, sin publicación en Play Store | ✅ |
| Modelo de LLM concreto | `gemini-3.5-flash-lite` (configurable con `GEMINI_MODEL`); la serie 2.5 ya no está abierta a proyectos nuevos | ⏳ PENDIENTE DE VALIDAR — calidad y costo por nota |

### 2.1 Stack del reloj

| Capa | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | `androidx.wear.compose:compose-material3:1.5.6`, más `compose-foundation` y `compose-ui-tooling` en la misma versión. Sin `compose-navigation` mientras haya una sola pantalla. |
| Reconocimiento de voz | `android.speech.SpeechRecognizer` + `RecognitionListener` — API de plataforma, sin dependencia externa |
| Asincronía | Kotlin Coroutines + Flow |
| Estado de UI | `ViewModel` con `androidx.lifecycle:lifecycle-viewmodel-compose` |
| Cliente HTTP | Ktor Client + kotlinx.serialization |
| Almacenamiento del token | Android Keystore directo — ver RNF-14 |
| Inyección de dependencias | **Ninguna.** Contenedor manual |
| Build | Gradle con *version catalog* (`libs.versions.toml`), R8 activado |
| Pruebas | JUnit, Turbine, `createComposeRule`, `@WearPreviewDevices` |

**No mezclar** `compose-material3` con `androidx.wear.compose:compose-material` (obsoleto) ni con `androidx.compose.material:material`. La documentación de Wear Compose Material 3 lo advierte de forma explícita.

Sobre la ausencia de framework de inyección de dependencias: para una pantalla con tres casos de uso, Hilt añade peso al APK y tiempo de compilación sin resolver ningún problema real. El objetivo del RNF-11 es un APK por debajo de 8 MB.

### 2.2 Stack del backend

| Capa | Tecnología |
|---|---|
| Runtime | Node.js 22 LTS |
| Lenguaje | TypeScript |
| Framework HTTP | **Fastify** |
| Validación de entrada | JSON Schema nativo de Fastify |
| Documentación de la API | `@fastify/swagger` — OpenAPI generado desde los esquemas de las rutas |
| ORM y migraciones | Prisma |
| Base de datos | PostgreSQL (Render) |
| Cliente de Google | `googleapis`, el cliente oficial de Node — intercambio OAuth y Tasks API |
| LLM | SDK oficial de Gemini para Node |
| Cifrado en reposo | `node:crypto`, AES-256-GCM, clave en variable de entorno |
| Página de vinculación | HTML servido directo, sin framework de plantillas. Es un formulario con un campo. |
| Pruebas | Vitest + `fastify.inject()` |

**Por qué Node y no Kotlin en el backend,** aunque unificar el lenguaje sería atractivo: una JVM en el plan gratuito de Render agrava justo el arranque en frío que la sección 3.5 mitiga. La diferencia entre frameworks de Node es marginal porque lo que domina es el arranque del contenedor, no el del proceso.

**Variables de entorno:** `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GEMINI_API_KEY`, `TOKEN_ENC_KEY`, `DATABASE_URL`.

### 2.3 Repositorio

**Un solo repositorio.** La razón principal para separar repos es que equipos distintos tengan ciclos de despliegue independientes; con un desarrollador único ese beneficio no existe y sí se pagan sus costos.

```
voznota/
├── settings.gradle.kts, build.gradle.kts, gradlew, gradle/   build de Gradle
├── wear/          app Android — módulo Gradle :wear
├── backend/       servicio Fastify (npm)
├── docs/
│   ├── PRD.md
│   └── investigacion.md
├── .gitignore     cubre Android y Node
└── README.md      cómo arrancar ambas partes
```

La build de Gradle está en la raíz para que Android Studio abra el repositorio completo y ejecute la app sin cambiar de carpeta. Gradle solo conoce el módulo `:wear`; `backend/` y `docs/` le son indiferentes.

| Aspecto | Decisión |
|---|---|
| Despliegue | En Render, *Root Directory* = `backend`. Render solo despliega si los cambios afectan a ese directorio. |
| Build filters | Ignorar `wear/**` y `docs/**`, para que tocar la app o la documentación no redespliegue el servidor. |
| Herramienta de monorepo | **Ninguna.** No hay paquetes JavaScript compartidos entre las partes, porque el reloj es Kotlin. Son dos cadenas de herramientas independientes en un mismo árbol. |
| Contrato de la API | No se puede compartir como tipos entre Kotlin y TypeScript. La fuente única de verdad es el OpenAPI que genera `@fastify/swagger`; los modelos de kotlinx.serialization del reloj se escriben a mano contra él. |
| Secretos | `.env` nunca se versiona. |

**Justificación del repositorio único:** el contrato entre reloj y backend cambia a lo largo de las etapas 2 a 4 del plan de implementación (sección 12). En un repositorio, añadir un campo a una respuesta y consumirlo en el reloj es un solo commit; en dos repositorios son dos commits que hay que mantener sincronizados manualmente. Además, siendo un entregable académico, un único repositorio con su README y su historial cuenta la historia completa del proyecto.

---

## 3. Arquitectura

### 3.1 Diagrama de componentes

```
┌─────────────────────── RELOJ (Wear OS) ───────────────────────┐
│                                                               │
│  Capa de presentación — Compose for Wear OS                   │
│    └─ Una sola pantalla, cinco estados + pantalla de          │
│       vinculación (solo primer uso)                           │
│                                                               │
│  Capa de dominio                                              │
│    ├─ CapturarVozUseCase   → SpeechRecognizer                 │
│    ├─ GuardarNotaUseCase   → NotaRepository                   │
│    └─ VincularUseCase      → VinculacionRepository            │
│                                                               │
│  Capa de datos                                                │
│    ├─ NotaRepository       → cliente HTTPS (Ktor/Retrofit)    │
│    └─ SesionRepository     → Android Keystore                 │
│                                                               │
└───────────────────────────────┬───────────────────────────────┘
                                │ HTTPS
                                │ POST /v1/notas  { texto }
                                │ Authorization: Bearer <token de sesión>
                                ▼
┌──────────────── BACKEND — Render (web service) ───────────────┐
│                                                               │
│  Rutas de la app                                              │
│    POST /v1/notas              crear la nota                  │
│    GET  /v1/salud              ping de calentamiento          │
│    POST /v1/sesion/revocar     cerrar sesión                  │
│                                                               │
│  Rutas de vinculación                                         │
│    POST /v1/vinculacion        el reloj pide un código        │
│    POST /v1/vinculacion/token  polling del reloj              │
│    GET  /v                     página web: pedir el código    │
│    POST /v                     valida y redirige a Google     │
│    GET  /auth/google/callback  recibe el código OAuth         │
│                                                               │
│  Secretos en variables de entorno de Render:                  │
│    GEMINI_API_KEY, GOOGLE_CLIENT_SECRET, TOKEN_ENC_KEY        │
│    Nunca en el cliente.                                       │
│                                                               │
└───────┬───────────────────────┬───────────────────────┬───────┘
        │                       │                       │
        ▼                       ▼                       ▼
┌───────────────┐     ┌──────────────────┐   ┌──────────────────┐
│   Render      │     │   API del LLM    │   │ Google Tasks API │
│  PostgreSQL   │     │    (Gemini)      │   │ scope auth/tasks │
└───────────────┘     └──────────────────┘   └──────────────────┘
```

### 3.2 Por qué el backend no es opcional

Dos razones independientes; cualquiera bastaría.

1. **Restricción de scopes OAuth.** El flujo OAuth 2.0 de Google para dispositivos de entrada limitada —el único que permitiría a un reloj autorizarse solo— soporta únicamente `email`, `openid`, `profile`, `drive.appdata`, `drive.file`, `youtube` y `youtube.readonly`. **`auth/tasks` no está en esa lista.**
2. **Protección de secretos.** La clave de la API del LLM y el `client_secret` de OAuth son extraíbles de un APK con herramientas triviales.

### 3.3 Captura de voz

Se usa `SpeechRecognizer` directamente, no `RecognizerIntent`, para conservar la interfaz propia durante toda la escucha.

**Construcción y ciclo de vida:**

- `SpeechRecognizer.createSpeechRecognizer(context)`; se crea al entrar en la pantalla y se libera con `destroy()` en `onDestroy`.
- Si `SpeechRecognizer.isRecognitionAvailable(context)` es `false`, no se crea: el dispositivo no tiene servicio de reconocimiento y se muestra FA-21.
- **Respaldo con `RecognizerIntent` (añadido tras la primera prueba).** Los emuladores de Wear OS, incluso la imagen Wear OS 6 / API 36 con servicios de Google, no traen ningún `RecognitionService`; solo exponen la pantalla de dictado de Gboard (`ACTION_RECOGNIZE_SPEECH`). Si no hay reconocedor pero sí esa actividad, se lanza el dictado del sistema. El respaldo pierde el anillo con amplitud real y los parciales, así que no sirve para medir R2/R7: esas mediciones exigen un reloj físico con `RecognitionService`. FA-21 solo aparece si no existe ninguna de las dos vías.
- **Todas las llamadas al reconocedor deben hacerse desde el hilo principal.** Es la causa más común de fallos silenciosos con esta API.
- Un único `RecognitionListener` traduce callbacks a estados de UI.

**Intent de reconocimiento:**

```kotlin
val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
             RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CO")
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
}
```

**Mapa de callbacks a estados de interfaz:**

| Callback | Efecto en la UI |
|---|---|
| `onReadyForSpeech` | Entra en estado GRABANDO, pulso háptico |
| `onBeginningOfSpeech` | El anillo pasa a su estado activo |
| `onRmsChanged(rmsdB)` | **Alimenta el anillo de nivel de voz con amplitud real** |
| `onPartialResults` | No se muestra texto; solo confirma que hay reconocimiento en curso |
| `onEndOfSpeech` | Entra en estado PROCESANDO |
| `onResults` | Extrae el texto y lo envía al backend |
| `onError` | Entra en estado ERROR con el mensaje correspondiente |

**Reconocedor estándar, sin rama en el dispositivo.** Se usa únicamente el reconocedor estándar:

```kotlin
val reconocedor = SpeechRecognizer.createSpeechRecognizer(context)
```

No se usa `createOnDeviceSpeechRecognizer` (API 31+) aunque `isOnDeviceRecognitionAvailable` sea `true`. Esa rama resolvería R4 —reconocimiento sin conexión—, que todavía no está validado, y a cambio introduce un fallo confirmado: los modelos locales pueden no incluir `es-CO`, y el usuario vería «Idioma no disponible» en un reloj donde el reconocedor estándar habría funcionado. El estándar ya emplea modelos en el dispositivo cuando los tiene, así que no se pierde nada real.

**Si la prueba en modo avión (R4) demuestra que el reconocimiento sin conexión importa,** se añadirá entonces una cadena de respaldo: intentar el reconocedor en el dispositivo y, ante `ERROR_LANGUAGE_NOT_SUPPORTED` o `ERROR_LANGUAGE_UNAVAILABLE`, reintentar una sola vez con el estándar y recordar la elección para las siguientes capturas.

Los extras de silencio (`EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS` y relacionados) existen, pero es el servicio de reconocimiento quien decide si aplicarlos. **No se debe depender de ellos.** El fin del habla se detecta con `onEndOfSpeech()`.

### 3.4 Vinculación de cuenta (primer uso)

Es un *device authorization grant* (la forma del RFC 8628) implementado entre el reloj y nuestro propio backend. No se inventa protocolo: se reimplementa un estándar contra un servidor propio, porque el flujo de dispositivo de Google no admite el scope que se necesita.

Funciona idéntico en el emulador y en un reloj físico, no requiere teléfono emparejado y no se teclea nada en el reloj.

**Secuencia:**

```
 1. Reloj  → POST /v1/vinculacion
 2. Backend → { user_code: "A7K-3M9",
                verification_url: "voznota.onrender.com/v",
                device_code: "<32 bytes aleatorios>",
                interval: 5, expires_in: 600 }
 3. Reloj  muestra user_code y verification_url
 4. Reloj  → POST /v1/vinculacion/token { device_code }  cada 5 s
 5. Usuario abre la URL en cualquier navegador
 6. Usuario escribe el código → POST /v
 7. Backend valida y redirige al consentimiento de Google
 8. Usuario aprueba el acceso a sus tareas
 9. Google → GET /auth/google/callback?code=…&state=…
10. Backend intercambia el código por access + refresh token,
    crea o actualiza el usuario y marca la vinculación aprobada
11. El siguiente polling del reloj devuelve { session_token }
12. El reloj lo guarda cifrado. No se repite.
```

**Parámetros de la autorización de Google:**

```
client_id      = <cliente de tipo "Aplicación web">
redirect_uri   = https://<servicio>.onrender.com/auth/google/callback
response_type  = code
scope          = openid email https://www.googleapis.com/auth/tasks
access_type    = offline
prompt         = consent
state          = HMAC(user_code + nonce)
```

`access_type=offline` junto con `prompt=consent` es lo que garantiza que Google devuelva un refresh token.

**El cliente OAuth debe ser de tipo «Aplicación web», no Android.** El `redirect_uri` apunta a la URL de Render y el intercambio del código lo ejecuta el backend, no el reloj. Elegir el tipo Android aquí es un error fácil de cometer y difícil de diagnosticar.

**Caducidad del refresh token en modo Testing.** Con la pantalla de consentimiento en estado «Testing», Google emite refresh tokens con una vida de 7 días. En la práctica eso significa que la vinculación se rompe cada semana durante el desarrollo y que el CA-09 falla de forma intermitente. Dos salidas: aceptarlo y documentarlo —revincular semanalmente es tolerable en un proyecto formativo— o pasar la app a estado «Production». Lo que no es aceptable es descubrirlo durante una sustentación. Ver riesgo R8. *(Dato pendiente de confirmar en la documentación oficial de Google: proviene de fuentes secundarias coincidentes.)*

**Esquema de datos:**

```sql
usuarios
  id             uuid primary key
  google_sub     text unique not null   -- 'sub' del ID token
  email          text
  refresh_token  bytea not null         -- cifrado con TOKEN_ENC_KEY
  creado_en      timestamptz not null

vinculaciones                            -- efímeras
  device_code    text primary key        -- el secreto que tiene el reloj
  user_code      text unique not null    -- el que ve y escribe el usuario
  estado         text not null           -- pendiente | aprobada | expirada
  usuario_id     uuid references usuarios(id)
  intentos       int not null default 0
  expira_en      timestamptz not null

sesiones
  token_hash     text primary key        -- hash, nunca el token en claro
  usuario_id     uuid not null references usuarios(id)
  creada_en      timestamptz not null
  ultimo_uso     timestamptz
```

**Reglas de seguridad de la vinculación:**

| # | Regla |
|---|---|
| V-01 | `user_code` de 6 caracteres en formato `XXX-XXX`, de un alfabeto sin caracteres ambiguos (sin `O`, `0`, `I`, `1`, `L`). |
| V-02 | `user_code` expira a los 10 minutos y es de un solo uso. |
| V-03 | Máximo 5 intentos por `user_code`; al sexto se invalida. |
| V-04 | El `device_code` es el secreto del reloj y nunca se muestra en pantalla. |
| V-05 | El parámetro `state` va firmado con HMAC para prevenir CSRF. |
| V-06 | El refresh token se cifra en reposo con una clave de variable de entorno. |
| V-07 | Del token de sesión solo se almacena su hash. |
| V-08 | El polling respeta el `interval`; ante respuesta de ralentización aplica retroceso exponencial. |

**Alternativa para reloj físico emparejado:** OAuth 2.0 con PKCE mediante `RemoteAuthClient`, que abre el consentimiento en el navegador del teléfono. Mejor experiencia en ese escenario, pero inviable en un emulador aislado. Queda como mejora opcional, no como parte de la v1.

### 3.5 Arranque en frío de Render

El plan gratuito de Render suspende un web service tras **15 minutos sin tráfico entrante**, y volver a levantarlo tarda **alrededor de un minuto**. Sin tratamiento, la primera nota después de un rato de inactividad fallaría siempre.

**Tres medidas, en orden de efecto:**

1. **Ping de calentamiento.** Al entrar en el estado INICIAL, el reloj dispara un `GET /v1/salud` sin esperar respuesta. El servidor despierta durante el tiempo que el usuario tarda en decidirse y presionar el botón —tiempo que de otro modo se desperdicia—, así que en la mayoría de los casos el arranque en frío queda oculto detrás del propio flujo.
2. **Timeout único de 90 s** para `POST /v1/notas`, en lugar de 15 s. Una sola constante, sin ramas que detecten si el servidor está frío.
3. **Texto escalonado en PROCESANDO**, según el tiempo transcurrido: «Procesando» → a los 5 s «Casi listo…» → a los 10 s «Despertando el servidor». El usuario entiende qué ocurre sin que la app mienta.

En condiciones normales —servidor caliente— la petición termina en menos de 4 s y el usuario nunca ve las etapas 2 y 3.

**El sistema de archivos de Render es efímero:** los cambios se pierden en cada redespliegue, reinicio o suspensión. Por eso la persistencia va en **Render PostgreSQL** y no en un SQLite local: con un archivo local, el usuario quedaría desvinculado cada 15 minutos de inactividad.

---

## 4. Requisito funcional

### RF-001 — Crear una nota mediante voz

**Prioridad:** Crítica. Es el único requisito funcional del producto.

**Descripción:**
El usuario debe poder presionar el icono de grabación del smartwatch, hablar, y permitir que el sistema convierta su voz en texto, lo procese mediante Inteligencia Artificial para estructurarlo, y lo envíe a su cuenta de Google como una nueva entrada, recibiendo confirmación visual del resultado.

**Actor:** Usuario del smartwatch, con su cuenta de Google ya vinculada.

**Precondiciones:**
- La aplicación está instalada en un dispositivo Wear OS 3 o superior
- La vinculación con la cuenta de Google está completada
- El permiso `RECORD_AUDIO` ha sido concedido
- Existe conectividad a Internet

**Flujo principal:**

| # | Actor | Acción |
|---|---|---|
| 1 | Usuario | Abre la aplicación |
| 2 | Sistema | Muestra el icono de grabación (estado INICIAL) y dispara el ping de calentamiento |
| 3 | Usuario | Presiona el icono |
| 4 | Sistema | Verifica el permiso de micrófono; lo solicita si no está concedido |
| 5 | Sistema | Llama a `startListening()`; en `onReadyForSpeech` pasa a GRABANDO con pulso háptico |
| 6 | Usuario | Habla |
| 7 | Sistema | El anillo refleja el nivel de voz vía `onRmsChanged` |
| 8 | Sistema | Detecta el fin del habla (`onEndOfSpeech`), o el usuario toca detener, o se cumple la guarda de 60 s |
| 9 | Sistema | Pasa a PROCESANDO y recoge el texto de `onResults` |
| 10 | Sistema | Envía el texto al backend |
| 11 | Backend | Procesa el texto con el LLM: genera título, limpia y estructura el contenido |
| 12 | Backend | Crea la entrada en Google Tasks con el token del usuario |
| 13 | Backend | Responde con confirmación y el título generado |
| 14 | Sistema | Muestra ÉXITO con el título y doble pulso háptico |
| 15 | Sistema | Vuelve automáticamente a INICIAL tras 3 segundos |

**Postcondiciones:**
- Existe una nueva entrada en la cuenta de Google Tasks del usuario
- No queda audio almacenado en ningún lugar, porque nunca se grabó
- La aplicación está lista para una nueva captura

### 4.1 Los tres finales de la captura

Ninguno destruye lo capturado. Es lo que hace que el límite de duración no genere deuda.

| Final | Disparador | Llamada | Resultado |
|---|---|---|---|
| **Normal** | El reconocedor detecta el fin del habla | — (`onEndOfSpeech`) | Entrega el texto |
| **Manual** | El usuario toca el botón de detener | `stopListening()` | Entrega el texto |
| **Guarda** | Se cumplen 60 s de captura | `stopListening()` | **Entrega el texto oído hasta ese momento** |

La cancelación es el único camino que usa `cancel()` y descarta. Se dispara en `onStop()` de la actividad, que cubre el gesto de retroceso (deslizar para cerrar) y cualquier interrupción que saque la app de primer plano —botón de encendido, notificación—. La app se cierra; no vuelve a INICIAL (FA-04). El gesto de retroceso no se intercepta: en Wear OS el usuario espera que lo saque de la app.

### 4.2 Por qué `stopListening()` y no `cancel()` en la guarda

`stopListening()` indica al reconocedor que deje de escuchar y procese lo que lleva; `cancel()` aborta y descarta. Usar el primero en la guarda significa que alcanzar el límite **produce una nota igual**, no un error. Por eso no existe una rama de código para «¿qué hago si se cortó?» ni un mensaje que el usuario deba entender.

### 4.3 Justificación de los 60 segundos

El límite es una **guarda**, no el mecanismo de fin de captura. El fin normal lo decide el reconocedor.

- 60 s equivalen a unas 150 palabras dictadas: muy por encima de cualquier «idea rápida».
- Está por debajo del punto en que haría falta trocear o transmitir en flujo, así que no es un techo que la arquitectura obligue a subir después.
- Alcanzarlo no destruye nada (sección 4.2), así que no introduce casos especiales.
- Es una sola constante en un solo lugar.

**Riesgo asociado, a validar pronto (R2 en la sección 11):** el reconocedor del sistema está afinado para frases cortas y en la práctica puede cerrar la sesión bastante antes de los 60 s. Si en pruebas corta a los 10–15 s de habla continua, la guarda es decorativa y aparece la pregunta real: reiniciar la sesión en `onEndOfSpeech` y concatenar fragmentos. **Esa sí sería deuda arquitectónica**, y es la razón para medirlo con un dictado largo en la primera semana de desarrollo, antes de construir la UI sobre el supuesto contrario.

---

## 5. Flujos alternativos y manejo de errores

Ninguno de estos casos es una funcionalidad nueva. Son condiciones que el flujo único debe manejar.

| ID | Condición | Comportamiento del sistema | Mensaje | ¿Reintentar? |
|---|---|---|---|---|
| FA-01 | Permiso de micrófono no concedido | Solicita el permiso en tiempo de ejecución | *(UI del sistema)* | — |
| FA-02 | Permiso denegado permanentemente | ERROR, ofrece abrir ajustes | «Necesito el micrófono» | Abrir ajustes |
| FA-03 | Micrófono ocupado por otra app | ERROR | «Micrófono ocupado» | Sí |
| FA-04 | Usuario cancela (gesto de retroceso) o la app sale de primer plano durante la captura | `cancel()` en `onStop()`, descarta todo y la app se cierra | *(ninguno)* | — |
| FA-05 | No se detecta voz (`ERROR_SPEECH_TIMEOUT`) | No crea nota. ERROR | «No escuché nada» | Sí |
| FA-06 | Sin coincidencias (`ERROR_NO_MATCH`) o texto vacío | No crea nota. ERROR | «No escuché nada» | Sí |
| FA-07 | Error del reconocedor (`ERROR_CLIENT`, `ERROR_RECOGNIZER_BUSY`) | ERROR | «No pude entender» | Sí |
| FA-08 | Idioma no disponible (`ERROR_LANGUAGE_NOT_SUPPORTED`, `ERROR_LANGUAGE_UNAVAILABLE`) | ERROR | «Idioma no disponible» | — |
| FA-21 | Reconocedor no disponible: `isRecognitionAvailable` devuelve `false` (típico de una imagen de emulador sin servicios de Google). Distinto de FA-07: es un fallo de configuración del dispositivo, no de captura | ERROR, y se registra en el log `VozNota.Config` | «Reconocedor no disponible» | — |
| FA-09 | Sin conexión | ERROR, **conserva el texto en memoria** | «Sin conexión» | Sí |
| FA-10 | Servidor suspendido (arranque en frío) | PROCESANDO con texto escalonado, timeout 90 s | «Despertando el servidor» | — |
| FA-11 | Error del servicio de IA (5xx o timeout) | **Degradación: envía el texto sin estructurar** | *(ninguno — éxito)* | — |
| FA-12 | Access token de Google expirado | El backend lo renueva de forma transparente | *(ninguno)* | — |
| FA-13 | Refresh token inválido o acceso revocado | ERROR, requiere nueva vinculación | «Vuelve a conectar tu cuenta» | Revincular |
| FA-14 | Usuario no vinculado | Muestra la pantalla de vinculación | *(flujo de 3.4)* | — |
| FA-15 | Código de vinculación expirado | Solicita uno nuevo automáticamente | «Código vencido» | Sí |
| FA-16 | Error de Google Tasks API | ERROR, conserva el texto | «No se pudo guardar» | Sí |
| FA-17 | Timeout general (> 90 s) | Cancela, ERROR, conserva el texto | «Tardó demasiado» | Sí |
| FA-18 | Batería por debajo del 5 % | Bloquea el inicio de la captura | «Batería muy baja» | — |
| FA-19 | App cerrada durante el proceso | Si ya se envió al backend, la nota se crea igual. Si no, se pierde | *(ninguno)* | — |
| FA-20 | Guarda de 60 s alcanzada | `stopListening()`, procesa lo capturado | *(transición a PROCESANDO)* | — |

**Regla transversal:** ante cualquier error posterior al reconocimiento, el texto **se conserva en memoria** para que el reintento no obligue al usuario a repetir lo que dijo. Hacer que alguien repita una idea que ya dictó es el peor fallo posible en un producto cuya promesa es «captura sin fricción».

---

## 6. Reglas de negocio

| ID | Regla |
|---|---|
| RN-01 | La aplicación nunca graba, almacena ni transmite audio. Solo texto. |
| RN-02 | Nunca se crea una entrada vacía o sin contenido significativo. |
| RN-03 | La autenticación es exclusivamente con cuenta de Google. No hay otro método. |
| RN-04 | Los secretos residen únicamente en variables de entorno del backend. |
| RN-05 | El token de sesión se almacena cifrado en el reloj con una clave AES-256-GCM del Android Keystore. |
| RN-06 | Si el servicio de IA falla, se guarda la transcripción cruda antes que no guardar nada. |
| RN-07 | El texto transcrito no se persiste en el backend tras crear la entrada. |
| RN-08 | Una sesión de captura produce exactamente una entrada, nunca más de una. |
| RN-09 | El usuario puede revocar el acceso desde su cuenta de Google en cualquier momento; la app debe degradar correctamente. |
| RN-10 | Cerrar sesión borra los tokens locales y llama al endpoint de revocación de Google. |
| RN-11 | Alcanzar la guarda de duración produce una nota, nunca un error. |
| RN-12 | Ningún estado persistente vive en el sistema de archivos del backend. |

---

## 7. Diseño de interfaz y experiencia de usuario

> Esta es la sección central del proyecto. El producto tiene una sola función; por lo tanto, **el producto es su interfaz**.

### 7.1 Principios rectores

Google establece el principio fundamental del diseño en Wear OS: enfocar las aplicaciones en tareas críticas para que la persona resuelva lo suyo **en segundos**, evitando incomodidad ergonómica o fatiga del brazo.

| # | Principio | Aplicación concreta |
|---|---|---|
| 1 | **Un solo objetivo táctil** | Nada compite por la atención. El botón ocupa el centro óptico de la pantalla. |
| 2 | **Estado legible de un vistazo** | El estado se comunica por color y forma antes que por texto. El texto confirma, no informa. |
| 3 | **Cero navegación** | No hay pantallas secundarias, menús ni diálogos modales. |
| 4 | **Háptica como canal principal** | En el reloj, la vibración es más fiable que la vista: el usuario puede no estar mirando. |
| 5 | **Recuperación sin castigo** | Un error nunca obliga a repetir el dictado. |

### 7.2 Fundamentos técnicos de diseño

Valores de las guías oficiales de Wear OS. No son negociables.

| Parámetro | Valor |
|---|---|
| **Área táctil mínima** | **48 × 48 dp** |
| **Pantalla base de diseño** | **192 dp redonda**, la más pequeña soportada |
| **Breakpoint de pantalla grande** | **225 dp** |
| **Rangos de pantalla** | 192–224 dp (pequeña) · 225–240+ dp (grande) |
| **Márgenes exteriores** | En porcentaje, nunca absolutos · mínimo 5,2 % en pantallas redondas |
| **Alineación de texto** | Centrado — el texto de esta app es corto |
| **Escalado tipográfico** | Debe tolerar los ajustes de accesibilidad del sistema |

### 7.3 Sistema de diseño

**Material 3 Expressive para Wear OS**, vía `androidx.wear.compose:compose-material3:1.5.6` (ver 2.1), con la estética de felpa de `docs/propuesta_diseño.png`: fondo crema, un parche central de color por estado con el icono bordado y tipografía Lilita One en mayúsculas. El detalle (composición, tokens, recursos) está en la skill `.claude/skills/wear-ui-ux`. El fondo claro sustituye al negro OLED anterior: consume más batería con la pantalla encendida, a cambio de identidad visual.

| Estado | Color del parche | Hex |
|---|---|---|
| INICIAL | Azul | `#2158A2` |
| GRABANDO | Rojo | `#D63A3A` |
| PROCESANDO | Ámbar | `#E09A14` |
| ÉXITO | Verde | `#2E8B57` |
| ERROR | Naranja óxido | `#C2451C` |
| Fondo | Felpa crema | `#D8CCB8` |
| Texto principal | Tinta | `#1B1A17` |
| Texto secundario | Tinta suave | `#4A453C` |
| Iconos sobre parches | Hilo crema | `#F1E8D8` |

**Accesibilidad:** todas las combinaciones de texto sobre fondo superan 4,5:1 (WCAG AA). El color **nunca** es el único portador de información: cada estado tiene además una forma de icono y un patrón háptico distintos.

### 7.4 Especificación de los estados

#### Estado 1 — INICIAL

- Botón circular de 96 dp, centrado, borde azul de 2 dp, icono de micrófono de 40 dp
- Texto «Toca para hablar», 12 sp, gris, a 16 dp bajo el botón
- Al presionar: escala a 0,92 y pulso háptico ligero
- **Al entrar en este estado se dispara el ping de calentamiento** (sección 3.5), sin bloquear la UI ni mostrar nada

#### Estado 2 — GRABANDO

- **Anillo de nivel de voz**: círculo concéntrico que pulsa entre 100 y 140 dp **alimentado por `onRmsChanged(rmsdB)`**. Es el elemento más importante del diseño: confirma al usuario que el sistema realmente lo está oyendo. Con `SpeechRecognizer` directo refleja amplitud real, no una animación simulada.
- Botón central de 75 dp relleno en rojo, icono de detener
- Texto «Escuchando…» en rojo claro
- Arco de progreso fino en el borde exterior, que se completa al llegar a los 60 s
- Pulso háptico corto al iniciar
- `FLAG_KEEP_SCREEN_ON` activo, para que el modo ambiente no interrumpa

> **Nota de implementación:** `rmsdB` llega en decibelios y su rango real varía por dispositivo. Conviene normalizar con un suavizado exponencial y recortar a un rango calibrado en pruebas, en lugar de mapearlo linealmente. Un anillo que salta de forma errática se lee como un fallo.

#### Estado 3 — PROCESANDO

- `CircularProgressIndicator` indeterminado de 72 dp, ámbar, grosor 4 dp
- **Texto escalonado por tiempo transcurrido:**

| Tiempo | Texto |
|---|---|
| 0 s | «Procesando» |
| 5 s | «Casi listo…» |
| 10 s | «Despertando el servidor» |

- Duración esperada con servidor caliente: 1–3 s. El usuario no debería ver las etapas 2 y 3 salvo tras una suspensión.
- No cancelable, para no dejar trabajo a medias en el servidor

#### Estado 4 — ÉXITO

- Check verde de 56 dp con animación de trazo (~300 ms)
- Título generado por la IA, 14 sp, blanco, máximo 2 líneas con elipsis. Mostrarlo es esencial: es la única prueba de que el sistema entendió.
- «Guardado», 12 sp, verde
- Doble pulso háptico corto
- Permanencia de 3 s, luego retorno automático a INICIAL

#### Estado 5 — ERROR

- Icono de advertencia de 48 dp, rojo claro
- Mensaje de **máximo tres palabras**. En una pantalla de reloj un mensaje largo no se lee: se descarta.
- Botón de reintento, altura mínima 48 dp
- Un pulso háptico largo, distinguible del de éxito
- **El reintento reutiliza el texto ya reconocido.** El usuario no vuelve a hablar.

#### Pantalla de vinculación (solo primer uso)

- «Vincula tu cuenta», 11 sp, gris
- **Código de 6 caracteres**, 26 sp, negrita, blanco, con interletraje amplio. Es el elemento dominante: es lo que el usuario debe leer y copiar.
- URL de verificación, 11 sp, azul
- «Abre el enlace e introduce el código», 10 sp, gris, dos líneas
- Sin botones: la pantalla es informativa y el reloj hace polling en segundo plano
- Al aprobarse, transición directa al estado INICIAL con pulso háptico de confirmación

### 7.5 Mapa de navegación

Sin navegación entre pantallas. Una superficie con una máquina de estados.

```
   ┌──────────────┐
   │ VINCULACIÓN  │  (solo si no hay sesión)
   └──────┬───────┘
          │ aprobada
          ▼
                    ┌──────────────┐
          ┌────────►│   INICIAL    │◄─────────┐
          │         └──────┬───────┘          │
          │                │ toca             │
          │                ▼                  │
          │         ┌──────────────┐          │ 3 s
          │         │  GRABANDO    │          │
          │         │              │          │
          │         └──────┬───────┘          │
          │                │ onEndOfSpeech    │
          │                │ / toca / 60 s    │
          │                ▼                  │
          │         ┌──────────────┐    ┌─────┴──────┐
          │         │ PROCESANDO   ├───►│   ÉXITO    │
          │         └──────┬───────┘    └────────────┘
          │                │ falla
          │                ▼
          │         ┌──────────────┐
          └─────────┤    ERROR     │
            reintenta└─────────────┘
                     (vuelve a PROCESANDO con el texto conservado)
```

La cancelación no aparece en el diagrama porque no es una transición: descarta la captura y cierra la app (FA-04).

### 7.6 Accesibilidad

| Requisito | Implementación |
|---|---|
| Lectores de pantalla | `contentDescription` en todos los elementos interactivos; cambios de estado anunciados mediante `liveRegion` |
| Daltonismo | Cada estado tiene icono y forma distintivos, no solo color |
| Baja visión | Tipografía mínima 12 sp; la UI tolera escalado de fuente hasta el 130 % sin recortes |
| Contraste | Todas las combinaciones ≥ 4,5:1 |
| Discapacidad auditiva | Toda la retroalimentación háptica tiene equivalente visual |
| Áreas táctiles | Todas ≥ 48 × 48 dp |
| Uso sin mirar | Patrones hápticos distintos para iniciar, éxito y error |

### 7.7 Diseño responsivo

| Pantalla | Botón | Título | Márgenes |
|---|---|---|---|
| Pequeña (192–224 dp) | 96 dp | 14 sp, 2 líneas | 5,2 % |
| Grande (225 dp+) | 112 dp | 16 sp, 3 líneas | 5,2 % |

Los márgenes se definen como porcentaje del ancho, nunca en dp fijos.

---

## 8. Requisitos no funcionales

### 8.1 Rendimiento

| ID | Requisito | Valor objetivo | Estado |
|---|---|---|---|
| RNF-01 | Arranque en frío de la app hasta INICIAL | < 1,5 s | PENDIENTE DE VALIDAR |
| RNF-02 | Latencia entre el toque y `onReadyForSpeech` | < 300 ms | PENDIENTE DE VALIDAR |
| RNF-03 | Tiempo desde el fin del habla hasta ÉXITO, **servidor caliente** | < 4 s en percentil 50 | PENDIENTE DE VALIDAR |
| RNF-04 | Tiempo desde el fin del habla hasta ÉXITO, **servidor suspendido** | < 75 s | Definido — limitación del plan gratuito de Render, no del producto |
| RNF-05 | Timeout global de `POST /v1/notas` | 90 s | Definido |
| RNF-06 | Fluidez de la animación del anillo de voz | 60 fps sostenidos | PENDIENTE DE VALIDAR |

> RNF-04 es una restricción del entorno de despliegue elegido, no una característica deseada. Se elimina pasando a una instancia de pago. Queda documentada, con las tres medidas de mitigación de la sección 3.5.

### 8.2 Consumo de recursos

| ID | Requisito | Valor objetivo | Estado |
|---|---|---|---|
| RNF-07 | Consumo de batería por nota creada | < 0,5 % | PENDIENTE DE VALIDAR |
| RNF-08 | Consumo en reposo (app cerrada) | 0 % — sin servicios en segundo plano | Definido |
| RNF-09 | Memoria en uso | < 50 MB | PENDIENTE DE VALIDAR |
| RNF-10 | Datos transferidos por nota | < 10 KB — solo texto | Definido |
| RNF-11 | Tamaño del APK | < 8 MB | PENDIENTE DE VALIDAR |

### 8.3 Seguridad y privacidad

| ID | Requisito |
|---|---|
| RNF-12 | **No se graba, almacena ni transmite audio en ningún momento.** |
| RNF-13 | Toda comunicación por HTTPS con TLS 1.2 o superior. |
| RNF-14 | El token de sesión se cifra con una clave AES-256-GCM generada en el Android Keystore mediante `KeyGenParameterSpec`; el texto cifrado y el IV se guardan en `SharedPreferences` normales. **No se usa `androidx.security:security-crypto`**, cuyas APIs están obsoletas desde la versión 1.1.0-beta01. |
| RNF-15 | Ningún secreto reside en el cliente. |
| RNF-16 | El refresh token de Google se cifra en reposo en la base de datos. |
| RNF-17 | De los tokens de sesión solo se almacena el hash. |
| RNF-18 | El texto transcrito no se persiste en el backend tras crear la entrada. |
| RNF-19 | Se solicita el scope mínimo necesario: `auth/tasks` más identidad básica. |
| RNF-20 | El cierre de sesión revoca el token contra el endpoint de Google, además de borrarlo localmente. |
| RNF-21 | Sin analítica de terceros ni telemetría de contenido. |
| RNF-22 | El consentimiento OAuth se presenta siempre en un navegador del sistema, nunca en un WebView embebido — Google lo bloquea con `disallowed_useragent`. |

### 8.4 Usabilidad y accesibilidad

| ID | Requisito |
|---|---|
| RNF-23 | El flujo completo se ejecuta con un máximo de dos toques. |
| RNF-24 | Todas las áreas táctiles miden al menos 48 × 48 dp. |
| RNF-25 | Contraste de texto ≥ 4,5:1. |
| RNF-26 | La información de estado nunca depende exclusivamente del color. |
| RNF-27 | Compatible con TalkBack. |
| RNF-28 | La interfaz soporta escalado de fuente hasta el 130 % sin recortes. |
| RNF-29 | La vinculación no requiere teclear nada en el reloj. |

### 8.5 Compatibilidad y fiabilidad

| ID | Requisito |
|---|---|
| RNF-30 | Wear OS 3 (API 30) como mínimo; `targetSdk` 37. |
| RNF-31 | Pantallas redondas de 192 dp a 240+ dp. |
| RNF-32 | Funciona sin teléfono emparejado, antes y después de la vinculación. |
| RNF-33 | Ningún error provoca el cierre inesperado de la aplicación. |
| RNF-34 | Todos los casos de la sección 5 están cubiertos por un estado de UI. |
| RNF-35 | Ningún estado persistente vive en el sistema de archivos del backend. |
| RNF-36 | Las llamadas a `SpeechRecognizer` se hacen desde el hilo principal. |
| RNF-37 | Disponibilidad dependiente de servicios externos (Google Tasks, LLM, Render): fuera del control del proyecto. |

---

## 9. Criterios de aceptación

### CA-01 — Flujo principal
**Dado** un usuario vinculado con conexión y servidor caliente,
**cuando** presiona el botón, dice «comprar café mañana en la tarde» y termina de hablar,
**entonces** aparece ÉXITO con un título generado y existe una nueva entrada en su Google Tasks con el contenido estructurado.

### CA-02 — Anillo de nivel de voz
**Dado** el estado GRABANDO,
**cuando** el usuario habla y luego calla,
**entonces** el anillo responde visiblemente al volumen de la voz y se aquieta en el silencio, sin saltos erráticos.

### CA-03 — Guarda de duración
**Dado** que el usuario dicta de forma continua hasta cumplir 60 s,
**cuando** se alcanza la guarda,
**entonces** la captura se detiene y **se crea una nota con el texto reconocido hasta ese momento**, mostrando ÉXITO y no ERROR.

### CA-04 — Permiso denegado
**Dado** que el permiso de micrófono está denegado permanentemente,
**cuando** el usuario presiona el botón,
**entonces** aparece ERROR con acceso directo a los ajustes del sistema, sin bloquear la aplicación.

### CA-05 — Sin voz detectada
**Dado** que el usuario presiona el botón y no dice nada,
**cuando** el reconocedor devuelve `ERROR_SPEECH_TIMEOUT`,
**entonces** aparece ERROR con «No escuché nada» y **no se crea ninguna entrada**.

### CA-06 — Sin conexión
**Dado** que el dispositivo no tiene conexión,
**cuando** el usuario completa un dictado,
**entonces** aparece ERROR con opción de reintentar, y al reintentar con conexión restaurada la nota se crea **sin que el usuario vuelva a hablar**.

### CA-07 — Arranque en frío
**Dado** un servidor suspendido por inactividad,
**cuando** el usuario crea una nota,
**entonces** PROCESANDO muestra los textos escalonados, la operación no expira antes de los 90 s y la nota se crea correctamente.

### CA-08 — Vinculación
**Dado** un reloj sin sesión,
**cuando** el usuario abre la app, lee el código, lo introduce en el navegador y aprueba el acceso en Google,
**entonces** el reloj pasa automáticamente a INICIAL sin que el usuario toque el reloj, y la sesión persiste tras reiniciar la aplicación.

### CA-09 — Persistencia tras suspensión
**Dado** un usuario ya vinculado,
**cuando** el servicio del backend se suspende por inactividad y vuelve a levantarse,
**entonces** el usuario **sigue vinculado** y puede crear notas sin repetir el proceso.

### CA-10 — Degradación del servicio de IA
**Dado** que el servicio de IA no responde,
**cuando** se procesa una nota,
**entonces** la entrada se crea con el texto sin estructurar y el usuario ve ÉXITO.

### CA-11 — Acceso revocado
**Dado** que el usuario revocó el acceso desde su cuenta de Google,
**cuando** intenta crear una nota,
**entonces** aparece ERROR indicando que debe reconectar la cuenta, sin cierre inesperado.

### CA-12 — Áreas táctiles
**Dado** cualquier estado de la interfaz,
**cuando** se inspeccionan los elementos interactivos,
**entonces** todos miden al menos 48 × 48 dp.

### CA-13 — Diseño responsivo
**Dado** un emulador de 192 dp y otro de 240 dp,
**cuando** se ejecuta la aplicación en ambos,
**entonces** ningún elemento queda recortado por la curvatura y los márgenes escalan proporcionalmente.

### CA-14 — Accesibilidad
**Dado** TalkBack activado,
**cuando** el usuario recorre la interfaz,
**entonces** todos los elementos se anuncian correctamente y los cambios de estado se comunican.

### CA-15 — Privacidad del audio
**Dado** un ciclo completo de creación de nota,
**cuando** se inspecciona el almacenamiento del dispositivo,
**entonces** no existe ningún archivo de audio, ni durante ni después del proceso.

---

## 10. Fuera de alcance de esta versión

| Elemento | Razón |
|---|---|
| Publicación en Google Play | Proyecto formativo; exigiría verificación OAuth, política de privacidad publicada y verificación de dominio |
| Soporte para Google Keep | Imposible técnicamente con cuenta personal (sección 0.1) |
| App complementaria de teléfono | No necesaria con la vinculación por código |
| `RemoteAuthClient` / PKCE por teléfono | Mejor experiencia en reloj físico, pero inviable en emulador. Mejora opcional. |
| Dominio propio corto para la URL de vinculación | Mejoraría la legibilidad en pantalla; no bloquea nada |
| Tile o complicación en la esfera | Mejora de acceso, no funcionalidad nueva; candidata a v2 |
| Cola de envío sin conexión | Complejidad de sincronización desproporcionada para v1 |
| Múltiples idiomas | v1 fija `es-CO` |
| Reinicio y concatenación de sesiones de reconocimiento | Solo si R2 lo obliga (sección 11) |
| Edición de la nota tras crearla | Viola la restricción 14 del planteamiento |

---

## 11. Riesgos

| # | Riesgo | Impacto | Prob. | Mitigación |
|---|---|---|---|---|
| **R1** | El reconocedor del emulador se comporta distinto al de un reloj real | Alto | Media | Probar en hardware físico antes de cerrar la UX |
| **R2** | **El reconocedor cierra la sesión mucho antes de los 60 s en habla continua** | Alto | Media | **Medir con un dictado largo en la primera semana.** Si corta a los 10–15 s, evaluar reinicio y concatenación en `onEndOfSpeech` — es la única deuda arquitectónica real que queda abierta |
| **R3** | La latencia total arruina la sensación de inmediatez | Alto | Media | Ping de calentamiento, timeout de 90 s y texto escalonado (sección 3.5). Medir temprano. |
| **R4** | El reconocimiento requiere conexión en el dispositivo objetivo | Medio | Media | Validar en modo avión con el reconocedor estándar. Si el reconocimiento sin conexión resulta necesario, añadir la cadena de respaldo descrita en la sección 3.3. |
| **R5** | Los límites del PostgreSQL gratuito de Render (retención, tamaño) afectan al proyecto | Medio | Media | Verificar los límites vigentes del plan antes de desplegar; el volumen de datos es mínimo (tres tablas pequeñas) |
| **R6** | El backend consume el tiempo del proyecto y desplaza el trabajo de UI | Medio | Alta | Construirlo en dos etapas: primero un único endpoint con token fijo para validar el canal completo, después la vinculación real. La etapa 1 es un andamio **deliberadamente desechable**, documentado como tal. |
| **R7** | La normalización de `rmsdB` produce un anillo errático | Bajo | Media | Suavizado exponencial y rango calibrado en pruebas, no mapeo lineal |
| **R8** | **El refresh token caduca a los 7 días con la app en estado «Testing»** | Medio | **Alta** | Documentado en 3.4. Revincular semanalmente durante el desarrollo, o pasar a estado «Production». El fallo se presenta como un error de autenticación aparentemente aleatorio, así que debe estar escrito antes de que ocurra. |

**Riesgo cerrado:** el R5 de la v1.0 —la UI del reconocedor del sistema rompiendo el diseño propio— queda resuelto por la decisión de usar `SpeechRecognizer` directamente.

---

## 12. Orden de implementación sugerido

No es parte del PRD en sentido estricto, pero se desprende de los riesgos.

| Etapa | Qué se construye | Qué valida |
|---|---|---|
| 1 | App Wear OS con los cinco estados, `SpeechRecognizer` y el anillo real. Sin backend: el texto se muestra en pantalla. | R1, R2, R4, R7 — y es la parte evaluada del proyecto |
| 2 | Backend mínimo en Render: un endpoint, token fijo en el cliente, sin LLM, creando la tarea con **tu** refresh token obtenido por script | El canal completo hasta Google Tasks |
| 3 | LLM de estructuración | R3 |
| 4 | Vinculación real (sección 3.4) y PostgreSQL | CA-08, CA-09 |
| 5 | Retirada del andamio de la etapa 2 | Que no quede deuda |

La etapa 1 valida los riesgos más altos y es además el núcleo de lo que se evalúa. Empezar por el backend invertiría esa prioridad.

---

## 13. Glosario

| Término | Definición |
|---|---|
| **Wear OS** | Sistema operativo de Google para relojes inteligentes, basado en Android |
| **Compose for Wear OS** | Toolkit declarativo de interfaz, recomendado oficialmente por Google para relojes |
| **dp** | *Density-independent pixel*: unidad independiente de la densidad de pantalla |
| **sp** | *Scale-independent pixel*: unidad para texto, sensible a los ajustes de accesibilidad |
| **Breakpoint** | Umbral de tamaño de pantalla a partir del cual cambia el diseño |
| **`SpeechRecognizer`** | API de Android para reconocimiento de voz con control programático completo |
| **`RecognitionListener`** | Interfaz de callbacks del reconocedor: inicio, fin, nivel de voz, resultados, errores |
| **`rmsdB`** | Nivel de sonido entrante en decibelios, entregado por `onRmsChanged` |
| **Scope (OAuth)** | Cadena que define qué datos y qué nivel de acceso solicita una aplicación |
| **Access token** | Credencial de corta duración que autoriza llamadas a una API |
| **Refresh token** | Credencial de larga duración que permite obtener nuevos access tokens |
| **Device authorization grant** | Flujo OAuth (RFC 8628) para dispositivos sin navegador: se muestra un código y se aprueba en otro dispositivo |
| **PKCE** | *Proof Key for Code Exchange* (RFC 7636): extensión de OAuth para clientes que no pueden guardar secretos |
| **Arranque en frío** | Primera petición tras una suspensión por inactividad, mucho más lenta que las siguientes |
| **App standalone** | App de reloj que funciona sin requerir un teléfono para su función principal |
| **Háptica** | Retroalimentación por vibración |
| **WCAG AA** | Nivel de conformidad de accesibilidad; exige contraste mínimo de 4,5:1 |

---

## 14. Anexo — Elementos pendientes

| # | Elemento | Tipo | Cómo se resuelve |
|---|---|---|---|
| 1 | Nombre definitivo del producto | PENDIENTE DE DEFINIR | Decisión del autor |
| 2 | **Duración real que tolera el reconocedor en habla continua** | PENDIENTE DE VALIDAR | **Dictado largo medido, etapa 1. Es el pendiente más importante (R2).** |
| 3 | Modelo de LLM concreto y su costo por nota | PENDIENTE DE VALIDAR | Prototipo, etapa 3 |
| 4 | Latencia real de extremo a extremo | PENDIENTE DE VALIDAR | Medición, etapas 2 y 3 |
| 5 | Comportamiento del reconocedor sin conexión | PENDIENTE DE VALIDAR | Prueba en modo avión, etapa 1 |
| 6 | Rango útil de `rmsdB` en el dispositivo objetivo | PENDIENTE DE VALIDAR | Calibración, etapa 1 |
| 7 | Límites vigentes del PostgreSQL gratuito de Render | PENDIENTE DE VALIDAR | Documentación de Render, antes de la etapa 4 |
| 8 | Valores de RNF-01, 02, 03, 06, 07, 09, 11 | PENDIENTE DE VALIDAR | Medición sobre prototipo |
| 9 | Caducidad de 7 días del refresh token en modo Testing | PENDIENTE DE VALIDAR | Documentación oficial de Google (el dato actual proviene de fuentes secundarias) |

**Resueltos en la v2.0:** mecanismo de transcripción, duración máxima, idioma, plataforma de despliegue, persistencia y mecanismo de vinculación.

**Resueltos en la v2.1:** stack del reloj, stack del backend (Fastify), estructura del repositorio, almacenamiento seguro del token y artefacto de UI.

**Resueltos en la v2.2:** versiones de SDK y de UI alineadas con el proyecto real, build de Gradle en la raíz, elección del reconocedor (solo el estándar), comportamiento de la cancelación (FA-04) y caso del reconocedor no disponible (FA-21).

---

*Documento sustentado en el informe de investigación `investigacion-smartwatch-voz-keep.md`, que contiene las fuentes oficiales de cada afirmación técnica.*

*Fuentes añadidas en la v2.0:* la política de Google sobre WebViews embebidos en OAuth 2.0, la documentación del plan gratuito de Render y los cambios de API 31 en `android.speech.SpeechRecognizer`.

*Fuentes añadidas en la v2.1:*

- [Wear Compose Material 3 — release notes](https://developer.android.com/jetpack/androidx/releases/wear-compose-m3) — artefacto, versión estable 1.5.0 y advertencia de no mezclar librerías
- [Jetpack Security — release notes](https://developer.android.com/jetpack/androidx/releases/security) — deprecación de `androidx.security:security-crypto` en 1.1.0-beta01
- [Render — Monorepo support](https://render.com/docs/monorepo-support) — *Root Directory* y *build filters*
- Caducidad del refresh token en modo Testing: fuentes secundarias coincidentes, **pendiente de confirmar** en la documentación oficial de Google (anexo, punto 9)
