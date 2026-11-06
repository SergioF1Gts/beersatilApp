# 🐝 BeerSatil

**Gestión profesional de apiarios, colmenas e incidencias en terreno.**
Aplicación móvil nativa para Android (Java + XML) que conecta a los operarios de campo con los administradores de la empresa apícola.

> Proyecto Integrador Final · Momento 3 · CESDE · Tecnología en Desarrollo de Software
> Autor: Sergio Campos Garcia*
 
---

##  Problema que resuelve

El control tradicional de los colmenares en zonas rurales se lleva en papel o en registros desconectados. Eso dificulta:

- hacer seguimiento a la salud de las colmenas,
- ubicar con exactitud los problemas (plagas, falta de alimento, tratamientos pendientes),
- tener evidencia visual de lo que pasa en el terreno,
- coordinar al personal y saber quién hizo qué.
##  Solución

BeerSatil digitaliza todo ese flujo en tiempo real:

- Los **operarios** registran tareas e incidentes con foto y ubicación GPS, y solo ven sus propios registros.
- Los **administradores** ven el trabajo de todo el equipo, tienen un panel de resumen y son los únicos que pueden eliminar registros.
- Todo se sincroniza al instante en la nube y se visualiza en un mapa interactivo.
---

##  Credenciales de prueba

Estos usuarios ya existen en el proyecto de Firebase configurado, con su rol asignado en Firestore.

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `beer@gmail.com` | `123456` |
| Operario (estándar) | `operator@beersatil.com` | `password123` |

En la pantalla de inicio de sesión hay dos botones de "Credenciales de prueba" que rellenan estos datos automáticamente.

> Son credenciales de demostración, solo para evaluar el proyecto.
> Cualquier usuario nuevo que se registre desde la app queda como **Operario**. El rol de Administrador se asigna manualmente desde la consola de Firestore (`users/{uid}.role = "admin"`).
 
---

##  Nota sobre los servicios utilizados

Google no aceptó mi tarjeta para activar la cuenta de facturación que exigen **Firebase Storage** y el **SDK de Google Maps**. Por eso, esos dos componentes se resolvieron con alternativas gratuitas que cumplen la misma función:

| El enunciado pide | Lo que se implementó | Dónde |
|---|---|---|
| Firebase Storage (archivos binarios, progreso y *Download URL*) | **Cloudinary** (subida *unsigned*, con barra de progreso y URL pública guardada en Firestore) | `helper/ImageUploader.java` |
| Google Maps SDK (pines, zoom, capas Normal/Satélite, InfoWindow) | **Leaflet + OpenStreetMap** (capa satélite Esri) dentro de un `WebView` | `MapsActivity.java` y `assets/map.html` |



##  Funcionalidades (los 5 pilares)

### A. Seguridad y control de acceso
- Registro e inicio de sesión con **FirebaseAuth** (correo y contraseña), con mensajes de error en español.
- **Auto-login:** si hay una sesión activa, la app salta el Login y abre la lista (sesión en `SharedPreferences`).
- **Cierre de sesión seguro:** ejecuta `FirebaseAuth.signOut()`, limpia `SharedPreferences` y vuelve al Login con `CLEAR_TASK | NEW_TASK` (el botón "atrás" no regresa a la sesión).
- **RBAC:** el rol se lee de Firestore (`users/{uid}.role`) después del login. No se elige en la pantalla de acceso.
   - *Administrador:* ve las tareas de todos, el panel de resumen, y puede eliminar.
   - *Operario:* ve y edita solo sus tareas, sin botón de eliminar.
   - El control también se aplica en el servidor mediante las **reglas de seguridad de Firestore**.
### B. Datos y multimedia
- **CRUD en tiempo real** de tareas en Firestore con `addSnapshotListener`.
- Cada tarea tiene tipo (Revisión de colmena, Alimentación, Cosecha de miel, Plaga/Incidente, Tratamiento sanitario), estado (Pendiente, En proceso, Resuelta), colmena/apiario, descripción, foto y ubicación.
- **Cámara o galería** con `ActivityResultLauncher`, con permiso de cámara en tiempo de ejecución y manejo del caso en que se niega.
- **Subida de la foto** con compresión, corrección de orientación, barra de progreso y obtención de la URL pública.
- **Glide** en las tarjetas del `RecyclerView`: caché en disco, *placeholder*, imagen de error y prevención del bug de reciclaje (se carga o se limpia la imagen en cada `onBindViewHolder`).
- Buscador con normalización de acentos y chip de color según el estado.
### C. Geolocalización y mapas
- Coordenadas con **FusedLocationProviderClient** (permisos precisos y aproximados).
- **Geocodificación inversa** con `Geocoder` (calle, barrio y ciudad) en segundo plano. Si se niega el permiso, la tarea se guarda con "Ubicación no disponible".
- **Mapa interactivo** con:
   - pines de color según el tipo de tarea, con leyenda,
   - ventana de información al tocar un pin (título, tipo, estado, dirección, autor y foto),
   - agrupación de pines cercanos o superpuestos (*clustering*),
   - controles de zoom, botón "Ver todos" y alternancia **Normal / Satélite**.
