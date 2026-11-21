# CLINEX — MediConsult-Beta

Sistema de gestión hospitalaria de escritorio con diagnóstico asistido por IA.
Proyecto académico de **Programación de Computadores III** — Universidad Popular del Cesar (UPC).

**Versión actual:** 0.2.0 · **Stack:** Java 17 · JavaFX 17 · MySQL 8 (JDBC) · OpenAI GPT-4.1 · Maven

---

## Funcionalidades

- **Login por rol** con contraseñas en SHA-256, registro de cuenta y recuperación de contraseña.
- **Gestión de pacientes y médicos** (CRUD) con inactivación/reactivación (*soft-delete*).
- **Citas**: agendamiento, estados, validación de fechas y lista de espera.
- **Diagnóstico asistido por IA**: análisis de síntomas en texto e imágenes médicas (GPT-4.1 con visión).
- **Autodiagnóstico** desde el portal del paciente.
- **Flujo clínico**: historia clínica, órdenes médicas, remisiones, consulta especializada
  (asignación de especialistas por especialidad) y alta médica.
- **Notificaciones** para administrador y paciente.

## Roles

| Rol | Pantalla inicial | Acceso |
|-----|------------------|--------|
| `ADMIN` | Gestión de pacientes | Pacientes, médicos, citas, remisiones, notificaciones |
| `MEDICO_GENERAL` (`MEDICO`) | Panel médico | Consultas, diagnósticos, órdenes, remisiones |
| `MEDICO_ESPECIALISTA` | Panel especialista | Consultas especializadas y altas médicas |
| `PACIENTE` | Portal del paciente | Citas, autodiagnóstico, órdenes, notificaciones |

## Requisitos

- JDK 17 o superior
- Maven 3.8+ (o NetBeans/IntelliJ con soporte Maven)
- MySQL 8
- Una API key de OpenAI (solo para las funciones de IA)

## Instalación y ejecución

1. **Base de datos** — ejecutar el script (borra y recrea la BD `clinex`):
   ```bash
   mysql -u root -p < DB.sql
   ```
   `DB.sql` crea el esquema base y los usuarios iniciales. Al arrancar, la aplicación
   completa automáticamente el resto (tablas `historia_clinica`, `orden_medica`,
   `autodiagnostico`, nuevas columnas y roles), así que no hace falta otro script.

2. **Conexión** — por defecto se usa `root` / `1234` en `localhost:3306`.
   Si tus credenciales son otras, cámbialas en
   `src/main/java/com/mediconsult/beta/util/ConexionDB.java`.

3. **API key de OpenAI** — se lee de la variable de entorno `OPENAI_API_KEY`
   (nunca la escribas en el código):
   ```powershell
   setx OPENAI_API_KEY "sk-..."
   ```

4. **Ejecutar la interfaz gráfica** (JavaFX):
   ```bash
   mvn javafx:run
   ```
   En NetBeans basta con *Run Project* (usa `nbactions.xml`).

   Existe además un modo consola heredado de la v0.1 (`mvn compile exec:java`),
   que se conserva solo como referencia.

### Usuarios iniciales

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| `admin` | `admin123` | ADMIN |
| `medico` | `medico123` | MEDICO |

Los pacientes pueden crear su propia cuenta desde *Crear cuenta* en el login.

## Arquitectura

Arquitectura en capas; cada capa solo depende de la inmediatamente inferior.

```
src/main/java/com/mediconsult/beta/
├── AppFX.java          Punto de entrada de la interfaz JavaFX
├── Main.java           Punto de entrada del modo consola (legado v0.1)
├── controller/         Controladores de las vistas FXML
├── service/            Lógica de negocio y validaciones
├── dao/interfaces/     Contratos de acceso a datos
├── dao/impl/           Implementaciones JDBC sobre MySQL
├── model/              Entidades (Persona → Paciente / Médico, Cita, Diagnóstico…)
├── util/               ConexionDB, OpenAIClient, Sesion, NotificacionUtil, TopBarUtil…
└── view/               Menús del modo consola
src/main/resources/com/mediconsult/beta/view/fxml/   Vistas FXML, estilos y logo
DB.sql                  Esquema base de la base de datos
CLINEX_Diagrama_Flujo.pdf / generar_diagrama.py      Diagrama de flujo del sistema
```

## Historial de versiones

- **v0.1** — Aplicación de consola con persistencia en archivos CSV e IA con GPT-4o-mini.
- **v0.2** — Interfaz JavaFX, persistencia MySQL vía JDBC, GPT-4.1 con visión, flujo clínico completo
  (remisiones, consultas especializadas, órdenes y altas), soft-delete y notificaciones.

## Equipo y flujo de trabajo

| Integrante | Rama | Aporte |
|------------|------|--------|
| Santiago (líder) | `feature/lider-util-service` | Capas `util` y `service` |
| César | `feature/dev1-model-interfaces` | Capa `model` e interfaces DAO |
| Moisés | `feature/dev2-impl-view` | Implementaciones DAO y vistas |

Las ramas `feature/*` se integran en `develop`, y `develop` se integra en `main` para cada entrega.
