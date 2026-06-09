package com.mediconsult.beta.dao.impl;

import com.mediconsult.beta.dao.interfaces.IAltaMedicaDAO;
import com.mediconsult.beta.model.AltaMedica;
import com.mediconsult.beta.util.ConexionDB;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AltaMedicaDAOImpl implements IAltaMedicaDAO {

    private final Connection conn;

    public AltaMedicaDAOImpl() {
        this.conn = ConexionDB.getInstance().getConexion();
        crearTablaIfNotExists();
    }

    private void crearTablaIfNotExists() {
        // FKs a remision y consulta_especializada se omiten aquí: ambas son auto-creadas y pueden
        // no existir aún cuando este constructor se ejecuta. La integridad se garantiza en la
        // capa de servicio y en DB.sql (instalación limpia).
        String sql =
            "CREATE TABLE IF NOT EXISTS alta_medica (" +
            "  id                            INT AUTO_INCREMENT PRIMARY KEY," +
            "  remision_id                   INT  NOT NULL," +
            "  consulta_especializada_id     INT  NOT NULL," +
            "  paciente_id                   INT  NOT NULL," +
            "  medico_especialista_id        INT  NOT NULL," +
            "  diagnostico_final             TEXT," +
            "  tipo_alta                     VARCHAR(30) NOT NULL," +
            "  indicaciones_paciente         TEXT," +
            "  indicaciones_medico_remitente TEXT," +
            "  observaciones                 TEXT," +
            "  fecha_alta                    DATE NOT NULL," +
            "  fecha_registro                TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (paciente_id)            REFERENCES paciente(id) ON DELETE CASCADE," +
            "  FOREIGN KEY (medico_especialista_id) REFERENCES medico(id)   ON DELETE CASCADE" +
            ")";
        try {
            conn.createStatement().execute(sql);
            System.out.println("[AltaMedicaDAO] Tabla 'alta_medica' verificada.");
        } catch (SQLException e) {
            System.err.println("[AltaMedicaDAO] Error en migración: " + e.getMessage());
        }
    }

    @Override
    public void guardar(AltaMedica a) {
        String sql = "INSERT INTO alta_medica " +
                     "(remision_id, consulta_especializada_id, paciente_id, medico_especialista_id, " +
                     " diagnostico_final, tipo_alta, indicaciones_paciente, " +
                     " indicaciones_medico_remitente, observaciones, fecha_alta) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, a.getRemisionId());
            ps.setInt(2, a.getConsultaEspecializadaId());
            ps.setInt(3, a.getPacienteId());
            ps.setInt(4, a.getMedicoEspecialistaId());
            ps.setString(5, a.getDiagnosticoFinal());
            ps.setString(6, a.getTipoAlta());
            ps.setString(7, a.getIndicacionesPaciente());
            ps.setString(8, a.getIndicacionesMedicoRemitente());
            ps.setString(9, a.getObservaciones());
            ps.setDate(10, a.getFechaAlta() != null
                    ? Date.valueOf(a.getFechaAlta()) : Date.valueOf(LocalDate.now()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) a.setId(keys.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar alta médica: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<AltaMedica> buscarPorRemision(int remisionId) {
        String sql = BASE_SELECT + " WHERE remision_id=? ORDER BY fecha_registro DESC LIMIT 1";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, remisionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar alta médica: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<AltaMedica> listarPorPaciente(int pacienteId) {
        String sql = BASE_SELECT + " WHERE paciente_id=? ORDER BY fecha_alta DESC";
        List<AltaMedica> lista = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, pacienteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar altas médicas: " + e.getMessage(), e);
        }
        return lista;
    }

    private static final String BASE_SELECT =
        "SELECT id, remision_id, consulta_especializada_id, paciente_id, medico_especialista_id, " +
        "diagnostico_final, tipo_alta, indicaciones_paciente, indicaciones_medico_remitente, " +
        "observaciones, fecha_alta, fecha_registro FROM alta_medica";

    private AltaMedica mapear(ResultSet rs) throws SQLException {
        AltaMedica a = new AltaMedica();
        a.setId(rs.getInt("id"));
        a.setRemisionId(rs.getInt("remision_id"));
        a.setConsultaEspecializadaId(rs.getInt("consulta_especializada_id"));
        a.setPacienteId(rs.getInt("paciente_id"));
        a.setMedicoEspecialistaId(rs.getInt("medico_especialista_id"));
        a.setDiagnosticoFinal(rs.getString("diagnostico_final"));
        a.setTipoAlta(rs.getString("tipo_alta"));
        a.setIndicacionesPaciente(rs.getString("indicaciones_paciente"));
        a.setIndicacionesMedicoRemitente(rs.getString("indicaciones_medico_remitente"));
        a.setObservaciones(rs.getString("observaciones"));
        Date fa = rs.getDate("fecha_alta");
        if (fa != null) a.setFechaAlta(fa.toLocalDate());
        Timestamp ts = rs.getTimestamp("fecha_registro");
        if (ts != null) a.setFechaRegistro(ts.toLocalDateTime());
        return a;
    }
}