### D. Notificaciones
- `NotificationChannel` para Android 8+ y permiso `POST_NOTIFICATIONS` para Android 13+.
- **Notificación local** al crear o modificar una tarea, con `PendingIntent` que reabre la app.
- **Firebase Cloud Messaging:** `MyFirebaseMessagingService` declarado en el Manifest, listo para recibir push desde la consola de Firebase.
### E. Diseño y buenas prácticas
- Soporte **horizontal (landscape)** con `ScrollView` y `AppBarLayout` colapsable, sin cortes ni superposiciones.
- **Validaciones en tiempo real** con `TextInputLayout.setError()` (campos vacíos, correo inválido, contraseña corta, título y descripción obligatorios).
- Manejo de los márgenes del sistema (`fitsSystemWindows`).
---

##  Tecnologías

- Java 11, XML, Material Components, RecyclerView, CardView
- Firebase Authentication, Cloud Firestore, Cloud Messaging
- Google Play Services Location (`FusedLocationProviderClient`)
- Glide 4.16
- Cloudinary (subida *unsigned* vía HTTP)
- Leaflet 1.9, Leaflet.markercluster, OpenStreetMap y Esri World Imagery
- `minSdk 24`
---

##  Instalación y ejecución

1. Clona el repositorio:
```bash
   git clone <URL-DEL-REPOSITORIO>
```
2. Ábrelo con **Android Studio** (versión reciente) y espera a que termine la sincronización de Gradle.
3. Verifica que exista `app/google-services.json` (archivo de configuración de Firebase). **[ajusta: indica si va incluido en el repositorio o cómo se entrega]**
4. Ejecuta la app en un **emulador con Google Play** (API 24 o superior) o en un celular con Android. Se requiere conexión a internet.
5. Inicia sesión con las credenciales de prueba.
### Configuración de Cloudinary
Las constantes están al inicio de `app/src/main/java/com/example/beersatil/helper/ImageUploader.java`:

```java
private static final String CLOUD_NAME = "knsaomho";
private static final String UPLOAD_PRESET = "beersatil";   // preset "unsigned"
```

No se usa ninguna clave secreta en la app.

### Si se quiere montar en otro proyecto de Firebase
1. Crear un proyecto, registrar la app con el paquete `com.example.beersatil` y descargar su `google-services.json` en `app/`.
2. Activar **Authentication → Correo y contraseña**.
3. Crear la base de datos **Cloud Firestore** y publicar las reglas descritas abajo.
4. Crear los usuarios de prueba en Authentication y, para cada uno, un documento `users/{uid}` con los campos `email` y `role` (`admin` u `operator`).
---

## 🗄️ Modelo de datos (Firestore)

**`users/{uid}`**

| Campo | Tipo | Descripción |
|---|---|---|
| `email` | string | Correo del usuario |
| `role` | string | `admin` u `operator` |
| `createdAt` | timestamp | Fecha de registro |

**`tasks/{id}`**

| Campo | Tipo | Descripción |
|---|---|---|
| `title`, `description` | string | Título y descripción |
| `type` | string | Tipo de tarea o incidente |
| `status` | string | Pendiente, En proceso o Resuelta |
| `apiary` | string | Colmena o apiario (opcional) |
| `imageUrl` | string | URL pública de la foto (Cloudinary) |
| `latitude`, `longitude` | number | Coordenadas GPS (`0, 0` = sin ubicación) |
| `address` | string | Dirección obtenida con Geocoder |
| `authorEmail`, `authorUid`, `role` | string | Autor y su rol |
| `timestamp` | number | Fecha de creación (milisegundos) |

##  Reglas de seguridad de Firestore (resumen)

- Solo usuarios autenticados pueden operar.
- Cada usuario puede leer únicamente su propio documento en `users`, y solo puede crearlo con el rol `operator`.
- **Operario:** lee, crea y edita únicamente las tareas cuyo `authorUid` es el suyo. No puede cambiar el autor ni eliminar.
- **Administrador:** lee y edita todas las tareas, y es el único que puede eliminarlas.
---

##  Permisos de la app

