package com.mediconsult.beta.dao.impl;

import com.mediconsult.beta.dao.interfaces.IConsultaEspecializadaDAO;
import com.mediconsult.beta.model.ConsultaEspecializada;
import com.mediconsult.beta.util.ConexionDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConsultaEspecializadaDAOImpl implements IConsultaEspecializadaDAO {

    private final Connection conn;

    public ConsultaEspecializadaDAOImpl() {
        this.conn = ConexionDB.getInstance().getConexion();
        crearTablaIfNotExists();
    }

    private void crearTablaIfNotExists() {
        // FK a remision se omite aquí: remision es también auto-creada y puede no existir aún.
        // La integridad se garantiza en la capa de servicio y en DB.sql (instalación limpia).
        String sql =
            "CREATE TABLE IF NOT EXISTS consulta_especializada (" +
            "  id                     INT AUTO_INCREMENT PRIMARY KEY," +
            "  remision_id            INT  NOT NULL," +
            "  paciente_id            INT  NOT NULL," +
            "  medico_especialista_id INT  NOT NULL," +
            "  motivo_consulta        TEXT," +
            "  hallazgos              TEXT," +
            "  diagnostico            TEXT," +
            "  tratamiento            TEXT," +
            "  evolucion              TEXT," +
            "  observaciones          TEXT," +
            "  estado                 VARCHAR(20) NOT NULL DEFAULT 'ACTIVA'," +
            "  fecha_registro         TIMESTAMP   DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (paciente_id)            REFERENCES paciente(id)  ON DELETE CASCADE," +
            "  FOREIGN KEY (medico_especialista_id) REFERENCES medico(id)    ON DELETE CASCADE" +
            ")";
        try {
            conn.createStatement().execute(sql);
            System.out.println("[ConsultaEspDAO] Tabla 'consulta_especializada' verificada.");
        } catch (SQLException e) {
            System.err.println("[ConsultaEspDAO] Error en migración: " + e.getMessage());
        }
    }

    @Override
    public void guardar(ConsultaEspecializada c) {
        String sql = "INSERT INTO consulta_especializada " +
                     "(remision_id, paciente_id, medico_especialista_id, motivo_consulta, " +
                     " hallazgos, diagnostico, tratamiento, evolucion, observaciones, estado) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, c.getRemisionId());
            ps.setInt(2, c.getPacienteId());
            ps.setInt(3, c.getMedicoEspecialistaId());
            ps.setString(4, c.getMotivoConsulta());
            ps.setString(5, c.getHallazgos());
            ps.setString(6, c.getDiagnostico());
            ps.setString(7, c.getTratamiento());
            ps.setString(8, c.getEvolucion());
            ps.setString(9, c.getObservaciones());
            ps.setString(10, c.getEstado() != null ? c.getEstado() : ConsultaEspecializada.ESTADO_ACTIVA);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) c.setId(keys.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar consulta especializada: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizar(ConsultaEspecializada c) {
        String sql = "UPDATE consulta_especializada SET motivo_consulta=?, hallazgos=?, " +
                     "diagnostico=?, tratamiento=?, evolucion=?, observaciones=?, estado=? WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, c.getMotivoConsulta());
            ps.setString(2, c.getHallazgos());
            ps.setString(3, c.getDiagnostico());
            ps.setString(4, c.getTratamiento());
            ps.setString(5, c.getEvolucion());
            ps.setString(6, c.getObservaciones());
            ps.setString(7, c.getEstado());
            ps.setInt(8, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar consulta especializada: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<ConsultaEspecializada> buscarPorRemision(int remisionId) {
        String sql = BASE_SELECT + " WHERE remision_id=? ORDER BY fecha_registro DESC LIMIT 1";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, remisionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar consulta especializada: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<ConsultaEspecializada> listarPorPaciente(int pacienteId) {
        String sql = BASE_SELECT + " WHERE paciente_id=? ORDER BY fecha_registro DESC";
        List<ConsultaEspecializada> lista = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, pacienteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar consultas especializadas: " + e.getMessage(), e);
        }
        return lista;
    }

    private static final String BASE_SELECT =
        "SELECT id, remision_id, paciente_id, medico_especialista_id, motivo_consulta, " +
        "hallazgos, diagnostico, tratamiento, evolucion, observaciones, estado, fecha_registro " +
        "FROM consulta_especializada";

    private ConsultaEspecializada mapear(ResultSet rs) throws SQLException {
        ConsultaEspecializada c = new ConsultaEspecializada();
        c.setId(rs.getInt("id"));
        c.setRemisionId(rs.getInt("remision_id"));
        c.setPacienteId(rs.getInt("paciente_id"));
        c.setMedicoEspecialistaId(rs.getInt("medico_especialista_id"));
        c.setMotivoConsulta(rs.getString("motivo_consulta"));
        c.setHallazgos(rs.getString("hallazgos"));
        c.setDiagnostico(rs.getString("diagnostico"));
        c.setTratamiento(rs.getString("tratamiento"));
        c.setEvolucion(rs.getString("evolucion"));
        c.setObservaciones(rs.getString("observaciones"));
        c.setEstado(rs.getString("estado"));
        Timestamp ts = rs.getTimestamp("fecha_registro");
        if (ts != null) c.setFechaRegistro(ts.toLocalDateTime());
        return c;
    }
}
