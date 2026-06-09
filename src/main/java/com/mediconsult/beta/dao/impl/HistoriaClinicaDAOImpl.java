package com.mediconsult.beta.dao.impl;

import com.mediconsult.beta.dao.interfaces.IHistoriaClinicaDAO;
import com.mediconsult.beta.model.HistoriaClinica;
import com.mediconsult.beta.util.ConexionDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC de {@link IHistoriaClinicaDAO}.
 *
 * <p>Crea la tabla {@code historia_clinica} automáticamente si no existe.</p>
 */
public class HistoriaClinicaDAOImpl implements IHistoriaClinicaDAO {

    private final Connection conn;

    public HistoriaClinicaDAOImpl() {
        this.conn = ConexionDB.getInstance().getConexion();
        crearTablaIfNotExists();
    }

    private void crearTablaIfNotExists() {
        String sql =
            "CREATE TABLE IF NOT EXISTS historia_clinica (" +
            "  id             INT AUTO_INCREMENT PRIMARY KEY," +
            "  cita_id        INT NOT NULL," +
            "  paciente_id    INT NOT NULL," +
            "  medico_id      INT NOT NULL," +
            "  anamnesis      TEXT," +
            "  examen_fisico  TEXT," +
            "  diagnostico    TEXT," +
            "  tratamiento    TEXT," +
            "  observaciones  TEXT," +
            "  fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (cita_id)     REFERENCES cita(id)     ON DELETE CASCADE," +
            "  FOREIGN KEY (paciente_id) REFERENCES paciente(id) ON DELETE CASCADE," +
            "  FOREIGN KEY (medico_id)   REFERENCES medico(id)   ON DELETE CASCADE" +
            ")";
        try {
            conn.createStatement().execute(sql);
            System.out.println("[HistoriaClinicaDAO] Tabla 'historia_clinica' verificada.");
        } catch (SQLException e) {
            System.err.println("[HistoriaClinicaDAO] Error en migración: " + e.getMessage());
        }
    }

    @Override
    public void guardar(HistoriaClinica h) {
        String sql = "INSERT INTO historia_clinica " +
                     "(cita_id, paciente_id, medico_id, anamnesis, examen_fisico, " +
                     " diagnostico, tratamiento, observaciones) " +
                     "VALUES (?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, h.getCitaId());
            ps.setInt(2, h.getPacienteId());
            ps.setInt(3, h.getMedicoId());
            ps.setString(4, h.getAnamnesis());
            ps.setString(5, h.getExamenFisico());
            ps.setString(6, h.getDiagnostico());
            ps.setString(7, h.getTratamiento());
            ps.setString(8, h.getObservaciones());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) h.setId(keys.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar historia clínica: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizar(HistoriaClinica h) {
        String sql = "UPDATE historia_clinica SET anamnesis=?, examen_fisico=?, " +
                     "diagnostico=?, tratamiento=?, observaciones=? WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, h.getAnamnesis());
            ps.setString(2, h.getExamenFisico());
            ps.setString(3, h.getDiagnostico());
            ps.setString(4, h.getTratamiento());
            ps.setString(5, h.getObservaciones());
            ps.setInt(6, h.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar historia clínica: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<HistoriaClinica> buscarPorCita(int citaId) {
        String sql = BASE_SELECT + " WHERE cita_id=? LIMIT 1";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, citaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar historia clínica: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<HistoriaClinica> listarPorPaciente(int pacienteId) {
        String sql = BASE_SELECT + " WHERE paciente_id=? ORDER BY fecha_registro DESC";
        List<HistoriaClinica> lista = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, pacienteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar historias clínicas: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public List<HistoriaClinica> listarPorMedico(int medicoId) {
        String sql = BASE_SELECT + " WHERE medico_id=? ORDER BY fecha_registro DESC";
        List<HistoriaClinica> lista = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, medicoId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar historias por médico: " + e.getMessage(), e);
        }
        return lista;
    }

    private static final String BASE_SELECT =
        "SELECT id, cita_id, paciente_id, medico_id, anamnesis, examen_fisico, " +
        "diagnostico, tratamiento, observaciones, fecha_registro FROM historia_clinica";

    private HistoriaClinica mapear(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("fecha_registro");
        LocalDateTime fecha = ts != null ? ts.toLocalDateTime() : null;
        return new HistoriaClinica(
                rs.getInt("id"),
                rs.getInt("cita_id"),
                rs.getInt("paciente_id"),
                rs.getInt("medico_id"),
                rs.getString("anamnesis"),
                rs.getString("examen_fisico"),
                rs.getString("diagnostico"),
                rs.getString("tratamiento"),
                rs.getString("observaciones"),
                fecha
        );
    }
}
