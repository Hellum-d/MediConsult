package com.mediconsult.beta.dao.impl;

import com.mediconsult.beta.dao.interfaces.IDiagnosticoDAO;
import com.mediconsult.beta.model.Diagnostico;
import com.mediconsult.beta.util.ConexionDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC de {@link IDiagnosticoDAO}.
 *
 * <p>El método {@code listarPorPaciente} ahora hace un JOIN real en MySQL
 * en lugar del cruce en memoria que hacía la versión CSV.</p>
 *
 * @author MediConsult Team
 * @version 0.2 — migración JDBC
 */
public class DiagnosticoDAOImpl implements IDiagnosticoDAO {

    private final Connection conn;

    public DiagnosticoDAOImpl() {
        this.conn = ConexionDB.getInstance().getConexion();
    }

    @Override
    public void guardar(Diagnostico diagnostico) {
        String sql = "INSERT INTO diagnostico (cita_id, sintomas, respuesta_ia) VALUES (?, ?, ?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, diagnostico.getCitaId());
            ps.setString(2, diagnostico.getSintomas());
            ps.setString(3, diagnostico.getResultadoIa());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) diagnostico.setId(keys.getInt(1));

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar diagnóstico: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizar(Diagnostico diagnostico) {
        String sql = "UPDATE diagnostico SET sintomas=?, respuesta_ia=?, diagnostico_final=?, tratamiento=? WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, diagnostico.getSintomas());
            ps.setString(2, diagnostico.getResultadoIa());
            ps.setString(3, null); // diagnostico_final — futuro uso
            ps.setString(4, null); // tratamiento — futuro uso
            ps.setInt(5, diagnostico.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar diagnóstico: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Diagnostico> buscarPorId(int id) {
        String sql = "SELECT id, cita_id, sintomas, respuesta_ia FROM diagnostico WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar diagnóstico: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Diagnostico> buscarPorCita(int citaId) {
        String sql = "SELECT id, cita_id, sintomas, respuesta_ia FROM diagnostico WHERE cita_id=? LIMIT 1";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, citaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar diagnóstico por cita: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Diagnostico> listarPorPaciente(int pacienteId) {
        // JOIN real en MySQL — equivalente al cruce en memoria de la versión CSV
        String sql = "SELECT d.id, d.cita_id, d.sintomas, d.respuesta_ia " +
                     "FROM diagnostico d " +
                     "JOIN cita c ON d.cita_id = c.id " +
                     "WHERE c.paciente_id = ? " +
                     "ORDER BY c.fecha DESC";
        List<Diagnostico> lista = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, pacienteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar diagnósticos por paciente: " + e.getMessage(), e);
        }
        return lista;
    }

    // ── Helper de mapeo ────────────────────────────────────────────────────

    private Diagnostico mapear(ResultSet rs) throws SQLException {
        return new Diagnostico(
                rs.getInt("id"),
                rs.getInt("cita_id"),
                rs.getString("sintomas"),
                rs.getString("respuesta_ia")
        );
    }
}
