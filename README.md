# Cliente Android — Alerta Turística Inversa

Aplicación Kotlin con Jetpack Compose, MapLibre Native, Ktor Client y SQLDelight. SQLDelight mantiene una copia local de las zonas para que el último mapa consultado se pueda ver sin conexión.

## Funciones disponibles

- Mapa comunitario con zonas de riesgo y filtros por categoría.
- Buscador local de avisos y búsqueda manual de lugares de referencia como parques, iglesias o tiendas.
- Marcadores diferenciados para avisos, resultados de lugares y ubicación actual.
- Ubicación actual mediante GPS o red, con permiso solicitado al usuario al utilizar la función.
- Zoom mediante gestos y controles visibles de acercar/alejar.
- Listado de avisos con descripción, fecha, radio e intensidad.
- Formulario para publicar una zona seleccionando su ubicación directamente en el mapa.
- Vista previa completa del radio elegido antes de publicar el aviso.
- Categorías: inseguridad, accidente, riesgo vial y otro riesgo.
- Intensidad en tres niveles: precaución, alerta y alto riesgo.
- Radio configurable entre 50 y 1000 metros.
- Mensajes de carga, error y confirmación, además de caché local con SQLDelight.
- Tema claro u oscuro según la configuración del dispositivo.

## Configuración

1. Abre esta carpeta en Android Studio y deja que sincronice Gradle.
2. Comprueba que Android Studio haya creado `local.properties` (no se publica) con la ruta del SDK:
   ```properties
   sdk.dir=/ruta/al/Android/Sdk
   ```
3. El cliente apunta a `https://alerta-backend-production.up.railway.app`.
4. Despliega primero la versión actualizada del backend para habilitar el campo `category`.
5. Compila y ejecuta en un dispositivo o emulador. MapLibre usa el estilo libre de OpenFreeMap y no requiere una clave de Google.

Al actualizar una instalación existente, la migración `1.sqm` agrega la categoría al caché local sin borrar los avisos guardados.

## Búsqueda y datos del mapa

Los avisos se filtran localmente mientras se escribe. La búsqueda externa de lugares solo se ejecuta al pulsar el icono de buscar y usa el servicio público Nominatim de OpenStreetMap. La aplicación identifica sus solicitudes, conserva resultados repetidos en memoria y limita las consultas a menos de una por segundo. No se implementa autocompletado.

Los datos de lugares y el mapa requieren atribución a OpenStreetMap, mostrada dentro de la aplicación. Para una aplicación con tráfico real se debe sustituir el servicio público por un proveedor contratado, una instancia propia o un proxy controlado por el backend.

## Publicación

Inicializa esta carpeta como repositorio y publícala en GitHub con el nombre `alerta-android`. No subas `local.properties`.

## Cuentas y biometría

La sección **Cuenta** permite registrarse con nombre de usuario, alias y contraseña, iniciar sesión y recuperar la contraseña mediante una clave offline. La clave se muestra una sola vez después del registro y debe guardarse fuera de la aplicación. Solo una cuenta autenticada puede publicar reportes; el mapa y los avisos siguen siendo públicos.

Después de iniciar sesión, el usuario puede activar el acceso con la biometría fuerte ya registrada en Android. El sistema operativo decide si muestra huella o reconocimiento facial según el dispositivo. La aplicación nunca recibe, almacena ni envía imágenes del rostro, huellas o plantillas biométricas: solo recibe el resultado correcto o incorrecto del diálogo del sistema.

El token de sesión se cifra con una clave AES-GCM mantenida en Android Keystore. Las copias de seguridad de la aplicación están desactivadas para evitar trasladar datos cifrados sin su clave. La contraseña no se guarda en el teléfono.

Para que el registro y la recuperación funcionen, despliega primero el backend actualizado y configura `AUTH_SECRET` en Railway. No se necesita un dominio de correo ni una cuenta de Resend.
