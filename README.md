# Ahorros

App de metas de ahorro con estética y movimiento **Material 3 Expressive**.
Sin frameworks, sin build, sin dependencias: HTML + CSS + JS (~25 KB en total).

## Qué incluye

- **Barra de progreso ondulada** (wavy progress de M3 Expressive): la onda fluye, crece con
  easing de resorte y muestra el indicador de parada al final. La bolita sigue el movimiento de
  la onda.
- Botón en la barra superior para **cambiar a un anillo circular** con el mismo estilo.
- **Animaciones fluidas**: ripple al tocar, entrada escalonada de tarjetas, hoja inferior con
  resorte, FAB que se deforma al presionar, confeti al completar una meta y números que cuentan
  hacia arriba.
- **Metas de ahorro** con aportes, historial de movimientos, progreso total y por meta.
- **Tema claro/oscuro** con transición suave.
- **Offline (PWA)**: se instala en la pantalla de inicio y funciona sin internet.
- Respeta `prefers-reduced-motion`.
- Datos guardados en el teléfono (`localStorage`), nada sale del dispositivo.

## Probarla en el computador

```powershell
node server.cjs
```

Abre <http://localhost:4173>.

## Probarla en el celular (misma red wifi)

1. Ejecuta `node server.cjs`.
2. Mira la IP local: `ipconfig` (por ejemplo `192.168.0.15`).
3. En el teléfono abre `http://192.168.0.15:4173`.

## Instalarla como app

Para que Chrome la ofrezca como app instalable (y funcione offline) necesita **HTTPS**.
La forma más rápida y gratuita:

1. Sube esta carpeta a un repositorio de GitHub.
2. Activa **GitHub Pages** (Settings → Pages → Deploy from branch → `main` / root).
3. Abre la URL `https://tu-usuario.github.io/tu-repo/` en el teléfono.
4. Menú de Chrome → **Instalar aplicación** (o "Añadir a pantalla de inicio").

Alternativas: Netlify Drop, Cloudflare Pages o Vercel (arrastrar la carpeta y listo).

## Convertirla en APK (Android) — automatizado

La app es una PWA. El repositorio incluye un workflow (`.github/workflows/build-and-release.yml`)
que, cada vez que haces `push` a `main`, **construye el APK automáticamente**, crea un Release en
GitHub con el APK adjunto y actualiza `version.json`. Así la app recibe la actualización al pulsar
"Buscar actualizaciones".

### Configuración inicial (una sola vez)

1. Sube el repo a GitHub y activa **GitHub Pages** (Settings → Pages → Source: *Deploy from a branch* → `main` / root).
2. Genera un keystore de firma (en tu PC, con Java instalado):
   ```bash
   keytool -genkeypair -v -keystore ahorros.jks -alias ahorros -keyalg RSA -keysize 2048 -validity 10000
   ```
3. Convierte el keystore a base64 y guárdalo como secreto:
   - Windows (PowerShell): `[Convert]::ToBase64String([IO.File]::ReadAllBytes("ahorros.jks")) | Out-File keystore.txt`
   - GitHub → tu repo → Settings → Secrets and variables → Actions → New repository secret:
     - `KEYSTORE_BASE64` = contenido de `keystore.txt`
     - `KEYSTORE_PASSWORD` = contraseña del keystore
     - `KEY_ALIAS` = `ahorros`
     - `KEY_PASSWORD` = contraseña de la clave
4. En `app.js` cambia `UPDATE_URL` por `https://TU-USUARIO.github.io/TU-REPO/version.json`.
5. En `version.json` ajusta la URL al patrón de tu repo.
6. Haz `push` a `main`. El workflow construye el APK y publica el Release.

### Flujo de actualización

1. Cambias `APP_VERSION` en `app.js` (ej. `"1.1.0"`), haces commit y push.
2. GitHub Actions construye el APK, crea el Release `v1.1.0` y actualiza `version.json`.
3. El usuario abre la app → Ajustes → Actualizaciones → **Buscar actualizaciones** → aparece
   **Descargar actualización** → instala el nuevo APK.

> Alternativa sin CI: usa https://www.pwabuilder.com/ (pega tu URL HTTPS → Android → descarga APK).

### Sistema de actualizaciones dentro de la app

En **Ajustes → Actualizaciones** la app comprueba un archivo `version.json` publicado por ti:

```json
{
  "version": "1.1.0",
  "url": "https://tu-servidor.com/ahorros-1.1.0.apk",
  "notes": "Nueva función X, corrección de Y."
}
```

Flujo:
1. Tú actualizas la app (cambias `APP_VERSION` en `app.js`, reconstruyes el APK).
2. Subes el nuevo APK y actualizas `version.json` (version + url + notes).
3. El usuario abre la app → Ajustes → Actualizaciones → **Buscar actualizaciones**.
4. Si la versión remota es mayor, aparece el botón **Descargar actualización**, que abre el APK para instalarlo.

> Recuerda cambiar `UPDATE_URL` en `app.js` por la URL real de tu `version.json`.

## Atajos de uso

- Toca una meta o **Añadir** para registrar un aporte.
- **Mantén presionada** una meta para ver opciones (añadir o eliminar).
- Botón **+** o **Nueva meta** para crear una meta.

## Archivos

| Archivo | Para qué |
| --- | --- |
| `index.html` | Estructura de la app |
| `styles.css` | Tokens de color, formas y todas las animaciones |
| `app.js` | Estado, cálculos, hojas de diálogo y efectos |
| `manifest.webmanifest` | Datos de instalación de la PWA |
| `sw.js` | Caché offline |
| `icon.svg` | Ícono de la app |
| `server.cjs` | Servidor estático mínimo para pruebas |

## Notas

- La moneda está fijada a **COP** (peso colombiano) en `app.js` → constante `nf`.
  Cámbiala por `USD`, `MXN`, `EUR`... y el formato se ajusta solo.
- Los datos de ejemplo se crean la primera vez. Puedes borrarlos manteniendo presionada cada meta.
