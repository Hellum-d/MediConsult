package com.mediconsult.beta.dao.impl;

import com.mediconsult.beta.dao.interfaces.ICitaDAO;
import com.mediconsult.beta.model.Cita;
import com.mediconsult.beta.util.ConexionDB;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC de {@link ICitaDAO}.
 *
 * <p>La verificación de disponibilidad (existeCitaEnFecha) ahora la hace
 * MySQL directamente con el UNIQUE KEY definido en el schema, más un SELECT
 * adicional para la lógica de negocio (excluir CANCELADAS).</p>
 *
 * @author MediConsult Team
 * @version 0.2 — migración JDBC
 */
public class CitaDAOImpl implements ICitaDAO {

    private final Connection conn;

    public CitaDAOImpl() {
        this.conn = ConexionDB.getInstance().getConexion();
        migrarEsquema();
    }

    private void migrarEsquema() {
        try {
            // 1. Agregar SOLICITUD al ENUM si aún no existe
            PreparedStatement checkEstado = conn.prepareStatement(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cita' AND COLUMN_NAME = 'estado'");
            ResultSet rsE = checkEstado.executeQuery();
            if (rsE.next() && !rsE.getString(1).contains("SOLICITUD")) {
                conn.createStatement().execute(
                    "ALTER TABLE cita MODIFY COLUMN estado " +
                    "ENUM('PENDIENTE','COMPLETADA','CANCELADA','SOLICITUD') DEFAULT 'PENDIENTE'");
            }
            // 2. medico_id nullable
            PreparedStatement checkM = conn.prepareStatement(
                "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cita' AND COLUMN_NAME = 'medico_id'");
            ResultSet rsM = checkM.executeQuery();
            if (rsM.next() && "NO".equals(rsM.getString(1))) {
                conn.createStatement().execute("ALTER TABLE cita MODIFY COLUMN medico_id INT NULL");
            }
            // 3. fecha nullable
            PreparedStatement checkF = conn.prepareStatement(
                "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cita' AND COLUMN_NAME = 'fecha'");
            ResultSet rsF = checkF.executeQuery();
            if (rsF.next() && "NO".equals(rsF.getString(1))) {
                conn.createStatement().execute("ALTER TABLE cita MODIFY COLUMN fecha DATE NULL");
            }
            // 4. hora nullable
            PreparedStatement checkH = conn.prepareStatement(
                "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cita' AND COLUMN_NAME = 'hora'");
            ResultSet rsH = checkH.executeQuery();
            if (rsH.next() && "NO".equals(rsH.getString(1))) {
                conn.createStatement().execute("ALTER TABLE cita MODIFY COLUMN hora TIME NULL");
            }
        } catch (SQLException e) {
            System.err.println("[CitaDAO] Migración de esquema: " + e.getMessage());
        }
    }

    @Override
    public void guardar(Cita cita) {
        String sql = "INSERT INTO cita (paciente_id, medico_id, fecha, hora, estado, motivo) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, cita.getPacienteId());
            if (cita.getMedicoId() > 0) ps.setInt(2, cita.getMedicoId());
            else                        ps.setNull(2, java.sql.Types.INTEGER);
            if (cita.getFecha() != null) ps.setDate(3, Date.valueOf(cita.getFecha()));
            else                         ps.setNull(3, java.sql.Types.DATE);
            if (cita.getHora() != null && !cita.getHora().isBlank()) ps.setString(4, cita.getHora());
            else                                                       ps.setNull(4, java.sql.Types.TIME);
            ps.setString(5, cita.getEstado());
            ps.setString(6, cita.getMotivo() != null ? cita.getMotivo() : "");
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) cita.setId(keys.getInt(1));

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar cita: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizar(Cita cita) {
        String sql = "UPDATE cita SET paciente_id=?, medico_id=?, fecha=?, hora=?, estado=?, motivo=? WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cita.getPacienteId());
            if (cita.getMedicoId() > 0) ps.setInt(2, cita.getMedicoId());
            else                        ps.setNull(2, java.sql.Types.INTEGER);
            if (cita.getFecha() != null) ps.setDate(3, Date.valueOf(cita.getFecha()));
            else                         ps.setNull(3, java.sql.Types.DATE);
            if (cita.getHora() != null && !cita.getHora().isBlank()) ps.setString(4, cita.getHora());
            else                                                       ps.setNull(4, java.sql.Types.TIME);
            ps.setString(5, cita.getEstado());
            ps.setString(6, cita.getMotivo() != null ? cita.getMotivo() : "");
            ps.setInt(7, cita.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar cita: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminar(int id) {
        String sql = "DELETE FROM cita WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar cita: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Cita> buscarPorId(int id) {
        String sql = "SELECT id, paciente_id, medico_id, fecha, hora, estado, motivo FROM cita WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar cita: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Cita> listarTodos() {
        String sql = "SELECT id, paciente_id, medico_id, fecha, hora, estado, motivo FROM cita ORDER BY fecha DESC";
        List<Cita> lista = new ArrayList<>();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar citas: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public List<Cita> listarPorPaciente(int pacienteId) {
        String sql = "SELECT id, paciente_id, medico_id, fecha, hora, estado, motivo FROM cita " +
                     "WHERE paciente_id=? ORDER BY fecha DESC";
        List<Cita> lista = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, pacienteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar citas por paciente: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public List<Cita> listarPorMedico(int medicoId) {
        String sql = "SELECT id, paciente_id, medico_id, fecha, hora, estado, motivo FROM cita " +
                     "WHERE medico_id=? ORDER BY fecha ASC";
        List<Cita> lista = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, medicoId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar citas por médico: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public boolean existeCitaEnFecha(int medicoId, LocalDate fecha) {
        String sql = "SELECT COUNT(*) FROM cita " +
                     "WHERE medico_id=? AND fecha=? AND estado != 'CANCELADA'";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, medicoId);
            ps.setDate(2, Date.valueOf(fecha));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar disponibilidad: " + e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean existeCitaEnFechaHora(int medicoId, LocalDate fecha, String hora) {
        String sql = "SELECT COUNT(*) FROM cita " +
                     "WHERE medico_id=? AND fecha=? AND hora=? AND estado != 'CANCELADA'";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, medicoId);
            ps.setDate(2, Date.valueOf(fecha));
            ps.setString(3, hora);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar hora: " + e.getMessage(), e);
        }
        return false;
    }

    @Override
    public List<String> horasOcupadasEnFecha(int medicoId, LocalDate fecha) {
        String sql = "SELECT hora FROM cita " +
                     "WHERE medico_id=? AND fecha=? AND estado != 'CANCELADA'";
        List<String> horas = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, medicoId);
            ps.setDate(2, Date.valueOf(fecha));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String h = rs.getString("hora");
                if (h != null) horas.add(h.substring(0, 5)); // "HH:mm"
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar horas ocupadas: " + e.getMessage(), e);
        }
        return horas;
    }

    @Override
    public List<Cita> listarSolicitudes() {
        String sql = "SELECT id, paciente_id, medico_id, fecha, hora, estado, motivo " +
                     "FROM cita WHERE estado = 'SOLICITUD' ORDER BY id DESC";
        List<Cita> lista = new ArrayList<>();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar solicitudes: " + e.getMessage(), e);
        }
        return lista;
    }

    // ── Helper de mapeo ────────────────────────────────────────────────────

    private Cita mapear(ResultSet rs) throws SQLException {
        // medicoId puede ser NULL en solicitudes sin médico asignado
        int medicoId = rs.getInt("medico_id");
        if (rs.wasNull()) medicoId = 0;

        // fecha y hora pueden ser NULL en solicitudes
        Date fechaDB = rs.getDate("fecha");
        LocalDate fecha = (fechaDB != null) ? fechaDB.toLocalDate() : null;

        String horaRaw = null;
        String motivoRaw = "";
        try { horaRaw   = rs.getString("hora");   } catch (SQLException ignored) {}
        try { motivoRaw = rs.getString("motivo"); } catch (SQLException ignored) {}
        String hora = (horaRaw != null) ? horaRaw.substring(0, Math.min(5, horaRaw.length())) : null;

        return new Cita(
                rs.getInt("id"),
                rs.getInt("paciente_id"),
                medicoId,
                fecha,
                rs.getString("estado"),
                hora,
                motivoRaw
        );
    }
}
