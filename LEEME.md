# Tablerista — app Android

Diseñador de tableros eléctricos: dispositivos realistas, cableado editable, medidas de cable, chequeo de normas AEA y QR para pegar en el tablero.

## Qué hay en esta carpeta

| Carpeta | Qué es |
|---|---|
| `web/` | La aplicación (un solo `index.html`). Se usa igual en el navegador y dentro de la app. |
| `web/config.js` | Los 3 datos para que funcione el QR online. **Es lo único que tenés que completar.** |
| `android/` | La app Android: una pantalla que abre `web/` sin internet y agrega guardar y compartir archivos. |
| `supabase/tablas.sql` | La base de datos donde se guardan los tableros publicados con QR. |
| `.github/workflows/compilar.yml` | Hace que GitHub compile el APK solo y publique la web en cada cambio. |

## Cómo funciona todo junto

1. Subís esta carpeta a un repositorio de GitHub.
2. GitHub compila el APK y publica la web en `https://TU-USUARIO.github.io/tablerista/`.
3. Desde el celu entrás a esa dirección y descargás la app con el botón **App Android** (o directo en `.../tablerista.apk`).
4. En la app armás el tablero y tocás **Código QR → Publicar**. El tablero se guarda en Supabase.
5. El QR apunta a la web: el cliente lo escanea y ve el plano sin instalar nada ni tener cuenta.

## Paso 1 — Supabase (una sola vez)

1. Entrá a supabase.com y creá un proyecto nuevo, por ejemplo `tablerista`. Sirve el plan gratis.
2. Andá a **SQL Editor → New query**, pegá todo el contenido de `supabase/tablas.sql` y tocá **Run**.
3. Andá a **Project Settings → API** y copiá:
   - **Project URL** (algo como `https://abcdefgh.supabase.co`)
   - **anon public key**
4. Pegá esos dos datos en `web/config.js`. La clave *anon* es pública por diseño: no pasa nada porque quede en el código. La protección está en la base: cada tablero se puede modificar solo con la clave del equipo que lo publicó.

## Paso 2 — GitHub

1. Creá un repositorio **público** llamado `tablerista`. Tiene que ser público para que GitHub Pages sea gratis.
2. Subí todo el contenido de esta carpeta, incluida la carpeta oculta `.github`.
3. En el repo: **Settings → Pages → Build and deployment → Source: GitHub Actions**.
4. Si tu usuario no es `chacalumes2653` o el repo no se llama `tablerista`, corregí `siteUrl` en `web/config.js`.
5. Andá a la pestaña **Actions**: la compilación tarda unos 3 a 5 minutos. Cuando termina en verde, la web y el APK ya están publicados.

Cada vez que cambies algo y lo subas, se compila una versión nueva. El número de versión sube solo, y el APK nuevo se instala encima del anterior sin perder los tableros guardados.

## Paso 3 — Instalar en el celular

1. Abrí `https://TU-USUARIO.github.io/tablerista/tablerista.apk` en el celu.
2. Android te va a pedir permiso para **instalar apps de origen desconocido** desde el navegador: aceptalo.
3. Abrí **Tablerista** desde el menú de apps.

## Qué hace la app además de la web

- Funciona **sin internet**. Solo publicar el QR y abrir tableros publicados necesitan conexión.
- **Guardar** imágenes y etiquetas QR en *Galería › Tablerista* y los proyectos `.json` en *Descargas › Tablerista*.
- **Compartir** el plano o la etiqueta QR por WhatsApp o cualquier app.
- El botón **Atrás** del celu cierra las ventanas abiertas antes de salir.

## Publicar en Google Play (más adelante)

1. Cuenta de desarrollador de Google Play: US$ 25, un solo pago.
2. Play pide el formato **AAB** en lugar de APK: se cambia `assembleRelease` por `bundleRelease` en el workflow.
3. La llave de firma **no está en el repo** (sería pública). GitHub Actions la arma desde dos *secrets* (Settings → Secrets and variables → Actions): `TABLERISTA_KEYSTORE_B64` (el archivo `tablerista.keystore` en base64) y `TABLERISTA_KEYSTORE_PASS` (su clave). Sin ellos el APK se firma con una llave de prueba. Para Play Store usá una llave nueva, distinta de esta.
4. Play pide política de privacidad, capturas y ficha de la app. Te las puedo preparar.

## Si algo falla

- **La compilación sale en rojo:** abrí el paso que falló en *Actions* y copiá el error.
- **"La publicación online no está configurada":** falta completar `web/config.js`.
- **"No se pudo publicar":** revisá que corriste `tablas.sql` completo y que la URL y la clave de Supabase estén bien pegadas.
