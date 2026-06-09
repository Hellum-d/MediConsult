package com.mediconsult.beta.dao.impl;

import com.mediconsult.beta.dao.interfaces.IMedicoDAO;
import com.mediconsult.beta.model.Medico;
import com.mediconsult.beta.util.ConexionDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC de {@link IMedicoDAO}.
 *
 * <p>Al inicializarse, verifica que la columna {@code estado} exista en la
 * tabla {@code medico} y la añade automáticamente si no está (migración
 * no destructiva). Esto permite actualizar el sistema sin pérdida de datos.</p>
 *
 * @author MediConsult Team
 * @version 0.3 — estados clínicos múltiples
 */
public class MedicoDAOImpl implements IMedicoDAO {

    private final Connection conn;

    public MedicoDAOImpl() {
        this.conn = ConexionDB.getInstance().getConexion();
        migrarColumnaEstado();
        migrarColumnaActivo();
    }

    private void migrarColumnaActivo() {
        try {
            PreparedStatement check = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'persona' AND column_name = 'activo'"
            );
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) == 0) {
                conn.createStatement().execute(
                    "ALTER TABLE persona ADD COLUMN activo TINYINT(1) NOT NULL DEFAULT 1"
                );
                System.out.println("[MedicoDAO] Columna 'activo' creada en persona.");
            }
        } catch (SQLException e) {
            System.err.println("[MedicoDAO] Error en migración de 'activo': " + e.getMessage());
        }
    }

    // ── Migración automática ───────────────────────────────────────────────

    /**
     * Añade la columna {@code estado} si no existe, usando INFORMATION_SCHEMA
     * para verificar primero (compatible con MySQL 5.7+ y MySQL 8.x).
     * Es idempotente: no hace nada si la columna ya existe.
     */
    private void migrarColumnaEstado() {
        try {
            // Verificar si la columna 'estado' ya existe en la tabla 'medico'
            PreparedStatement check = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() " +
                "  AND table_name   = 'medico' " +
                "  AND column_name  = 'estado'"
            );
            ResultSet rs = check.executeQuery();
            rs.next();
            boolean columnaExiste = rs.getInt(1) > 0;

            if (!columnaExiste) {
                // Añadir la columna (sin IF NOT EXISTS — compatible con MySQL estándar)
                conn.createStatement().execute(
                    "ALTER TABLE medico " +
                    "ADD COLUMN estado VARCHAR(30) NOT NULL DEFAULT 'Disponible'"
                );
                // Inicializar: médicos no disponibles → 'Fuera de turno'
                conn.createStatement().execute(
                    "UPDATE medico SET estado = 'Fuera de turno' WHERE disponible = 0"
                );
                System.out.println("[MedicoDAO] Columna 'estado' creada e inicializada.");
            }
        } catch (SQLException e) {
            System.err.println("[MedicoDAO] Error en migración de 'estado': " + e.getMessage());
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    @Override
    public void guardar(Medico medico) {
        String sqlPersona = "INSERT INTO persona (nombre, apellido, telefono, direccion, correo) VALUES (?, ?, ?, ?, ?)";
        String sqlMedico  = "INSERT INTO medico (id, especialidad, disponible, estado) VALUES (?, ?, ?, ?)";

        try {
            PreparedStatement psPersona = conn.prepareStatement(sqlPersona, Statement.RETURN_GENERATED_KEYS);
            psPersona.setString(1, medico.getNombre());
            psPersona.setString(2, medico.getApellido());
            psPersona.setString(3, medico.getTelefono());
            psPersona.setString(4, medico.getDireccion());
            psPersona.setString(5, medico.getCorreo());
            psPersona.executeUpdate();

            ResultSet keys = psPersona.getGeneratedKeys();
            if (keys.next()) {
                int idGenerado = keys.getInt(1);
                medico.setId(idGenerado);

                PreparedStatement psMedico = conn.prepareStatement(sqlMedico);
                psMedico.setInt(1, idGenerado);
                psMedico.setString(2, medico.getEspecialidad());
                psMedico.setBoolean(3, medico.isDisponible());
                psMedico.setString(4, medico.getEstado() != null
                        ? medico.getEstado() : Medico.ESTADO_DISPONIBLE);
                psMedico.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar médico: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizar(Medico medico) {
        String sqlPersona = "UPDATE persona SET nombre=?, apellido=?, telefono=?, direccion=?, correo=? WHERE id=?";
        String sqlMedico  = "UPDATE medico SET especialidad=?, disponible=?, estado=? WHERE id=?";

        try {
            PreparedStatement psPersona = conn.prepareStatement(sqlPersona);
            psPersona.setString(1, medico.getNombre());
            psPersona.setString(2, medico.getApellido());
            psPersona.setString(3, medico.getTelefono());
            psPersona.setString(4, medico.getDireccion());
            psPersona.setString(5, medico.getCorreo());
            psPersona.setInt(6, medico.getId());
            psPersona.executeUpdate();

            PreparedStatement psMedico = conn.prepareStatement(sqlMedico);
            psMedico.setString(1, medico.getEspecialidad());
            psMedico.setBoolean(2, medico.isDisponible());
            psMedico.setString(3, medico.getEstado() != null
                    ? medico.getEstado() : Medico.ESTADO_DISPONIBLE);
            psMedico.setInt(4, medico.getId());
            psMedico.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar médico: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminar(int id) {
        try {
            // Inactivar usuario asociado (no puede iniciar sesión)
            PreparedStatement psUsuario = conn.prepareStatement(
                "UPDATE usuario SET activo = 0 WHERE medico_id = ?"
            );
            psUsuario.setInt(1, id);
            psUsuario.executeUpdate();

            // Soft delete: el registro permanece en BD pero invisible para usuarios normales
            PreparedStatement psPersona = conn.prepareStatement(
                "UPDATE persona SET activo = 0 WHERE id = ?"
            );
            psPersona.setInt(1, id);
            psPersona.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al inactivar médico: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Medico> listarInactivos() {
        String sql = buildSelect() + " WHERE pe.activo = 0 ORDER BY pe.apellido, pe.nombre";
        List<Medico> lista = new ArrayList<>();
        try {
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar médicos inactivos: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public void reactivar(int id) {
        try {
            PreparedStatement psPersona = conn.prepareStatement(
                "UPDATE persona SET activo = 1 WHERE id = ?"
            );
            psPersona.setInt(1, id);
            psPersona.executeUpdate();

            PreparedStatement psUsuario = conn.prepareStatement(
                "UPDATE usuario SET activo = 1 WHERE medico_id = ?"
            );
            psUsuario.setInt(1, id);
            psUsuario.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al reactivar médico: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Medico> buscarPorId(int id) {
        String sql = buildSelect() + " WHERE pe.id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar médico por id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Medico> listarTodos() {
        String sql = buildSelect() + " WHERE pe.activo = 1 ORDER BY pe.apellido, pe.nombre";
        List<Medico> lista = new ArrayList<>();
        try {
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar médicos: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public List<Medico> listarDisponibles() {
        String sql = buildSelect() + " WHERE pe.activo = 1 AND me.disponible = TRUE ORDER BY pe.apellido";
        List<Medico> lista = new ArrayList<>();
        try {
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar médicos disponibles: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public List<Medico> buscarPorEspecialidad(String especialidad) {
        String sql = buildSelect() + " WHERE pe.activo = 1 AND LOWER(me.especialidad) LIKE LOWER(?)";
        List<Medico> lista = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, "%" + especialidad + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar por especialidad: " + e.getMessage(), e);
        }
        return lista;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String buildSelect() {
        return "SELECT pe.id, pe.nombre, pe.apellido, pe.telefono, pe.direccion, pe.correo, " +
               "me.especialidad, me.disponible, me.estado " +
               "FROM persona pe JOIN medico me ON pe.id = me.id";
    }

    private Medico mapear(ResultSet rs) throws SQLException {
        String estadoRaw = null;
        try {
            estadoRaw = rs.getString("estado");
        } catch (SQLException ignored) {
            // La columna aún no existe en BD antigua — se infiere de disponible
        }
        return new Medico(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("apellido"),
                rs.getString("telefono"),
                rs.getString("direccion"),
                rs.getString("correo"),
                rs.getString("especialidad"),
                rs.getBoolean("disponible"),
                estadoRaw
        );
    }
}
