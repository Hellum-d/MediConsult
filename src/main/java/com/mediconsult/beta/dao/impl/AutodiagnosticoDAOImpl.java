package com.mediconsult.beta.dao.impl;

import com.mediconsult.beta.dao.interfaces.IAutodiagnosticoDAO;
import com.mediconsult.beta.model.Autodiagnostico;
import com.mediconsult.beta.util.ConexionDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC de {@link IAutodiagnosticoDAO}.
 *
 * <p>Crea la tabla {@code autodiagnostico} automáticamente si no existe
 * (migración no destructiva). Compatible con la BD {@code clinex} actual.</p>
 */
public class AutodiagnosticoDAOImpl implements IAutodiagnosticoDAO {

    private final Connection conn;

    public AutodiagnosticoDAOImpl() {
        this.conn = ConexionDB.getInstance().getConexion();
        crearTablaIfNotExists();
        migrarColumnaImagenPath();
    }

    // ── Migración automática ───────────────────────────────────────────────

    private void crearTablaIfNotExists() {
        String sql =
            "CREATE TABLE IF NOT EXISTS autodiagnostico (" +
            "  id             INT AUTO_INCREMENT PRIMARY KEY," +
            "  paciente_id    INT NOT NULL," +
            "  sintomas       TEXT NOT NULL," +
            "  resultado_ia   TEXT," +
            "  fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (paciente_id) REFERENCES paciente(id)" +
            "    ON DELETE CASCADE ON UPDATE CASCADE" +
            ")";
        try {
            conn.createStatement().execute(sql);
            System.out.println("[AutodiagnosticoDAO] Tabla 'autodiagnostico' verificada.");
        } catch (SQLException e) {
            System.err.println("[AutodiagnosticoDAO] Error en migración: " + e.getMessage());
        }
    }

    private void migrarColumnaImagenPath() {
        try {
            PreparedStatement check = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'autodiagnostico' AND column_name = 'imagen_path'"
            );
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) == 0) {
                conn.createStatement().execute(
                    "ALTER TABLE autodiagnostico ADD COLUMN imagen_path VARCHAR(500) NULL"
                );
                System.out.println("[AutodiagnosticoDAO] Columna 'imagen_path' creada.");
            }
        } catch (SQLException e) {
            System.err.println("[AutodiagnosticoDAO] Error migrando imagen_path: " + e.getMessage());
        }
    }

    // ── Implementación ─────────────────────────────────────────────────────

    @Override
    public void guardar(Autodiagnostico a) {
        String sql = "INSERT INTO autodiagnostico (paciente_id, sintomas, resultado_ia, imagen_path) VALUES (?,?,?,?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, a.getPacienteId());
            ps.setString(2, a.getSintomas());
            ps.setString(3, a.getResultadoIa() != null ? a.getResultadoIa() : "");
            if (a.getImagenPath() != null && !a.getImagenPath().isBlank())
                ps.setString(4, a.getImagenPath());
            else
                ps.setNull(4, Types.VARCHAR);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) a.setId(keys.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar autodiagnóstico: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizarResultado(int id, String resultadoIa) {
        String sql = "UPDATE autodiagnostico SET resultado_ia=? WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, resultadoIa);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar resultado IA: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Autodiagnostico> listarPorPaciente(int pacienteId) {
        String sql = "SELECT id, paciente_id, sintomas, resultado_ia, fecha_registro, imagen_path " +
                     "FROM autodiagnostico WHERE paciente_id=? " +
                     "ORDER BY fecha_registro DESC";
        List<Autodiagnostico> lista = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, pacienteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar autodiagnósticos: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public Optional<Autodiagnostico> buscarPorId(int id) {
        String sql = "SELECT id, paciente_id, sintomas, resultado_ia, fecha_registro, imagen_path " +
                     "FROM autodiagnostico WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar autodiagnóstico: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    // ── Helper ─────────────────────────────────────────────────────────────

    private Autodiagnostico mapear(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("fecha_registro");
        LocalDateTime fecha = ts != null ? ts.toLocalDateTime() : null;
        Autodiagnostico ad = new Autodiagnostico(
                rs.getInt("id"),
                rs.getInt("paciente_id"),
                rs.getString("sintomas"),
                rs.getString("resultado_ia"),
                fecha
        );
        try { ad.setImagenPath(rs.getString("imagen_path")); } catch (SQLException ignored) {}
        return ad;
    }
}
