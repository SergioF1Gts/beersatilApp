# 🐝 BeerSatil - Mi Checklist Personal de Validación (Stage 3 - Versión Java)

¡Hola! Este documento es tu guía paso a paso y lista de verificación personal para validar que la app **BeerSatil** (desarrollada en **Java** con XML Views, RecyclerView, CardView, TextInputLayout y Glide) cumple con todos los requisitos del Stage 3.

---

## ✅ Lista de Verificación (Checklist) para Validar en el Emulador

### 1. Inicio de Sesión y Control de Acceso (Auth + RBAC + TextInputLayout)
- [ ] **Apertura de la App:** La app abre correctamente en `LoginActivity`.
- [ ] **Validación en Tiempo Real:** Deja el correo o contraseña vacíos e intenta iniciar sesión; comprueba que `TextInputLayout.setError()` muestra los mensajes de error en rojo.
- [ ] **Credenciales de Administrador:** Haz clic en *"Fill Admin"* (`admin@beersatil.com`) e inicia sesión. Verifica que al entrar a `MainActivity` puedes ver el ícono de papelera (eliminar) en las tarjetas (`CardView`) de tareas.
- [ ] **Cierre de Sesión:** Ve al menú **Profile** (`ProfileActivity`) y haz clic en *"Sign Out / Secure Logout"*. Verifica que te regresa al login y borra la sesión.
- [ ] **Credenciales de Operador:** Haz clic en *"Fill Operator"* (`operator@beersatil.com`) e inicia sesión. Verifica que **no** aparece el botón de eliminar (RBAC funcionando).

### 2. Módulo de Datos y Multimedia (RecyclerView + CardView + Glide)
- [ ] **Lista en RecyclerView:** En la pantalla principal, visualiza las tareas cargadas en la lista con sus tarjetas (`CardView`).
- [ ] **Crear Tarea con Foto:**
  - Haz clic en el botón flotante `+` (`FAB`) para abrir `AddTaskActivity`.
  - Rellena título y descripción (con validación `TextInputLayout`).
  - Selecciona una imagen de la galería usando `ActivityResultLauncher`.
  - Guarda la tarea.
- [ ] **Renderizado con Glide:** Verifica que la imagen se carga correctamente en la tarjeta con caché y placeholder.

### 3. Módulo de Notificaciones (Local)
- [ ] **Notificación en Barra de Estado:** Al guardar la tarea, verifica que apareció una notificación en la barra superior de tu dispositivo diciendo *"Task Created"*. Al hacer clic en ella, reabre la app.

### 4. Módulo de Geolocalización y Mapas (GPS + Google Maps)
- [ ] **Captura de GPS:** Al crear una tarea, observa que captura automáticamente las coordenadas y la dirección física.
- [ ] **Mapa Interactivo:** Haz clic en el botón **Map** (`MapsActivity`). Verifica que el mapa muestra los pines en las ubicaciones de las tareas y prueba el botón *"Satellite View"* para alternar entre capas.

---

## 📌 Datos de Acceso Rápidos
| Rol | Correo | Contraseña | Permisos |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin@beersatil.com` | `password123` | Gestión completa y borrado |
| **Operator** | `operator@beersatil.com` | `password123` | Creación y visualización |
