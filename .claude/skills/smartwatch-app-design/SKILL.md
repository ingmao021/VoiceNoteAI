---
name: smartwatch-app-design
description: Diseña apps, tiles, complicaciones y notificaciones para smartwatch (Wear OS / relojes redondos) con estilo Material 3 Expressive, iconos profesionales y la paleta verde #cbfe00 + azul #009bfe. Úsala siempre que pidan diseñar, maquetar, prototipar o codificar pantallas de reloj inteligente, wearables, tiles o UI circular, aunque no mencionen la paleta.
---

# Diseño de apps para smartwatch — Verde Volt + Azul

Esta skill produce interfaces de reloj inteligente modernas, legibles de un vistazo y coherentes
entre sí. El lenguaje visual parte de Material 3 Expressive para Wear OS (contenedores en forma de
píldora, botones que abrazan el borde inferior, fondo negro OLED) y lo personaliza con una marca
de alto contraste: **verde #cbfe00 (primario)** y **azul #009bfe (secundario)**.

La razón de fondo de casi todas las reglas: en un reloj la gente mira 2–5 segundos, con el brazo
en movimiento, a menudo bajo el sol. Todo lo que no se entiende en un vistazo sobra.

---

## 1. Flujo de trabajo

1. **Entender el caso de uso.** Identifica la tarea principal (una por pantalla/tile), el
   contexto (corriendo, en reunión, de noche) y la superficie:
   - **Tile**: pantalla deslizable desde la esfera, sin scroll, 1 tarea, acceso inmediato.
   - **App**: pantallas con scroll vertical (listas), detalle, flujos cortos (máx. 3 niveles).
   - **Complicación**: dato mínimo dentro de la esfera (valor + icono o arco).
   - **Notificación**: título, cuerpo corto, 1–2 acciones.
2. **Elegir un patrón de layout** de la sección 5 antes de dibujar nada.
3. **Aplicar tokens** (color, tipografía, forma, espaciado) — nunca valores sueltos.
4. **Iconografía** con un único set profesional (sección 6).
5. **Construir el entregable** en el formato pedido (sección 9): mockup HTML/SVG, código
   Jetpack Compose / ProtoLayout, o especificación para Figma.
6. **Revisar con la checklist** (sección 10) y, si puedes renderizar, mira la captura antes de
   entregar: los recortes en pantallas redondas solo se ven al renderizar.

Si el pedido es ambiguo (¿tile o app?, ¿qué datos?), toma la interpretación más razonable,
dilo en una línea y continúa; un mockup es barato de rehacer.

---

## 2. Color

Fondo siempre **negro puro #000000**: en OLED ahorra batería y hace que el verde y el azul brillen.
Los roles siguen la nomenclatura de Material 3 para que el paso a código sea directo.

| Rol | Hex | Uso | Texto/icono encima (`on-`) | Contraste |
|---|---|---|---|---|
| `primary` | `#cbfe00` | Acción principal, selección, progreso, Edge Button principal | `#0e1400` | 15.8:1 |
| `primary-container` | `#2b3600` | Fondo tonal verde (estado activo suave) | `#e6ff80` | 11.6:1 |
| `secondary` | `#009bfe` | Botones secundarios, datos de salud, enlaces | `#00131f` | 6.4:1 |
| `secondary-container` | `#00344f` | Chips/listas, Edge Button tonal | `#9ed6ff` | 8.4:1 |
| `background` | `#000000` | Fondo de pantalla | `#e4e6d8` | 16:1 |
| `surface-container` | `#1b1d17` | Tarjetas, píldoras de datos | `#e4e6d8` / `#c5c8b8` | 13.4 / 10:1 |
| `surface-container-high` | `#26291f` | Pistas de progreso, elementos elevados | `#e4e6d8` | 11.7:1 |
| `outline` | `#8f9284` | Bordes, divisores, estado deshabilitado | — | — |
| `error` | `#ffb4ab` | Alertas, valores fuera de rango | `#690005` | — |