| Permiso | Para qué | Cuándo se pide |
|---|---|---|
| `CAMERA` | Tomar la foto de evidencia | Al elegir "Tomar foto" |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Ubicación de la tarea | Al crear una tarea o pulsar "Actualizar GPS" |
| `POST_NOTIFICATIONS` | Notificaciones (Android 13+) | Al abrir la lista de tareas |
| `INTERNET` | Firebase, Cloudinary y mapa | No requiere solicitud |

Si el usuario niega un permiso, la app no se rompe: la tarea se guarda sin GPS, se ofrece la galería en vez de la cámara, o simplemente no se muestran notificaciones.
 
---

## 🏛 Estructura del proyecto

```
com.example.beersatil
├── BeerSatilApp.java              Inicializa Firebase y el canal de notificaciones
├── LoginActivity.java             Login, registro y validaciones en tiempo real
├── MainActivity.java              Lista, buscador, panel de resumen (admin) y permisos
├── AddTaskActivity.java           Crear/editar tarea: foto, GPS y validaciones
├── TaskDetailActivity.java        Detalle de la tarea con foto grande
├── MapsActivity.java              Mapa Leaflet con los pines de Firestore
├── ProfileActivity.java           Perfil y cierre de sesión
├── adapter/
│   └── TaskAdapter.java           Tarjetas del RecyclerView (Glide, RBAC)
├── helper/
│   ├── AuthManager.java           Auth, roles y sesión
│   ├── FirestoreRepository.java   CRUD y sincronización en tiempo real
│   ├── ImageUploader.java         Subida de fotos a Cloudinary con progreso
│   ├── LocationHelper.java        GPS y geocodificación inversa
│   ├── NotificationHelper.java    Canal y notificaciones locales
│   ├── MyFirebaseMessagingService.java   Recepción de push (FCM)
│   └── StatusStyle.java           Chips de color por estado
└── model/
    └── TaskItem.java              Modelo de tarea
assets/map.html                    Mapa Leaflet (pines, colores, clustering, capas)
```
 
---

##  Cumplimiento del enunciado

| Requisito | Dónde está |
|---|---|
| FirebaseAuth (correo y contraseña) | `AuthManager`, `LoginActivity` |
| Auto-login con SharedPreferences | `AuthManager.isLoggedIn()`, `LoginActivity` |
| Logout seguro | `ProfileActivity`, `AuthManager.logout()` |
| RBAC | `AuthManager`, `MainActivity`, `TaskAdapter`, reglas de Firestore |
| CRUD en tiempo real (`addSnapshotListener`) | `FirestoreRepository`, `AddTaskActivity`, `MainActivity` |
| Cámara/Galería con `ActivityResultLauncher` y permisos | `AddTaskActivity` |
| Subida con progreso y URL | `ImageUploader` (Cloudinary, en lugar de Firebase Storage) |
| Glide con caché, placeholder, error y sin bug de reciclaje | `TaskAdapter` |
| `FusedLocationProviderClient` y `Geocoder` | `LocationHelper` |
| Mapa con pines, zoom, Normal/Satélite e InfoWindow | `MapsActivity`, `assets/map.html` (Leaflet, en lugar de Google Maps SDK) |
| `NotificationChannel` y `POST_NOTIFICATIONS` | `NotificationHelper`, `MainActivity` |
| Notificación local con `PendingIntent` | `NotificationHelper`, `AddTaskActivity` |
| `FirebaseMessagingService` | `MyFirebaseMessagingService`, `AndroidManifest.xml` |
| Landscape | Layouts con `ScrollView` y `AppBarLayout` |
| Validaciones con `setError()` | `LoginActivity`, `AddTaskActivity` |
 
---

##  Cómo probarla (flujo sugerido)

1. Abre la app y prueba las validaciones: correo inválido y contraseña corta.
2. Entra como **operario** (botón de credenciales de prueba).
3. Crea una tarea con foto (cámara o galería) y ubicación. Observa la barra de progreso y la notificación.
4. Abre el detalle, edítala y verifica la segunda notificación.
5. Abre el **mapa**: pines de color, ventana de información, Satélite/Normal y "Ver todos".
6. Cierra sesión desde **Perfil** y comprueba que el botón "atrás" no regresa.
7. Entra como **administrador**: ve las tareas de todos, el panel de resumen, el buscador y el botón de eliminar.
8. Gira el dispositivo en cada pantalla (landscape).
9. Niega cada permiso (cámara, ubicación y notificaciones) y verifica que la app no se rompe.
## ℹNotas

- El mapa y la subida de fotos requieren conexión a internet.
- Para probar las notificaciones push desde la consola de Firebase, el emulador debe tener Google Play. El token del dispositivo aparece en Logcat con la etiqueta `BeerSatilFCM`.
---

🐝 *BeerSatil: trazabilidad por GPS, evidencia fotográfica y control de personal por roles, en tiempo real.*