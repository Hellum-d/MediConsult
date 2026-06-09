package com.mediconsult.beta.dao.impl;

import com.mediconsult.beta.dao.interfaces.IUsuarioDAO;
import com.mediconsult.beta.model.Usuario;
import com.mediconsult.beta.util.ConexionDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementación JDBC de {@link IUsuarioDAO}.
 *
 * <p>Al instanciarse ejecuta migraciones no destructivas sobre la tabla
 * {@code usuario}: expande el ENUM de roles y agrega las columnas
 * {@code paciente_id}, {@code reset_token} y {@code reset_expires}.</p>
 *
 * @author MediConsult Team
 * @version 0.3 — roles expandidos, registro y recuperación de contraseña
 */
public class UsuarioDAOImpl implements IUsuarioDAO {

    private final Connection conn;

    public UsuarioDAOImpl() {
        this.conn = ConexionDB.getInstance().getConexion();
        migrarTablaUsuario();
    }

    // ── Migración automática ───────────────────────────────────────────────

    private void migrarTablaUsuario() {
        try {
            // 1. Expandir ENUM de roles si faltan los nuevos valores
            PreparedStatement checkEnum = conn.prepareStatement(
                "SELECT COLUMN_TYPE FROM information_schema.columns " +
                "WHERE table_schema=DATABASE() AND table_name='usuario' AND column_name='rol'"
            );
            ResultSet rsEnum = checkEnum.executeQuery();
            if (rsEnum.next()) {
                String type = rsEnum.getString(1);
                if (!type.contains("PACIENTE")) {
                    conn.createStatement().execute(
                        "ALTER TABLE usuario MODIFY COLUMN rol " +
                        "ENUM('ADMIN','MEDICO','MEDICO_GENERAL','MEDICO_ESPECIALISTA','PACIENTE') NOT NULL"
                    );
                    System.out.println("[UsuarioDAO] ENUM de roles expandido.");
                }
            }

            // 2. Agregar columnas nuevas si no existen
            String[][] columnas = {
                {"paciente_id",   "INT NULL"},
                {"medico_id",     "INT NULL"},
                {"reset_token",   "VARCHAR(10) NULL"},
                {"reset_expires", "DATETIME NULL"},
                {"activo",        "TINYINT(1) NOT NULL DEFAULT 1"}
            };
            for (String[] col : columnas) {
                PreparedStatement ck = conn.prepareStatement(
                    "SELECT COUNT(*) FROM information_schema.columns " +
                    "WHERE table_schema=DATABASE() AND table_name='usuario' AND column_name=?"
                );
                ck.setString(1, col[0]);
                ResultSet rs = ck.executeQuery();
                rs.next();
                if (rs.getInt(1) == 0) {
                    conn.createStatement().execute(
                        "ALTER TABLE usuario ADD COLUMN " + col[0] + " " + col[1]
                    );
                    System.out.println("[UsuarioDAO] Columna '" + col[0] + "' agregada.");
                }
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO] Error en migración: " + e.getMessage());
        }
    }

    // ── Implementación de IUsuarioDAO ──────────────────────────────────────

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        String sql = "SELECT id, username, password_hash, rol, paciente_id, medico_id " +
                     "FROM usuario WHERE username = ? AND activo = 1";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar usuario: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public void guardar(Usuario usuario) {
        String sql = "INSERT INTO usuario (username, password_hash, rol, paciente_id, medico_id) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, usuario.getUsername());
            ps.setString(2, usuario.getPassword());
            ps.setString(3, usuario.getRol());
            if (usuario.getPacienteId() > 0) ps.setInt(4, usuario.getPacienteId());
            else ps.setNull(4, Types.INTEGER);
            if (usuario.getMedicoId() > 0) ps.setInt(5, usuario.getMedicoId());
            else ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) usuario.setId(keys.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizarPasswordHash(int userId, String nuevoHash) {
        String sql = "UPDATE usuario SET password_hash=? WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, nuevoHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar contraseña: " + e.getMessage(), e);
        }
    }

    @Override
    public void guardarResetToken(int userId, String token, LocalDateTime expires) {
        String sql = "UPDATE usuario SET reset_token=?, reset_expires=? WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, token);
            ps.setTimestamp(2, Timestamp.valueOf(expires));
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar token: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorResetToken(String token) {
        String sql = "SELECT id, username, password_hash, rol, paciente_id, medico_id " +
                     "FROM usuario WHERE reset_token=? AND reset_expires > NOW()";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar token: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public void limpiarResetToken(int userId) {
        String sql = "UPDATE usuario SET reset_token=NULL, reset_expires=NULL WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al limpiar token: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existeUsername(String username) {
        String sql = "SELECT COUNT(*) FROM usuario WHERE username=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar username: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorPacienteId(int pacienteId) {
        String sql = "SELECT id, username, password_hash, rol, paciente_id, medico_id " +
                     "FROM usuario WHERE paciente_id=? LIMIT 1";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, pacienteId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar usuario por paciente: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public void actualizarMedicoId(int userId, int medicoId) {
        String sql = "UPDATE usuario SET medico_id=? WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            if (medicoId > 0) ps.setInt(1, medicoId);
            else ps.setNull(1, Types.INTEGER);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar medico_id: " + e.getMessage(), e);
        }
    }

    @Override
    public void inactivar(int id) {
        String sql = "UPDATE usuario SET activo = 0 WHERE id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al inactivar usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public void reactivar(int id) {
        String sql = "UPDATE usuario SET activo = 1 WHERE id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al reactivar usuario: " + e.getMessage(), e);
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────────

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("rol")
        );
        u.setPacienteId(rs.getInt("paciente_id")); // 0 si NULL
        try { u.setMedicoId(rs.getInt("medico_id")); } catch (SQLException ignored) {}
        return u;
    }
}