**Reglas críticas (por qué importan):**
- **Nunca texto blanco sobre #009bfe** (contraste 2.95:1, no pasa WCAG). Sobre azul usa
  `#00131f` o negro. Es el error más fácil de cometer con esta paleta.
- **Nunca texto blanco sobre #cbfe00**: usa `#0e1400`.
- Verde y azul puros como texto sobre negro sí funcionan (17.7:1 y 7.1:1): úsalos para cifras
  destacadas o etiquetas cortas.
- **Jerarquía de color, no arcoíris:** en una pantalla, máximo **un** elemento en `primary`
  sólido (la acción o el dato clave). El azul y los contenedores tonales hacen el resto. Si todo
  es verde, nada destaca.
- El color nunca es el único indicador de estado: acompaña con icono, forma o texto.
- Semántica sugerida: verde = actividad/logro/acción; azul = salud/información/comunicación.

Mapeo desde el estilo de referencia (lila/turquesa → esta marca): chip turquesa oscuro →
`secondary-container`; botón "+" lila → `primary`; botones laterales celeste → `secondary`;
botón seleccionado → `primary`; "More" oscuro → `secondary-container`.

---

## 3. Tipografía

Fuente: **Google Sans Flex** (o **Roboto Flex**) — variables, la estándar de Wear OS M3.
Fallback web: `"Google Sans Flex","Roboto Flex","Inter",system-ui,sans-serif`.

| Estilo | Tamaño (sp) | Peso | Uso |
|---|---|---|---|
| Display L | 40 | 600 | Métrica principal (pasos, BPM, hora) |
| Display M | 30 | 600 | Métrica secundaria |
| Title L | 20 | 600 | Título de pantalla en relojes grandes |
| Title M | 16 | 600 | Título de tile/pantalla ("Fitness") |
| Label L | 15 | 600 | Texto de botón / Edge Button |
| Body L | 14 | 500–600 | Línea 1 de chip (nombre) |
| Body M | 12–13 | 400–500 | Línea 2 de chip, subtítulos |
| Caption | 11–12 | 500 | Unidades, metadatos (mín. absoluto 11) |

- Máximo 2 líneas por chip; trunca con elipsis. Máximo ~3 estilos por pantalla.
- Números grandes con cifras tabulares (`font-variant-numeric: tabular-nums`) para que no
  "bailen" al actualizarse.
- Diseña asumiendo escala de fuente del sistema mayor: deja holgura vertical.

---

## 4. Forma, espaciado y tamaños

**Pantalla:** diseña primero a **192–216 dp** (pequeño) y verifica a **225–240 dp** (grande);
**225 dp** es el punto de quiebre recomendado para mostrar contenido extra. Márgenes en
**porcentaje**, no en dp fijos, para que escalen sin recortes en la circunferencia.

| Elemento | Medida (en 225 dp) |
|---|---|
| Margen lateral contenido | ~8% del ancho (el contenido ocupa ~84%) |
| Zona superior reservada (icono de sistema/título) | ~12–18% |
| Área táctil mínima | **48 × 48 dp** siempre |
| Chip / Button | alto 52 dp, radio 26 dp (píldora completa) |
| Botón compacto | alto 32–40 dp |
| Grupo de 3 botones | alto 80–100 dp, separación 4 dp |
| Edge Button | ancho ~45–50% de pantalla, alto 40–56 dp, pegado al borde inferior |
| Separación entre elementos | 4 dp (agrupados) / 8 dp (bloques) |
| Avatar en chip | 32–38 dp, circular |
| Iconos | 24 dp estándar · 26–32 dp en botones grandes · 16–20 dp en píldoras de datos |

**Formas (Material 3 Expressive):** píldora completa (`50%`) por defecto; radios 18–28 dp en
botones altos. El estado **seleccionado** se comunica *también* con la forma: el botón activo
pierde redondez (p. ej. de 28 → 18 dp) además de cambiar a `primary` — la "morfología de forma"
es la firma de M3 Expressive.

