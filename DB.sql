-- ============================================================
--  CLINEX - Sistema de Gestión Hospitalaria
--  Base de Datos - Schema completo y corregido
--  Programación de Computadores III - UPC
--  v2 — elimina y recrea todo limpio
--  Nota: al arrancar, la aplicación crea las tablas historia_clinica,
--  orden_medica y autodiagnostico, agrega columnas nuevas y amplía los
--  roles (MEDICO_GENERAL, MEDICO_ESPECIALISTA, PACIENTE) automáticamente.
-- ============================================================

-- Eliminar BD si existe y crearla de nuevo limpia
DROP DATABASE IF EXISTS clinex;
CREATE DATABASE clinex CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clinex;

-- ------------------------------------------------------------
-- TABLA BASE: persona
-- ------------------------------------------------------------
CREATE TABLE persona (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    nombre    VARCHAR(100) NOT NULL,
    apellido  VARCHAR(100) NOT NULL,
    telefono  VARCHAR(20),
    direccion VARCHAR(150),
    correo    VARCHAR(100)
);

-- ------------------------------------------------------------
-- TABLA: paciente  (extiende persona)
-- ------------------------------------------------------------
CREATE TABLE paciente (
    id          INT PRIMARY KEY,
    edad        INT NOT NULL CHECK (edad >= 0),
    historial   TEXT,
    tipo_sangre VARCHAR(5)   NOT NULL DEFAULT '',
    alergias    VARCHAR(500) NOT NULL DEFAULT ''
);

