---
name: wear-ui-ux
description: Sistema de diseño y reglas de UI/UX para la app de reloj (Wear OS) de VoiceNoteAI — estética de felpa (fondo crema, parche central de color, tipografía Lilita One), composición en pantalla redonda, paleta por estado, tipografía, accesibilidad, movimiento y háptica. Úsala al crear o cambiar cualquier pantalla, estado, color, texto, icono o recurso gráfico del módulo `wear/`, al revisar una propuesta de diseño (imágenes en docs/) o al regenerar los recursos de felpa. Para APIs y componentes de Compose Material 3 usa además la skill `wear-compose-m3`.
---

# UI/UX del reloj — VoiceNoteAI

La app es **una sola pantalla con cinco estados** (INICIAL, GRABANDO, PROCESANDO, ÉXITO, ERROR) que el usuario
mira de reojo, con la muñeca levantada, durante 2–5 segundos. Cada decisión de diseño se juzga con esa
pregunta: *¿se entiende en un vistazo, sin leer?*

Referencia visual: `docs/propuesta_diseño.png` (fuente de verdad).

## 1. Principios de UX para reloj

1. **Un estado = una acción.** Hay un único objetivo táctil por estado: el parche central. Nada de menús,
   listas ni segundos botones. Si un estado necesita acción (detener, reintentar, ajustes, volver) la hace
   el parche, y el texto inferior dice `TOCA PARA …`.
2. **Vistazo antes que lectura.** El estado se reconoce por **color + icono + háptica**, nunca solo por el
   texto, y nunca solo por el color (daltonismo: rojo/verde).
3. **Texto mínimo.** Máximo 3–4 palabras por mensaje de estado; 2 líneas como tope absoluto. Si no cabe,
   se acorta el mensaje en `strings.xml`, no se baja la fuente.
4. **Objetivos grandes.** Área táctil ≥ 48 dp; el parche mide 45 % del ancho (≥ 86 dp en 192 dp).
5. **Sin sorpresas de posición.** El parche ocupa **el mismo lugar y tamaño en todos los estados**: el ojo y
   el dedo ya saben dónde mirar/tocar. (Además tapa la costura del centro del fondo generado.)
6. **Retroalimentación inmediata.** Toque → escala 0,92 + háptico `VIRTUAL_KEY` en < 100 ms. Cada estado
   tiene su patrón háptico (`Haptic` en `NoteUiState.kt`).
7. **Movimiento con propósito.** Transición entre estados: fundido 200/150 ms. Dentro de un estado solo se
   anima lo que informa (nivel de voz, progreso de 60 s, giro de procesamiento, trazo del check).

## 2. Composición en pantalla redonda

Medidas como fracción del lado de la pantalla `S` (`BoxWithConstraints.maxWidth`), así escalan solas
entre 192 dp y 240 dp+:

```
            ┌───────────── S ─────────────┐
   0.00 S   │        (fondo de felpa)      │
   0.13 S   │       ╭───────────────╮      │  ← parche: diámetro 0.45 S, centro en 0.355 S
            │       │   icono 40 %   │      │
   0.58 S   │       ╰───────────────╯      │
   0.62 S   │      TEXTO PRINCIPAL (1–2)   │  ← ancho máx. 0.72 S (cuadrado inscrito)
            │        texto secundario      │  ← termina hacia 0.80 S en el peor caso
   0.85 S   │                              │  ← nada por debajo: la curva lo corta
            └──────────────────────────────┘
```

- **Zona segura de texto:** ancho ≤ 0.72 S y nada por debajo de 0.85 S ni por encima de 0.08 S. El diseño
  original ponía el parche en 0.175 S y el texto en 0.69 S, pero en el emulador el `TOCA PARA …` de ERROR
  (mensaje de 2 líneas + pista) se perdía contra el borde: por eso todo sube.
- **Indicadores de borde** (progreso de 60 s) van pegados al bisel, `fillMaxSize`, trazo 3–4 dp.
- **Pantallas cuadradas:** el fondo de felpa es redondo; las esquinas se pintan con `FurPalette.Base`.

## 3. Paleta

Toda la paleta vive en `presentation/theme/Theme.kt` (`FurPalette`). No se escriben hex sueltos en pantallas.

| Token | Uso | Hex |
|---|---|---|
| `Base` | Felpa crema (esquinas, respaldo del fondo) | `#D8CCB8` |
| `Ink` | Texto principal sobre crema | `#1B1A17` |
| `InkSoft` | Texto secundario | `#4A453C` |
| `Thread` | Bordado (iconos sobre los parches) | `#F1E8D8` |
| `Idle` | Parche INICIAL — azul | `#2158A2` |
| `Recording` | Parche GRABANDO — rojo | `#D63A3A` |
| `Processing` | Parche PROCESANDO — ámbar | `#E09A14` |
| `Success` | Parche ÉXITO — verde | `#2E8B57` |
| `Error` | Parche ERROR — naranja óxido | `#C2451C` |

