package com.mediconsult.beta.dao.impl;

import com.mediconsult.beta.dao.interfaces.IOrdenMedicaDAO;
import com.mediconsult.beta.model.OrdenMedica;
import com.mediconsult.beta.util.ConexionDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrdenMedicaDAOImpl implements IOrdenMedicaDAO {

    private final Connection conn;

    public OrdenMedicaDAOImpl() {
        this.conn = ConexionDB.getInstance().getConexion();
        crearTablaIfNotExists();
    }

    private void crearTablaIfNotExists() {
        String sql =
            "CREATE TABLE IF NOT EXISTS orden_medica (" +
            "  id                  INT AUTO_INCREMENT PRIMARY KEY," +
            "  cita_id             INT NOT NULL," +
            "  paciente_id         INT NOT NULL," +
            "  medico_id           INT NOT NULL," +
            "  historia_clinica_id INT," +
            "  tipo                VARCHAR(30)  NOT NULL," +
            "  descripcion         TEXT         NOT NULL," +
            "  observaciones       TEXT," +
            "  estado              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVA'," +
            "  fecha_registro      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (cita_id)     REFERENCES cita(id)     ON DELETE CASCADE," +
            "  FOREIGN KEY (paciente_id) REFERENCES paciente(id) ON DELETE CASCADE," +
            "  FOREIGN KEY (medico_id)   REFERENCES medico(id)   ON DELETE CASCADE" +
            ")";
        try {
            conn.createStatement().execute(sql);
            System.out.println("[OrdenMedicaDAO] Tabla 'orden_medica' verificada.");
        } catch (SQLException e) {
            System.err.println("[OrdenMedicaDAO] Error en migración: " + e.getMessage());
        }
    }

    @Override
    public void guardar(OrdenMedica orden) {
        String sql = "INSERT INTO orden_medica " +
                     "(cita_id, paciente_id, medico_id, historia_clinica_id, " +
                     " tipo, descripcion, observaciones, estado) " +
                     "VALUES (?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, orden.getCitaId());
            ps.setInt(2, orden.getPacienteId());
            ps.setInt(3, orden.getMedicoId());
            if (orden.getHistoriaClinicaId() != null)
                ps.setInt(4, orden.getHistoriaClinicaId());
            else
                ps.setNull(4, Types.INTEGER);
            ps.setString(5, orden.getTipo());
            ps.setString(6, orden.getDescripcion());
            ps.setString(7, orden.getObservaciones());
            ps.setString(8, orden.getEstado());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) orden.setId(keys.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar orden médica: " + e.getMessage(), e);
        }
    }

    @Override
    public List<OrdenMedica> listarPorCita(int citaId) {
        String sql = BASE_SELECT + " WHERE cita_id=? ORDER BY fecha_registro DESC";
        List<OrdenMedica> lista = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, citaId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar órdenes por cita: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public List<OrdenMedica> listarPorPaciente(int pacienteId) {
        String sql = BASE_SELECT + " WHERE paciente_id=? ORDER BY fecha_registro DESC";
        List<OrdenMedica> lista = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, pacienteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar órdenes por paciente: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public void eliminar(int id) {
        String sql = "DELETE FROM orden_medica WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar orden médica: " + e.getMessage(), e);
        }
    }

    private static final String BASE_SELECT =
        "SELECT id, cita_id, paciente_id, medico_id, historia_clinica_id, " +
        "tipo, descripcion, observaciones, estado, fecha_registro FROM orden_medica";

    private OrdenMedica mapear(ResultSet rs) throws SQLException {
        OrdenMedica o = new OrdenMedica();
        o.setId(rs.getInt("id"));
        o.setCitaId(rs.getInt("cita_id"));
        o.setPacienteId(rs.getInt("paciente_id"));
        o.setMedicoId(rs.getInt("medico_id"));
        int hcId = rs.getInt("historia_clinica_id");
        if (!rs.wasNull()) o.setHistoriaClinicaId(hcId);
        o.setTipo(rs.getString("tipo"));
        o.setDescripcion(rs.getString("descripcion"));
        o.setObservaciones(rs.getString("observaciones"));
        o.setEstado(rs.getString("estado"));
        Timestamp ts = rs.getTimestamp("fecha_registro");
        if (ts != null) o.setFechaRegistro(ts.toLocalDateTime());
        return o;
    }
}