**Edge Button:** el botón característico de relojes redondos. Su parte superior es redondeada y
la inferior sigue la curva de la pantalla. Úsalo para la acción principal o "Más". Uno por
pantalla como máximo.

---

## 5. Patrones de layout (tiles y pantallas)

Estructura de slots de arriba abajo: **título (opcional) → contenido principal → Edge Button**.

**A. Lista de contactos/mensajes**
2 chips tonales (`secondary-container`) con avatar + nombre + vista previa, Edge Button
`primary` con icono `add`. En ≥225 dp cabe un 3er chip o el título.

**B. Grupo de acciones**
Título corto + 3 botones altos con solo icono (`secondary`), el seleccionado en `primary` con
radio menor; Edge Button tonal "Más". Para 2 opciones usa 2 botones más anchos; nunca >3 en
una fila en pantallas pequeñas.

**C. Progreso / métrica**
Arco de progreso de 270° siguiendo el borde (hueco abajo para el Edge Button), pista en
`surface-container-high`, progreso en `primary`, trazo 6–8 dp con extremos redondeados. Centro:
etiqueta (Caption) + cifra (Display L) + contexto ("Meta 10.000 · 78%"). Debajo, 1–2 píldoras
de datos (`surface-container`) con icono de color + valor. Edge Button `primary` (play/iniciar).

**D. Tarjeta de datos**
Tarjeta `surface-container` radio 24–26 dp con icono, valor grande y tendencia; útil para clima,
ritmo cardiaco, sueño.

**E. Entrenamiento en curso**
Cifra dominante (tiempo o ritmo), 2 métricas secundarias, controles pausa/parar como botones
redondos 52 dp (pausa en `secondary`, parar en `surface-container-high` con icono `error`).

**F. App con scroll (lista)**
Título centrado arriba que se reduce al hacer scroll, chips a ancho completo con escalado en
los bordes (los elementos se encogen/atenúan cerca del borde superior e inferior, como
`TransformingLazyColumn`), Edge Button al final de la lista. Indicador de scroll en el borde
derecho.

**Notas de tiles:** el icono de la app que aparece arriba lo pone el sistema; **no lo dibujes
dentro del diseño del tile** (aparecería duplicado). En los mockups puedes mostrarlo como
referencia, marcado como "icono de sistema". Un tile = una tarea ("Iniciar carrera"), no un menú.

---

## 6. Iconografía profesional

Usa **un solo set** en toda la app; mezclar sets es lo que más delata un diseño amateur.

**Set principal: Material Symbols Rounded** (nativo de Wear OS, Apache 2.0).
- Ajustes: `FILL 1` (relleno) para iconos dentro de botones sólidos y estados activos;
  `FILL 0` (contorno) solo para estados inactivos en listas. `wght 500`, `GRAD 0`, `opsz 24`.
- Web: `<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@24,500,1,0" rel="stylesheet">`
  y `<span class="material-symbols-rounded">directions_run</span>`.
- Android: Material Symbols vía Vector Asset Studio / `androidx.compose.material.icons`.
- Alternativas si el destino no es Android: **Phosphor (Fill/Bold)** o **Lucide** (stroke 2.25).
  Elige una y mantenla.

**Diccionario de iconos (nombres Material Symbols):**

| Concepto | Icono | Concepto | Icono |
|---|---|---|---|
| Correr | `directions_run` | Ritmo cardiaco | `favorite` / `monitor_heart` |
| Bicicleta | `directions_bike` | Calorías | `local_fire_department` |
| Yoga / meditación | `self_improvement` | Pasos | `footprint` |
| Caminar | `directions_walk` | Sueño | `bedtime` |
| Natación | `pool` | Agua | `water_drop` |
| Gimnasio | `fitness_center` | Temporizador | `timer` |
| Añadir | `add` | Más opciones | `more_horiz` |
| Confirmar | `check` | Cerrar/cancelar | `close` |
| Reproducir / pausa / parar | `play_arrow` / `pause` / `stop` | Siguiente / anterior | `skip_next` / `skip_previous` |
| Mensaje | `chat` | Llamada | `call` |
| Voz | `mic` | Música | `music_note` |
| Notificación | `notifications` | Ajustes | `settings` |
| Batería | `battery_full` | Energía/app | `bolt` |
| Clima | `partly_cloudy_day` | Ubicación | `location_on` |