-- ------------------------------------------------------------
-- TABLA: medico  (extiende persona)
-- ------------------------------------------------------------
CREATE TABLE medico (
    id           INT PRIMARY KEY,
    especialidad VARCHAR(100) NOT NULL,
    disponible   BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (id) REFERENCES persona(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- ------------------------------------------------------------
-- TABLA: usuario
-- Sin medico_id para mantener compatibilidad con la tabla actual
-- ------------------------------------------------------------
CREATE TABLE usuario (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol           ENUM('ADMIN', 'MEDICO') NOT NULL
);

-- ------------------------------------------------------------
-- TABLA: cita
-- ------------------------------------------------------------
CREATE TABLE cita (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    paciente_id INT  NOT NULL,
    medico_id   INT  NOT NULL,
    fecha       DATE NOT NULL,
    hora        TIME NOT NULL,
    estado      ENUM('PENDIENTE', 'COMPLETADA', 'CANCELADA') DEFAULT 'PENDIENTE',
    motivo      TEXT,
    FOREIGN KEY (paciente_id) REFERENCES paciente(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    FOREIGN KEY (medico_id) REFERENCES medico(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    UNIQUE KEY uq_medico_fecha_hora (medico_id, fecha, hora)
);

-- ------------------------------------------------------------
-- TABLA: diagnostico
-- ------------------------------------------------------------
CREATE TABLE diagnostico (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    cita_id           INT  NOT NULL,
    sintomas          TEXT NOT NULL,
    respuesta_ia      TEXT,
    diagnostico_final TEXT,
    tratamiento       TEXT,
    fecha_registro    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cita_id) REFERENCES cita(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- ------------------------------------------------------------
-- TABLA: remision
-- ------------------------------------------------------------
CREATE TABLE remision (
    id                     INT AUTO_INCREMENT PRIMARY KEY,
    cita_id                INT          NOT NULL,
    paciente_id            INT          NOT NULL,
    medico_remitente_id    INT          NOT NULL,
    medico_especialista_id INT          NULL,
    especialidad_destino   VARCHAR(100) NOT NULL,
    motivo_remision        TEXT         NOT NULL,
    observaciones          TEXT,
    estado                 VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE',
    fecha_registro         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cita_id)             REFERENCES cita(id)     ON DELETE CASCADE,
    FOREIGN KEY (paciente_id)         REFERENCES paciente(id) ON DELETE CASCADE,
    FOREIGN KEY (medico_remitente_id) REFERENCES medico(id)   ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- TABLA: consulta_especializada  (depende de remision)
-- ------------------------------------------------------------
CREATE TABLE consulta_especializada (
    id                     INT AUTO_INCREMENT PRIMARY KEY,
    remision_id            INT  NOT NULL,
    paciente_id            INT  NOT NULL,
    medico_especialista_id INT  NOT NULL,
    motivo_consulta        TEXT,
    hallazgos              TEXT,
    diagnostico            TEXT,
    tratamiento            TEXT,
    evolucion              TEXT,
    observaciones          TEXT,
    estado                 VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    fecha_registro         TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (remision_id)            REFERENCES remision(id)  ON DELETE CASCADE,
    FOREIGN KEY (paciente_id)            REFERENCES paciente(id)  ON DELETE CASCADE,
    FOREIGN KEY (medico_especialista_id) REFERENCES medico(id)    ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- TABLA: orden_especializada  (depende de consulta_especializada)
-- ------------------------------------------------------------
CREATE TABLE orden_especializada (
    id                        INT AUTO_INCREMENT PRIMARY KEY,
    consulta_especializada_id INT         NOT NULL,
    paciente_id               INT         NOT NULL,
    medico_id                 INT         NOT NULL,
    tipo                      VARCHAR(30) NOT NULL,
    descripcion               TEXT        NOT NULL,
    observaciones             TEXT,
    estado                    VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    fecha_registro            TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (consulta_especializada_id) REFERENCES consulta_especializada(id) ON DELETE CASCADE,
    FOREIGN KEY (paciente_id)               REFERENCES paciente(id)               ON DELETE CASCADE,
    FOREIGN KEY (medico_id)                 REFERENCES medico(id)                 ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- TABLA: alta_medica  (depende de remision y consulta_especializada)
-- ------------------------------------------------------------
CREATE TABLE alta_medica (
    id                            INT AUTO_INCREMENT PRIMARY KEY,
    remision_id                   INT  NOT NULL,
    consulta_especializada_id     INT  NOT NULL,
    paciente_id                   INT  NOT NULL,
    medico_especialista_id        INT  NOT NULL,
    diagnostico_final             TEXT,
    tipo_alta                     VARCHAR(30) NOT NULL,
    indicaciones_paciente         TEXT,
    indicaciones_medico_remitente TEXT,
    observaciones                 TEXT,
    fecha_alta                    DATE NOT NULL,
    fecha_registro                TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (remision_id)               REFERENCES remision(id)               ON DELETE CASCADE,
    FOREIGN KEY (consulta_especializada_id) REFERENCES consulta_especializada(id)  ON DELETE CASCADE,
    FOREIGN KEY (paciente_id)               REFERENCES paciente(id)               ON DELETE CASCADE,
    FOREIGN KEY (medico_especialista_id)    REFERENCES medico(id)                 ON DELETE CASCADE
);

-- ============================================================
-- DATOS INICIALES
-- admin   / admin123  → SHA-256
-- medico  / medico123 → SHA-256
-- ============================================================
INSERT INTO usuario (username, password_hash, rol) VALUES
('admin',  '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN'),
('medico', '673ab82a6530ee3bd9b04ee72a4d66afa7fa059aedc685cf44e35d29d90ebafa', 'MEDICO');

-- ============================================================
-- VISTAS ÚTILES
-- ============================================================

CREATE VIEW vista_paciente AS
    SELECT pe.id, pe.nombre, pe.apellido, pe.telefono,
           pe.correo, pa.edad, pa.historial,
           pa.tipo_sangre, pa.alergias
    FROM persona pe
    INNER JOIN paciente pa ON pe.id = pa.id;

CREATE VIEW vista_medico AS
    SELECT pe.id, pe.nombre, pe.apellido, pe.telefono,
           pe.correo, me.especialidad, me.disponible
    FROM persona pe
    INNER JOIN medico me ON pe.id = me.id;

CREATE VIEW vista_cita AS
    SELECT c.id, c.fecha, c.hora, c.estado, c.motivo,
           CONCAT(pp.nombre, ' ', pp.apellido) AS paciente,
           CONCAT(pm.nombre, ' ', pm.apellido) AS medico,
           me.especialidad
    FROM cita c
    INNER JOIN paciente pa ON c.paciente_id = pa.id
    INNER JOIN persona  pp ON pa.id = pp.id
    INNER JOIN medico   me ON c.medico_id = me.id
    INNER JOIN persona  pm ON me.id = pm.id;
    
    
