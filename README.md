# Sistema de Gestión de Prácticas Empresariales — UAH

Sistema web institucional para la gestión integral del ciclo de vida de las prácticas empresariales de la Universidad Antonio Nariño (UAH). Administra desde la evaluación de aptitud del estudiante hasta el cierre formal de la práctica, integrando todos los actores del proceso: estudiantes, docentes asesores, tutores empresariales, coordinadores, secretaría y dirección.

---

## Tabla de Contenidos

- [Propósito](#propósito)
- [Funcionalidades principales](#funcionalidades-principales)
- [Roles del sistema](#roles-del-sistema)
- [Flujo de una práctica](#flujo-de-una-práctica)
- [Arquitectura](#arquitectura)
- [Stack tecnológico](#stack-tecnológico)
- [Patrones de diseño](#patrones-de-diseño)
- [Requisitos previos](#requisitos-previos)
- [Configuración local](#configuración-local)
- [Variables de entorno](#variables-de-entorno)
- [Despliegue en producción](#despliegue-en-producción)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Pruebas](#pruebas)
- [Equipo](#equipo)

---

## Propósito

El sistema centraliza y automatiza el proceso de prácticas empresariales, eliminando el seguimiento manual por correo y planillas. Permite:

- **Coordinar** la vinculación entre estudiantes aptos, empresas con vacantes y tutores empresariales.
- **Hacer seguimiento** en tiempo real del avance de cada práctica a través de un flujo de estados controlado.
- **Evaluar** el desempeño del estudiante desde tres perspectivas: docente asesor, tutor empresarial y coordinación académica.
- **Gestionar** la documentación requerida (ARL, planeador, informes) con aprobación digital.
- **Generar** el paz y salvo automáticamente cuando todos los requisitos de cierre están completos.
- **Reportar** indicadores globales a la dirección académica con filtros por facultad y programa.

---

## Funcionalidades principales

### Gestión de usuarios y acceso
- Creación de usuarios con 9 roles diferenciados y envío automático de credenciales por correo.
- Autenticación con JWT, expiración de token y cambio de contraseña obligatorio en primer inicio.
- Recuperación de contraseña mediante token de un solo uso enviado al correo institucional.

### Ciclo de vida de la práctica
- Evaluación de aptitud del estudiante mediante motor de reglas (Chain of Responsibility).
- Creación automática de la instancia de práctica al marcar al estudiante como apto.
- Flujo de estados controlado con transiciones válidas (patrón State).
- Asignación de empresa, tutor empresarial y docente asesor desde el sistema.

### Seguimiento académico
- Registro de avances periódicos por parte del estudiante con archivo adjunto opcional.
- Retroalimentación del docente sobre cada avance con cambio de estado a revisado.
- Visualización de avances por el tutor empresarial en modo lectura.

### Evaluación y notas
- Registro independiente de nota por parte del docente asesor y del tutor empresarial.
- Cálculo de nota final con comparación contra la nota mínima de aprobación del programa.
- Determinación automática del resultado: APROBADO o REPROBADO al ejecutar el cierre.

### Documentos y paz y salvo
- Carga de documentos obligatorios (ARL, Planeador, Informe Ejecutivo, Presentación, Documento Final) a Firebase Storage.
- Aprobación de documentos por el docente asesor desde el sistema.
- Checklist de paz y salvo calculado en tiempo real con 7 requisitos: notas, encuestas, documentos e informe final.
- Cierre formal de práctica habilitado únicamente cuando todos los requisitos están completos.

### Encuestas de satisfacción
- Plantillas de encuesta configurables (secciones, preguntas de escala 1-5, texto libre y sí/no).
- Encuesta del estudiante disponible durante la práctica activa.
- Evaluación del tutor empresarial sobre el desempeño del practicante.
- Estadísticas de satisfacción por programa disponibles en el dashboard de dirección.

### Vacantes y postulaciones
- Publicación de vacantes por las empresas vinculadas con información de perfil requerido.
- Aprobación o rechazo de vacantes por el coordinador empresarial.
- Postulación de estudiantes aptos a vacantes aprobadas con seguimiento de estados.
- Vista de candidatos por empresa con historial de postulaciones.

### Reportes y dirección
- Dashboard de dirección con KPIs globales: estudiantes en práctica, tasa de aprobación, promedio de notas y satisfacción.
- Gráficos de distribución por programa, estado y resultado.
- Filtros por facultad, programa académico y número de práctica.
- Tabla de detalle por programa con barra de porcentaje de aprobación.

### Contratos y visitas
- Generación de contrato de práctica en PDF (patrón Builder + Adapter).
- Registro de visitas a empresa con diferenciación de rol (coordinador o docente).

---

## Roles del sistema

| Rol | Descripción |
|-----|-------------|
| `ADMINISTRADOR` | Gestión total del sistema, usuarios y configuración. |
| `COORDINADOR_ACADEMICO` | Clasificación de estudiantes, asignación de docentes, catálogos de práctica. |
| `COORDINADOR_PRACTICA` | Gestión de empresas, vacantes, vinculación, cierre formal y reportes. |
| `SECRETARIA_COORDINACION` | Acceso de lectura al módulo del coordinador sin aprobar vacantes ni postular. |
| `DOCENTE_ASESOR` | Seguimiento de sus estudiantes, aprobación de documentos, registro de notas y retroalimentación de avances. |
| `TUTOR_EMPRESARIAL` | Evaluación del practicante, registro de nota y visualización de avances. |
| `EMPRESA_VINCULADA` | Publicación de vacantes, gestión de tutores y seguimiento de candidatos. |
| `ESTUDIANTE` | Registro de avances, carga de documentos, encuesta de satisfacción y consulta de paz y salvo. |
| `DIRECCION` | Visualización de dashboard global con KPIs e indicadores por facultad y programa. |

---

## Flujo de una práctica

```
Coordinador Académico evalúa aptitud del estudiante
        │
        ▼  Estado: ASIGNADA_PENDIENTE_INICIO
Estudiante marcado APTO → práctica creada automáticamente
        │
        ▼  Estado: EN_PROCESO_VINCULACION
Coordinador Empresarial asigna empresa y tutor
        │
        ▼  Estado: VINCULADA
Convenio registrado con la empresa
        │
        ▼  Estado: EN_PRACTICA
Estudiante en ejecución:
  - Registra avances periódicos
  - Carga documentos (ARL, Planeador, Informe, Presentación)
  - Responde encuesta de satisfacción
Docente Asesor:
  - Retroalimenta avances
  - Aprueba documentos
  - Registra nota del docente
Tutor Empresarial:
  - Evalúa al practicante
  - Registra nota del tutor
        │
Coordinador verifica checklist completo:
  ✓ Nota docente registrada
  ✓ Nota tutor registrada
  ✓ Nota final registrada
  ✓ Encuesta estudiante completada
  ✓ Encuesta tutor completada
  ✓ Documentos aprobados (ARL + Planeador + Informe + Presentación)
  ✓ Informe final aprobado
        │
        ▼  Cierre formal ejecutado
notaFinal >= notaMinima → COMPLETADA (APROBADO)
notaFinal <  notaMinima → REPROBADA  (REPROBADO)
```

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────┐
│                     FRONTEND (Vercel)                   │
│   React 18 · Vite · Tailwind CSS · Zustand · React Query│
│   Axios · React Router v6 · Recharts · Shadcn/ui        │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTPS / REST API
┌──────────────────────▼──────────────────────────────────┐
│                    BACKEND (Render)                     │
│       Spring Boot 3.3 · Java 21 · Spring Security       │
│       JWT · Flyway Migrations · Firebase Storage SDK    │
└──────────────────────┬──────────────────────────────────┘
                       │ JDBC / SSL
┌──────────────────────▼──────────────────────────────────┐
│               BASE DE DATOS (Aiven MySQL 8)             │
│        Cloud managed · SSL · Backups automáticos        │
└─────────────────────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│              FIREBASE STORAGE (Google Cloud)            │
│    Documentos de práctica · Hojas de vida · Archivos    │
└─────────────────────────────────────────────────────────┘
```

---

## Stack tecnológico

### Backend
| Tecnología | Versión | Uso |
|-----------|---------|-----|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.3 | Framework web |
| Spring Security | 6.x | Autenticación y autorización |
| JWT (jjwt) | 0.11+ | Tokens de acceso |
| Flyway | 9.x | Migraciones de base de datos |
| MySQL | 8.0 | Base de datos relacional |
| Firebase Admin SDK | 9.x | Almacenamiento de archivos |
| JavaMail / Spring Mail | — | Envío de correos SMTP |
| Lombok | — | Reducción de boilerplate |
| OpenAPI / Swagger | 3.x | Documentación de API |

### Frontend
| Tecnología | Versión | Uso |
|-----------|---------|-----|
| React | 18 | Librería de UI |
| Vite | 5.x | Build tool |
| Tailwind CSS | 3.x | Estilos utilitarios |
| Zustand | 4.x | Estado global (auth store) |
| React Query | 5.x | Caché y sincronización de datos |
| React Router | 6.x | Enrutamiento SPA |
| Axios | 1.x | Cliente HTTP |
| Recharts | 2.x | Gráficos del dashboard |
| Shadcn/ui | — | Componentes base |
| Sonner | — | Notificaciones toast |
| Lucide React | — | Iconografía |

### Infraestructura
| Servicio | Uso |
|---------|-----|
| Render | Despliegue del backend (Spring Boot JAR) |
| Vercel | Despliegue del frontend (React SPA) |
| Aiven | MySQL 8 administrado en la nube con SSL |
| Firebase Storage | Almacenamiento de documentos y archivos |
| GitHub Actions | CI/CD — 100 casos de prueba automatizados |

---

## Patrones de diseño

El sistema implementa múltiples patrones de diseño orientados a objetos:

- **State** — `Practica.java` delega el comportamiento según su estado actual (`EstadoEnPractica`, `EstadoCompletada`, etc.). Cada estado controla qué transiciones son válidas.
- **Facade** — Cada módulo expone una fachada (`EstudianteFacade`, `PracticaFacade`, `ConfiguracionFacade`) que oculta la complejidad interna al controlador.
- **Chain of Responsibility** — Motor de evaluación de aptitud del estudiante. Cada regla académica es un eslabón de la cadena.
- **Builder** — Construcción del acta de cierre (`ActaCierreBuilder`) y del contrato de práctica (`ContratoBuilder`).
- **Adapter** — `ArchivoStorageService` abstrae Firebase Storage detrás de una interfaz genérica.
- **Observer** — Actualización automática del checklist cuando ocurren eventos (documento aprobado, encuesta completada).
- **Singleton** — `ConfiguracionProvider` expone parámetros globales del sistema con instancia única.
- **Strategy** — Inicialización de cortes de seguimiento y checklist inicial al crear la práctica.

---

## Requisitos previos

- **Java 21** o superior
- **Maven 3.9+**
- **Node.js 20+** y **npm 10+**
- **MySQL 8** (local o instancia en Aiven)
- **Cuenta de Firebase** con Storage habilitado
- **Cuenta de Gmail** con App Password de 16 caracteres para SMTP

---

## Configuración local

### 1. Clonar el repositorio

```bash
git clone https://github.com/<org>/sistema-practicas-empresariales.git
cd sistema-practicas-empresariales
```

### 2. Configurar el backend

```bash
cd backend
cp src/main/resources/application.properties.example \
   src/main/resources/application.properties
```

Edita `application.properties` con tus credenciales (ver sección [Variables de entorno](#variables-de-entorno)).

```bash
# Ejecutar migraciones y levantar el servidor
mvn spring-boot:run
```

El backend queda disponible en `http://localhost:8082`.

### 3. Configurar el frontend

```bash
cd frontend
cp .env.example .env
# Editar .env con VITE_API_URL=http://localhost:8082/api
npm install
npm run dev
```

El frontend queda disponible en `http://localhost:5173`.

### 4. Crear el primer usuario administrador

Si la base de datos está vacía, ejecuta una vez:

```bash
curl -X POST http://localhost:8082/api/setup/admin
```

Esto crea el usuario `admin@uah.edu.co` con contraseña `Admin123*`. El endpoint se deshabilita automáticamente una vez que existe un administrador en el sistema.

---

## Variables de entorno

### Backend (`application.properties`)

```properties
# Base de datos
spring.datasource.url=jdbc:mysql://<host>:<puerto>/<bd>?useSSL=true&requireSSL=true
spring.datasource.username=<usuario>
spring.datasource.password=<contraseña>

# JWT
app.jwt.secret=<clave-secreta-larga-aleatoria>
app.jwt.expiration=86400000

# Correo (Gmail con App Password)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=<correo>@gmail.com
spring.mail.password=<app-password-16-chars>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.mail.from=<correo>@gmail.com
app.mail.from-name=Sistema Practicas UAH

# Firebase (ruta al archivo JSON de credenciales)
firebase.credentials.path=<ruta>/firebase-service-account.json
firebase.storage.bucket=<nombre-bucket>.appspot.com

# URL del frontend (para CORS y links en correos)
app.frontend.url=https://<tu-app>.vercel.app
```

### Frontend (`.env`)

```env
VITE_API_URL=https://<tu-backend>.onrender.com/api
VITE_FIREBASE_API_KEY=<api-key>
VITE_FIREBASE_AUTH_DOMAIN=<proyecto>.firebaseapp.com
VITE_FIREBASE_STORAGE_BUCKET=<proyecto>.appspot.com
VITE_FIREBASE_PROJECT_ID=<proyecto>
```

---

## Despliegue en producción

### Backend en Render

1. Crear un nuevo servicio **Web Service** en Render.
2. Conectar el repositorio y configurar:
   - **Build command:** `mvn clean package -DskipTests`
   - **Start command:** `java -jar target/*.jar`
3. Agregar todas las variables de entorno del backend en la sección **Environment**.
4. Asegurarse de que la URL del frontend esté en `allowedOrigins` de `SecurityConfig.java`.

### Frontend en Vercel

1. Importar el repositorio en Vercel.
2. Configurar el directorio raíz como `frontend/`.
3. Agregar las variables de entorno del frontend.
4. Vercel detecta automáticamente Vite y configura el build.

### Base de datos en Aiven

1. Crear un servicio MySQL 8 en [Aiven Console](https://console.aiven.io).
2. Copiar el **Connection URI** con SSL habilitado.
3. Las migraciones Flyway se ejecutan automáticamente al iniciar el backend.

---

## Estructura del proyecto

```
sistema-practicas-empresariales/
│
├── backend/
│   └── src/main/java/co/edu/sistema_practicas_empresariales/
│       ├── config/                    # SecurityConfig, CORS, Swagger
│       ├── security/                  # JwtTokenProvider, JwtAuthenticationFilter
│       ├── modules/
│       │   ├── auth/                  # Login, JWT, reset password
│       │   ├── usuario/               # CRUD usuarios, roles
│       │   ├── estudiante/            # Perfil, aptitud, historial
│       │   ├── practica/              # Instancias, State Pattern, documentos, avances
│       │   │   ├── state/             # EstadoEnPractica, EstadoCompletada, etc.
│       │   │   └── builder/           # ContratoBuilder, ActaCierreBuilder
│       │   ├── empresa/               # Empresas, tutores empresariales
│       │   ├── vacante/               # Vacantes, postulaciones
│       │   ├── evaluacion/            # Notas docente, tutor, nota final
│       │   ├── encuesta/              # Plantillas, secciones, respuestas
│       │   ├── cierre/                # ChecklistCierreService, CierreService
│       │   ├── configuracion/         # Facultades, programas, catálogos, parámetros
│       │   ├── reportes/              # DTOs y servicio de reportes por programa
│       │   ├── bitacora/              # Auditoría con @Auditable
│       │   └── infraestructura/       # Firebase Storage, generador de documentos
│       └── shared/
│           └── email/                 # EmailService, EmailTemplates
│
└── frontend/
    └── src/
        ├── features/
        │   ├── auth/                  # Login, cambio de contraseña, recuperación
        │   ├── estudiante/            # Dashboard, avances, documentos, encuesta, paz y salvo
        │   ├── docente/               # Mis estudiantes, perfil, seguimientos, visitas
        │   ├── tutor/                 # Mis practicantes, evaluación, encuesta
        │   ├── coordinacion/          # Clasificación, carga de docentes
        │   ├── coordinacion-empresarial/ # Practicas, cierre, historial, reportes
        │   ├── empresas/              # Perfil, practicantes, tutores, candidatos
        │   ├── vacantes/              # Listado, detalle, mis vacantes
        │   ├── dashboard/             # Dashboard por rol
        │   ├── configuracion/         # Facultades, programas, catálogos
        │   └── admin/                 # Usuarios, bitácora
        ├── components/
        │   └── layout/               # Layout, Sidebar, ProtectedRoute
        ├── store/
        │   └── authStore.js          # Zustand — usuario autenticado y token
        └── lib/
            ├── axios.js              # Instancia con interceptor de JWT
            ├── roles.js              # Constantes de roles y menú por rol
            └── cloudinary.js         # (Opcional) carga de archivos alternativa
```

---

## Pruebas

El proyecto cuenta con **750 casos de prueba** diseñados, distribuidos entre pruebas unitarias, de integración y end-to-end. La ejecución completa toma aproximadamente **72 horas**.

Actualmente **100 casos están automatizados en GitHub Actions** (todos exitosos), ejecutándose en cada push a la rama `main`.

```bash
# Ejecutar pruebas del backend
cd backend
mvn test

# Ver reporte de cobertura (Jacoco)
mvn jacoco:report
# Reporte disponible en target/site/jacoco/index.html
```

### Distribución por módulo

| Módulo | Casos diseñados | En CI/CD |
|--------|:--------------:|:--------:|
| Autenticación y JWT | 45 | 15 |
| Gestión de Usuarios | 60 | 12 |
| Módulo Estudiante | 80 | 10 |
| Módulo Prácticas (State Pattern) | 90 | 18 |
| Módulo Cierre y Paz y Salvo | 55 | 8 |
| Módulo Evaluaciones | 50 | 8 |
| Módulo Encuestas | 40 | 5 |
| Módulo Vacantes y Postulación | 60 | 8 |
| Módulo Empresas y Tutores | 50 | 5 |
| Módulo Documentos y Firebase | 40 | 5 |
| Módulo Visitas | 30 | 4 |
| Módulo Reportes y Dashboard | 50 | 2 |
| Frontend — Componentes React | 60 | 0 |
| Responsividad Móvil | 40 | 0 |
| Seguridad / Penetración | 30 | 0 |
| **Total** | **750** | **100** |

---

## Equipo

Desarrollado por el equipo de desarrollo de software de la Universidad Antonio Nariño — Semestre 2026-1.

- **Stack:** Java 21 + Spring Boot 3.3 · React 18 + Vite · MySQL 8 en Aiven · Firebase Storage · Render + Vercel
- **Metodología:** Desarrollo iterativo con revisión continua de calidad
- **Control de versiones:** Git + GitHub con GitHub Actions para CI/CD

---

*Sistema de Gestión de Prácticas Empresariales — UAH · 2026*