**Reglas:**
- Iconos dentro de botones con solo icono: **siempre** con descripción accesible
  (`contentDescription` / `aria-label`).
- Color del icono = color `on-` del contenedor (p. ej. `#00131f` sobre azul), o `primary`/
  `secondary` cuando va suelto sobre negro o sobre `surface-container`.
- No uses emojis como iconos de interfaz (sí pueden aparecer dentro del contenido de un mensaje).
- No dibujes iconos a mano en SVG improvisado; usa el set.

**Sin conexión (mockups HTML offline):** usa estos símbolos SVG verificados de Material Icons
(Apache 2.0). Defínelos una vez y reutilízalos con `<svg><use href="#i-run"/></svg>`:

```html
<svg width="0" height="0" style="position:absolute">
  <symbol id="i-bolt" viewBox="0 0 24 24"><path d="M11 21h-1l1-7H7.5c-.58 0-.57-.32-.38-.66.19-.34.05-.08.07-.12C8.48 10.94 10.42 7.54 13 3h1l-1 7h3.5c.49 0 .56.33.47.51l-.07.15C12.96 17.55 11 21 11 21z"/></symbol>
  <symbol id="i-add" viewBox="0 0 24 24"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></symbol>
  <symbol id="i-run" viewBox="0 0 24 24"><path d="M13.49 5.48c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm-3.6 13.9l1-4.4 2.1 2v6h2v-7.5l-2.1-2 .6-3c1.3 1.5 3.3 2.5 5.5 2.5v-2c-1.9 0-3.5-1-4.3-2.4l-1-1.6c-.4-.6-1-1-1.7-1-.3 0-.5.1-.8.1l-5.2 2.2v4.7h2v-3.4l1.8-.7-1.6 8.1-4.9-1-.4 2 7 1.4z"/></symbol>
  <symbol id="i-bike" viewBox="0 0 24 24"><path d="M15.5 5.5c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zM5 12c-2.8 0-5 2.2-5 5s2.2 5 5 5 5-2.2 5-5-2.2-5-5-5zm0 8.5c-1.9 0-3.5-1.6-3.5-3.5s1.6-3.5 3.5-3.5 3.5 1.6 3.5 3.5-1.6 3.5-3.5 3.5zm5.8-10l2.4-2.4.8.8c1.3 1.3 3 2.1 5.1 2.1V9c-1.5 0-2.7-.6-3.6-1.5l-1.9-1.9c-.5-.4-1-.6-1.6-.6s-1.1.2-1.4.6L7.8 8.4c-.4.4-.6.9-.6 1.4 0 .6.2 1.1.6 1.4L11 14v5h2v-6.2l-2.2-2.3zM19 12c-2.8 0-5 2.2-5 5s2.2 5 5 5 5-2.2 5-5-2.2-5-5-5zm0 8.5c-1.9 0-3.5-1.6-3.5-3.5s1.6-3.5 3.5-3.5 3.5 1.6 3.5 3.5-1.6 3.5-3.5 3.5z"/></symbol>
  <symbol id="i-yoga" viewBox="0 0 24 24"><path d="M12 2c1.1 0 2 .9 2 2s-.9 2-2 2-2-.9-2-2 .9-2 2-2zm9 14v-2c-2.24 0-4.16-.96-5.6-2.68l-1.34-1.6C13.68 9.26 13.12 9 12.53 9h-1.05c-.59 0-1.15.26-1.53.72l-1.34 1.6C7.16 13.04 5.24 14 3 14v2c2.77 0 5.19-1.17 7-3.25V15l-3.88 1.55c-.67.27-1.12.93-1.12 1.66C5 19.2 5.8 20 6.79 20H9v-.5c0-1.38 1.12-2.5 2.5-2.5h3c.28 0 .5.22.5.5s-.22.5-.5.5h-3c-.83 0-1.5.67-1.5 1.5v.5h7.21c.99 0 1.79-.8 1.79-1.79 0-.73-.45-1.39-1.12-1.66L14 15v-2.25c1.81 2.08 4.23 3.25 7 3.25z"/></symbol>
  <symbol id="i-heart" viewBox="0 0 24 24"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></symbol>
  <symbol id="i-fire" viewBox="0 0 24 24"><path d="M13.5.67s.74 2.65.74 4.8c0 2.06-1.35 3.73-3.41 3.73-2.07 0-3.63-1.67-3.63-3.73l.03-.36C5.21 7.51 4 10.62 4 14c0 4.42 3.58 8 8 8s8-3.58 8-8C20 8.61 17.41 3.8 13.5.67zM11.71 19c-1.78 0-3.22-1.4-3.22-3.14 0-1.62 1.05-2.76 2.81-3.12 1.77-.36 3.6-1.21 4.62-2.58.39 1.29.59 2.65.59 4.04 0 2.65-2.15 4.8-4.8 4.8z"/></symbol>
  <symbol id="i-play" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></symbol>
</svg>
```
Para cualquier otro icono offline, usa el path oficial de Material Icons si lo conoces con
certeza; si no, usa la fuente Material Symbols o indica el nombre del icono como marcador.