Reglas:
- Texto siempre `Ink`/`InkSoft` sobre crema: contraste ≥ 7:1 y ≥ 4,5:1 (WCAG AA). **Nunca** texto de color
  de estado sobre crema salvo en elementos no textuales (anillos, arcos).
- Iconos sobre parches siempre `Thread` (crema bordado) con sombra suave: contraste ≥ 3:1 con cada parche.
- Si se cambia un color de estado, se cambia en **dos sitios**: `FurPalette` y `STATE_COLORS` de
  `scripts/build_assets.py`, y se regeneran los recursos.

## 4. Tipografía

- **Lilita One** (`res/font/lilita_one.ttf`, OFL, licencia en `wear/licenses/`): pesada, redondeada y algo
  condensada, como el diseño, y legible a 12 sp. Se descartó Rubik Dirt: la textura sucia se pierde a tamaño reloj.
- Mensajes de estado en **MAYÚSCULAS** al pintar (`text.uppercase()`); las cadenas en `strings.xml` siguen en
  formato normal. **El contenido del usuario (título de la nota) no se pasa a mayúsculas.**
- Escala: principal 14 sp (pantalla < 225 dp) / 16 sp (≥ 225 dp); secundario 11 / 12 sp. Unidades `sp`
  para respetar la escala de fuente del sistema; probar con `@WearPreviewFontScales`.

## 5. Estados

| Estado | Parche | Icono (Thread) | Texto inferior | Extra |
|---|---|---|---|---|
| INICIAL | `patch_idle` (micrófono bordado incluido) | — | `TOCA PARA HABLAR` | toque = grabar |
| GRABANDO | `patch_recording` | Stop | `ESCUCHANDO…` | anillo de voz rojo translúcido tras el parche; arco de 60 s en el bisel; toque = detener |
| PROCESANDO | `patch_processing` | gusanito bordado que da vueltas reptando (`CaterpillarSpinner`, 2,4 s por vuelta) | etapa (`PROCESANDO`, `CASI LISTO…`, `DESPERTANDO EL SERVIDOR`) | sin toque |
| ÉXITO | `patch_success` | check que se traza en 300 ms | título de la nota (2 líneas) + `GUARDADO` | anuncio TalkBack del título |
| ERROR | `patch_error` | aviso | mensaje + `TOCA PARA REINTENTAR/ABRIR AJUSTES/VOLVER` | toque = acción del error |

## 6. Accesibilidad (obligatorio)

- Cada parche tocable: `Role.Button` + `contentDescription` con la acción (“Grabar nota”, “Detener grabación”,
  “Reintentar”…).
- Textos de estado con `liveRegion = Polite` para que TalkBack anuncie los cambios.
- Imágenes decorativas (`fur_background`) con `contentDescription = null`.
- Nada depende solo del color (ver §1.2).

## 7. Recursos gráficos

Los recursos de felpa salen de la imagen del diseño con `scripts/build_assets.py` (Pillow):

```bash
python -m pip install --user pillow
python .claude/skills/wear-ui-ux/scripts/build_assets.py docs/propuesta_diseño.png wear/src/main/res/drawable-nodpi
```

Genera `fur_background.webp` (sin disco ni texto), `patch_idle.webp` y `patch_<estado>.webp` (felpa lisa
teñida). Presupuesto: < 300 KB en total (RNF-11, APK < 8 MB). Los iconos **no** se hornean en la imagen: los
pinta Compose (`AppIcons`) para poder animarlos y mantenerlos nítidos.

Si llega una propuesta nueva: medir de nuevo las constantes del script (centro/radio del disco, caja del
texto), regenerar, y revisar una hoja de contacto de todos los parches sobre el fondo antes de compilar.

## 8. Lista de revisión antes de dar por terminado un cambio de UI

- [ ] El parche está en la misma posición y tamaño en los cinco estados.
- [ ] Ningún texto sale de la zona segura (0.72 S de ancho, entre 0.08 S y 0.85 S) en 192 dp y en 227 dp.
- [ ] Mensajes ≤ 2 líneas con fuente al 130 % (`@WearPreviewFontScales`).
- [ ] Colores solo desde `FurPalette`; texto en `Ink`.
- [ ] Cada estado distinguible en escala de grises (icono + texto).
- [ ] Parches tocables con `Role.Button` y descripción; textos con `liveRegion`.
- [ ] `./gradlew :wear:assembleDebug :wear:testDebugUnitTest` en verde.
- [ ] Vistas previas de los cinco estados revisadas en Android Studio.
