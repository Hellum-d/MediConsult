package com.mediconsult.beta.dao.impl;

import com.mediconsult.beta.dao.interfaces.IRemisionDAO;
import com.mediconsult.beta.model.Remision;
import com.mediconsult.beta.util.ConexionDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RemisionDAOImpl implements IRemisionDAO {

    private final Connection conn;

    public RemisionDAOImpl() {
        this.conn = ConexionDB.getInstance().getConexion();
        crearTablaIfNotExists();
        migrarColumnaEspecialista();
    }

    private void crearTablaIfNotExists() {
        String sql =
            "CREATE TABLE IF NOT EXISTS remision (" +
            "  id                    INT AUTO_INCREMENT PRIMARY KEY," +
            "  cita_id               INT          NOT NULL," +
            "  paciente_id           INT          NOT NULL," +
            "  medico_remitente_id   INT          NOT NULL," +
            "  medico_especialista_id INT          NULL," +
            "  especialidad_destino  VARCHAR(100) NOT NULL," +
            "  motivo_remision       TEXT         NOT NULL," +
            "  observaciones         TEXT," +
            "  estado                VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE'," +
            "  fecha_registro        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (cita_id)             REFERENCES cita(id)     ON DELETE CASCADE," +
            "  FOREIGN KEY (paciente_id)         REFERENCES paciente(id) ON DELETE CASCADE," +
            "  FOREIGN KEY (medico_remitente_id) REFERENCES medico(id)   ON DELETE CASCADE" +
            ")";
        try {
            conn.createStatement().execute(sql);
            System.out.println("[RemisionDAO] Tabla 'remision' verificada.");
        } catch (SQLException e) {
            System.err.println("[RemisionDAO] Error creando tabla: " + e.getMessage());
        }
    }

    private void migrarColumnaEspecialista() {
        String check =
            "SELECT COUNT(*) FROM information_schema.columns " +
            "WHERE table_schema = DATABASE() " +
            "  AND table_name   = 'remision' " +
            "  AND column_name  = 'medico_especialista_id'";
        try {
            ResultSet rs = conn.prepareStatement(check).executeQuery();
            rs.next();
            if (rs.getInt(1) == 0) {
                conn.createStatement().execute(
                    "ALTER TABLE remision ADD COLUMN medico_especialista_id INT NULL");
                System.out.println("[RemisionDAO] Columna 'medico_especialista_id' añadida.");
            }
        } catch (SQLException e) {
            System.err.println("[RemisionDAO] Error en migración especialista: " + e.getMessage());
        }
    }

    @Override
    public void guardar(Remision r) {
        String sql = "INSERT INTO remision " +
                     "(cita_id, paciente_id, medico_remitente_id, medico_especialista_id, " +
                     " especialidad_destino, motivo_remision, observaciones, estado) " +
                     "VALUES (?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, r.getCitaId());
            ps.setInt(2, r.getPacienteId());
            ps.setInt(3, r.getMedicoRemitenteId());
            if (r.getMedicoEspecialistaId() != null)
                ps.setInt(4, r.getMedicoEspecialistaId());
            else
                ps.setNull(4, Types.INTEGER);
            ps.setString(5, r.getEspecialidadDestino());
            ps.setString(6, r.getMotivoRemision());
            ps.setString(7, r.getObservaciones());
            ps.setString(8, r.getEstado() != null ? r.getEstado() : Remision.ESTADO_PENDIENTE);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) r.setId(keys.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar remisión: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Remision> buscarPorId(int id) {
        String sql = BASE_SELECT + " WHERE id=? LIMIT 1";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar remisión por id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public void actualizar(Remision r) {
        String sql = "UPDATE remision SET especialidad_destino=?, motivo_remision=?, " +
                     "observaciones=?, estado=?, medico_especialista_id=? WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, r.getEspecialidadDestino());
            ps.setString(2, r.getMotivoRemision());
            ps.setString(3, r.getObservaciones());
            ps.setString(4, r.getEstado());
            if (r.getMedicoEspecialistaId() != null)
                ps.setInt(5, r.getMedicoEspecialistaId());
            else
                ps.setNull(5, Types.INTEGER);
            ps.setInt(6, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar remisión: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Remision> listarPorCita(int citaId) {
        return listar(BASE_SELECT + " WHERE cita_id=? ORDER BY fecha_registro DESC", citaId);
    }

    @Override
    public List<Remision> listarPorPaciente(int pacienteId) {
        return listar(BASE_SELECT + " WHERE paciente_id=? ORDER BY fecha_registro DESC", pacienteId);
    }

    @Override
    public List<Remision> listarPorEspecialidad(String especialidad) {
        String sql = BASE_SELECT + " WHERE especialidad_destino=? ORDER BY fecha_registro DESC";
        List<Remision> lista = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, especialidad);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar remisiones por especialidad: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public List<Remision> listarPorEspecialista(int medicoEspecialistaId) {
        String sql = BASE_SELECT + " WHERE medico_especialista_id=? ORDER BY fecha_registro DESC";
        List<Remision> lista = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, medicoEspecialistaId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar remisiones por especialista: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public List<Remision> listarTodas() {
        List<Remision> lista = new ArrayList<>();
        try {
            ResultSet rs = conn.createStatement()
                    .executeQuery(BASE_SELECT + " ORDER BY fecha_registro DESC");
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar todas las remisiones: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public void eliminar(int id) {
        String sql = "DELETE FROM remision WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar remisión: " + e.getMessage(), e);
        }
    }

    private List<Remision> listar(String sql, int param) {
        List<Remision> lista = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar remisiones: " + e.getMessage(), e);
        }
        return lista;
    }

    private static final String BASE_SELECT =
        "SELECT id, cita_id, paciente_id, medico_remitente_id, medico_especialista_id, " +
        "especialidad_destino, motivo_remision, observaciones, estado, fecha_registro FROM remision";

    private Remision mapear(ResultSet rs) throws SQLException {
        Remision r = new Remision();
        r.setId(rs.getInt("id"));
        r.setCitaId(rs.getInt("cita_id"));
        r.setPacienteId(rs.getInt("paciente_id"));
        r.setMedicoRemitenteId(rs.getInt("medico_remitente_id"));
        int espId = rs.getInt("medico_especialista_id");
        if (!rs.wasNull()) r.setMedicoEspecialistaId(espId);
        r.setEspecialidadDestino(rs.getString("especialidad_destino"));
        r.setMotivoRemision(rs.getString("motivo_remision"));
        r.setObservaciones(rs.getString("observaciones"));
        r.setEstado(rs.getString("estado"));
        Timestamp ts = rs.getTimestamp("fecha_registro");
        if (ts != null) r.setFechaRegistro(ts.toLocalDateTime());
        return r;
    }
}