---

## 7. Movimiento e interacción

- **Pulsar:** el botón se comprime (escala ~0.95) y su radio se reduce; vuelve con un muelle
  suave (~200–300 ms). Es la "shape morph" de M3 Expressive.
- **Seleccionar:** transición de color + forma a la vez (azul píldora → verde radio 18 dp).
- **Progreso:** los arcos se animan desde 0 al entrar (≤600 ms, ease-out).
- **Scroll:** elementos que se escalan/atenúan al acercarse al borde curvo.
- Soporta **corona/bisel giratorio** para scroll y ajustes de valor (no solo gestos táctiles).
- Animaciones cortas y con propósito: cuestan batería y atención. Respeta "reducir movimiento".

---

## 8. Accesibilidad y contexto de uso

- Contraste ≥ 4.5:1 para texto normal y ≥ 3:1 para texto grande e iconos (la tabla de color ya
  cumple; respeta los pares `on-`).
- Áreas táctiles ≥ 48 dp aunque el elemento visible sea más pequeño.
- Todo icono sin texto lleva descripción para TalkBack.
- Prueba con fuente grande y negrita del sistema: nada debe recortarse contra el círculo.
- **Modo ambiente (AOD):** fondo negro, solo contornos/texto en `outline` o gris claro, sin
  rellenos grandes de verde (quemado OLED y batería); oculta datos que cambian por segundo.

---

## 9. Formatos de entrega

### 9.1 Mockup HTML/SVG (por defecto para "diséñame / muéstrame")
- Pantalla circular a **225 px = 225 dp**, `border-radius:50%; overflow:hidden; background:#000`.
  Escala la presentación con `zoom:2` en un contenedor para verla nítida.
- Opcional: bisel del reloj (anillo oscuro con degradado) y fondo de presentación con degradados
  suaves verde/azul muy oscuros — pero la pantalla en sí siempre negra.
- Todos los colores como variables CSS con los nombres de rol de la sección 2.
- Muestra varias pantallas lado a lado con un pie de una línea (nombre + patrón).
- Edge Button en CSS: `position:absolute; bottom:3px; left:50%; transform:translateX(-50%);
  width:108px; height:46px; border-radius:22px 22px 54px 54px / 20px 20px 34px 34px;`
- Arco de progreso 270° en SVG: `r=104` en lienzo 225, `stroke-dasharray="490 654"` (pista) y
  `"<490×progreso> 654"` (valor), ambos con `transform="rotate(135 112.5 112.5)"` y
  `stroke-linecap="round"`.
