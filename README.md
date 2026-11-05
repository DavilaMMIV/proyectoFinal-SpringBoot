# Habita · Administración de condominios

Aplicación Spring Boot con Java 21, Spring Security, Thymeleaf, JPA, H2 persistente y reportes PDF con OpenPDF.

## Ejecutar

Requisito: Java 21. Incluye Maven Wrapper (`mvnw` para Linux/macOS y `mvnw.cmd` para Windows); la primera ejecución necesita internet para descargar Maven y las dependencias.

```bash
export ADMIN_CORREO='admin@habita.local'
read -rsp 'Contraseña inicial del administrador (mínimo 12 caracteres): ' ADMIN_CONTRASENA
export ADMIN_CONTRASENA
./mvnw spring-boot:run
```

Abre http://localhost:8080. El administrador se crea la primera vez que se proporciona `ADMIN_CONTRASENA`; después se conserva la contraseña guardada. Sin esa variable no se crea una cuenta administrativa. No hay contraseñas predeterminadas. El registro web solo crea usuarios normales.

## Funciones

- Catálogo de seis condominios de ejemplo con precio mensual en MXN, superficie, recámaras y disponibilidad.
- Registro con nombre completo, correo único, teléfono y contraseña cifrada con BCrypt.
- Inicio de sesión y cierre de sesión con protección CSRF.
- Alquiler con fecha de inicio, bloqueo transaccional para impedir alquileres duplicados y folio UUID único.
- Usuario: consulta de sus propios alquileres y folios de identificación.
- Administrador: directorio de residentes con datos de contacto, estadísticas y reporte PDF multipágina.
- Datos guardados localmente en `data/condominios.mv.db` entre reinicios.

## Control de entradas y salidas

- El residente abre **Mis visitas y accesos** desde su espacio, autoriza una visita para un condominio propio y una fecha, y comparte el código generado. Puede cancelar autorizaciones sin utilizar.
- El administrador abre **Control de entradas y salidas**, comprueba la identidad y registra la entrada mediante el código del visitante o seleccionando al residente.
- Cada visita permite una sola entrada el día autorizado. Se rechazan códigos vencidos, cancelados o utilizados, alquileres futuros y entradas repetidas de residentes sin salida.
- El administrador registra la salida y consulta el historial con fecha, hora y operador. Cada residente solo ve sus propios accesos. Las operaciones conservan CSRF y permisos por rol.
- Las horas usan la zona horaria del servidor; configura `TZ=America/Mexico_City` al ejecutarlo si corresponde a tu ubicación.
- El panel de operación está reservado al administrador; no se crea una cuenta independiente de vigilancia. El registro es manual y no acciona cerraduras ni torniquetes.

El folio identifica el alquiler; no implementa control físico de puertas ni es una contraseña. No se procesan pagos, contratos legales, vencimientos ni cancelaciones. Los importes del panel son rentas registradas, no pagos cobrados. La identidad del usuario se toma de su cuenta al alquilar. Este proyecto está preparado para ejecución local; para publicarlo se necesita alojamiento Java, HTTPS y configuración de base de datos y respaldos.

## Pruebas y empaquetado

```bash
./mvnw verify
java -jar target/condominios-1.0.0.jar
```

Las pruebas usan una base de datos en memoria y verifican las páginas públicas, el registro, CSRF, los permisos, la generación PDF, las fechas y el rechazo de alquileres duplicados.

## Estructura

Las clases en `src/main/java/mx/condominios/` siguen los mismos nombres de carpetas que el proyecto de pizzas:

- `controllers/`: controladores web y del control de accesos.
- `dtos/`: formulario de registro y sus validaciones.
- `entities/`: entidades JPA de cuentas, condominios, alquileres y accesos.
- `businessLayer/`: servicios con reglas de negocio y transacciones.
- `repositories/`: repositorios de acceso a la base de datos.

`Aplicacion`, `ConfiguracionSeguridad` y `DatosIniciales` permanecen en el paquete raíz `mx.condominios`. Spring descubre los componentes, entidades y repositorios de los subpaquetes desde la clase principal.

- `src/main/resources/templates`: pantallas en español.
- `src/main/resources/static/estilos.css`: diseño adaptable a móviles e ilustraciones CSS.
- `src/main/resources/application.properties`: base de datos y configuración administrativa.

## Nombres en español

Las clases, atributos, métodos propios, plantillas y estilos usan nombres en español. Los paquetes `controllers`, `dtos`, `entities`, `businessLayer` y `repositories` conservan los nombres de carpetas del proyecto de pizzas. La clase principal es `mx.condominios.Aplicacion`; el acceso es `/iniciar-sesion` y el panel administrativo está en `/administracion`. Se conservan los nombres exigidos por Java, Spring, Maven y sus archivos de configuración. Los nombres físicos de tablas y columnas se mantienen mediante anotaciones JPA para conservar los datos existentes.