- Ejemplo completo y renderizado de los patrones A, B y C: `assets/ejemplo-mockup.html`.
  Léelo y parte de él en lugar de empezar desde cero.
- Usa nombres y textos realistas en el idioma del usuario; avatares como iniciales sobre
  `primary`/`secondary` si no hay fotos.

Tokens CSS base:
```css
:root{
  --bg:#000000; --surface-container:#1b1d17; --surface-container-high:#26291f;
  --on-surface:#e4e6d8; --on-surface-variant:#c5c8b8; --outline:#8f9284;
  --primary:#cbfe00; --on-primary:#0e1400;
  --primary-container:#2b3600; --on-primary-container:#e6ff80;
  --secondary:#009bfe; --on-secondary:#00131f;
  --secondary-container:#00344f; --on-secondary-container:#9ed6ff;
  --error:#ffb4ab; --on-error:#690005;
  --font:"Google Sans Flex","Roboto Flex","Inter",system-ui,sans-serif;
}
```

### 9.2 Jetpack Compose (Wear Compose Material 3)
Usa `androidx.wear.compose:compose-material3` (no la librería material 2 de Wear). Tema:

```kotlin
val VoltColorScheme = ColorScheme(
    primary = Color(0xFFCBFE00),            onPrimary = Color(0xFF0E1400),
    primaryContainer = Color(0xFF2B3600),   onPrimaryContainer = Color(0xFFE6FF80),
    secondary = Color(0xFF009BFE),          onSecondary = Color(0xFF00131F),
    secondaryContainer = Color(0xFF00344F), onSecondaryContainer = Color(0xFF9ED6FF),
    surfaceContainer = Color(0xFF1B1D17),   surfaceContainerHigh = Color(0xFF26291F),
    onSurface = Color(0xFFE4E6D8),          onSurfaceVariant = Color(0xFFC5C8B8),
    outline = Color(0xFF8F9284),
    background = Color(0xFF000000),         onBackground = Color(0xFFE4E6D8),
    error = Color(0xFFFFB4AB),              onError = Color(0xFF690005),
)

@Composable
fun VoltTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = VoltColorScheme, content = content)
```
Verifica los nombres de parámetros contra la versión de la librería del proyecto. Componentes
clave: `ScreenScaffold`, `TransformingLazyColumn`, `Button`/`FilledTonalButton`, `EdgeButton`,
`IconButton` (con variantes que se animan al pulsar), `ButtonGroup`,
`CircularProgressIndicator`, `ListHeader`.

### 9.3 Tiles (ProtoLayout Material 3)
Usa `primaryLayout` con `titleSlot`, `mainSlot` y `bottomSlot` (Edge Button). Respeta las
plantillas: los tiles se ven en carrusel junto a los del sistema y deben sentirse parte de él.
Proporciona variante para ≥225 dp si añades contenido.

### 9.4 Especificación para Figma
Lista: frames 192/225/240 dp, estilos de color con los nombres de rol, estilos de texto de la
sección 3, componentes (Chip, Button group, Edge Button, Data pill, Progress arc) con variantes
de estado (default, pressed, selected, disabled).

---

## 10. Checklist antes de entregar

- [ ] Fondo #000000 y una sola tarea clara por pantalla/tile.
- [ ] Un único elemento dominante en `primary`; el resto en azul/tonal.
- [ ] Ningún texto blanco sobre #009bfe ni sobre #cbfe00.
- [ ] Nada recortado por el círculo (revisa esquinas de chips y textos largos) a 192 y 240 dp.
- [ ] Áreas táctiles ≥ 48 dp; máximo un Edge Button.
- [ ] Iconos de un solo set, con relleno coherente y descripción accesible.
- [ ] Textos truncados con elipsis, máx. 2 líneas por chip, cifras tabulares.
- [ ] Estados (pressed/selected/disabled) definidos, el seleccionado también cambia de forma.
- [ ] Si es tile: sin icono de app dibujado dentro del diseño.
- [ ] Renderizado y revisado visualmente cuando sea posible.
