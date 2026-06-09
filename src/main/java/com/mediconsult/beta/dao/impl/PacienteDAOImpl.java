package com.mediconsult.beta.dao.impl;

import com.mediconsult.beta.dao.interfaces.IPacienteDAO;
import com.mediconsult.beta.model.Paciente;
import com.mediconsult.beta.util.ConexionDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC de {@link IPacienteDAO}.
 *
 * <p>Usa la arquitectura de herencia de MySQL: inserta primero en {@code persona}
 * y luego en {@code paciente} con el mismo id generado.</p>
 *
 * <p><strong>MIGRACIÓN REQUERIDA:</strong> Ejecutar en MySQL antes de usar esta versión:</p>
 * <pre>
 *   ALTER TABLE paciente
 *     ADD COLUMN tipo_sangre VARCHAR(5)   DEFAULT '' AFTER historial,
 *     ADD COLUMN alergias    VARCHAR(500) DEFAULT '' AFTER tipo_sangre;
 * </pre>
 *
 * @author MediConsult Team
 * @version 0.4 — eliminación en cascada manual (cita → paciente → persona)
 */
public class PacienteDAOImpl implements IPacienteDAO {

    private final Connection conn;

    public PacienteDAOImpl() {
        this.conn = ConexionDB.getInstance().getConexion();
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
                System.out.println("[PacienteDAO] Columna 'activo' creada en persona.");
            }
        } catch (SQLException e) {
            System.err.println("[PacienteDAO] Error en migración de 'activo': " + e.getMessage());
        }
    }

    // ── CRUD ───────────────────────────────────────────────────────────────

    @Override
    public void guardar(Paciente paciente) {
        String sqlPersona  = "INSERT INTO persona (nombre, apellido, telefono, direccion, correo) VALUES (?, ?, ?, ?, ?)";
        String sqlPaciente = "INSERT INTO paciente (id, edad, historial, tipo_sangre, alergias) VALUES (?, ?, ?, ?, ?)";

        try {
            // 1. Insertar en persona y obtener el id generado
            PreparedStatement psPersona = conn.prepareStatement(sqlPersona, Statement.RETURN_GENERATED_KEYS);
            psPersona.setString(1, paciente.getNombre());
            psPersona.setString(2, paciente.getApellido());
            psPersona.setString(3, paciente.getTelefono());
            psPersona.setString(4, paciente.getDireccion());
            psPersona.setString(5, paciente.getCorreo());
            psPersona.executeUpdate();

            ResultSet keys = psPersona.getGeneratedKeys();
            if (keys.next()) {
                int idGenerado = keys.getInt(1);
                paciente.setId(idGenerado);

                // 2. Insertar en paciente con el mismo id
                PreparedStatement psPaciente = conn.prepareStatement(sqlPaciente);
                psPaciente.setInt(1, idGenerado);
                psPaciente.setInt(2, paciente.getEdad());
                psPaciente.setString(3, paciente.getHistorial());
                psPaciente.setString(4, nvl(paciente.getTipoSangre()));
                psPaciente.setString(5, nvl(paciente.getAlergias()));
                psPaciente.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar paciente: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizar(Paciente paciente) {
        String sqlPersona  = "UPDATE persona SET nombre=?, apellido=?, telefono=?, direccion=?, correo=? WHERE id=?";
        String sqlPaciente = "UPDATE paciente SET edad=?, historial=?, tipo_sangre=?, alergias=? WHERE id=?";

        try {
            PreparedStatement psPersona = conn.prepareStatement(sqlPersona);
            psPersona.setString(1, paciente.getNombre());
            psPersona.setString(2, paciente.getApellido());
            psPersona.setString(3, paciente.getTelefono());
            psPersona.setString(4, paciente.getDireccion());
            psPersona.setString(5, paciente.getCorreo());
            psPersona.setInt(6, paciente.getId());
            psPersona.executeUpdate();

            PreparedStatement psPaciente = conn.prepareStatement(sqlPaciente);
            psPaciente.setInt(1, paciente.getEdad());
            psPaciente.setString(2, paciente.getHistorial());
            psPaciente.setString(3, nvl(paciente.getTipoSangre()));
            psPaciente.setString(4, nvl(paciente.getAlergias()));
            psPaciente.setInt(5, paciente.getId());
            psPaciente.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar paciente: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminar(int id) {
        try {
            conn.setAutoCommit(false);

            // Inactivar usuario asociado (no puede iniciar sesión)
            PreparedStatement psUsuario = conn.prepareStatement(
                "UPDATE usuario SET activo = 0 WHERE paciente_id = ?"
            );
            psUsuario.setInt(1, id);
            psUsuario.executeUpdate();

            // Soft delete: el registro permanece en BD pero invisible para usuarios normales
            PreparedStatement psPersona = conn.prepareStatement(
                "UPDATE persona SET activo = 0 WHERE id = ?"
            );
            psPersona.setInt(1, id);
            psPersona.executeUpdate();

            conn.commit();

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { /* ignorar */ }
            throw new RuntimeException("Error al inactivar paciente: " + e.getMessage(), e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { /* ignorar */ }
        }
    }

    @Override
    public List<Paciente> listarInactivos() {
        String sql = BASE_SELECT + " WHERE pe.activo = 0 ORDER BY pe.apellido, pe.nombre";
        List<Paciente> lista = new ArrayList<>();
        try {
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar pacientes inactivos: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public void reactivar(int id) {
        try {
            conn.setAutoCommit(false);

            PreparedStatement psPersona = conn.prepareStatement(
                "UPDATE persona SET activo = 1 WHERE id = ?"
            );
            psPersona.setInt(1, id);
            psPersona.executeUpdate();

            PreparedStatement psUsuario = conn.prepareStatement(
                "UPDATE usuario SET activo = 1 WHERE paciente_id = ?"
            );
            psUsuario.setInt(1, id);
            psUsuario.executeUpdate();

            conn.commit();

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { /* ignorar */ }
            throw new RuntimeException("Error al reactivar paciente: " + e.getMessage(), e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { /* ignorar */ }
        }
    }

    @Override
    public Optional<Paciente> buscarPorId(int id) {
        String sql = BASE_SELECT + " WHERE pe.id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar paciente por id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Paciente> listarTodos() {
        String sql = BASE_SELECT + " WHERE pe.activo = 1 ORDER BY pe.apellido, pe.nombre";
        List<Paciente> lista = new ArrayList<>();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar pacientes: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public List<Paciente> buscarPorNombre(String nombre) {
        String sql = BASE_SELECT + " WHERE pe.activo = 1 AND LOWER(CONCAT(pe.nombre, ' ', pe.apellido)) LIKE LOWER(?)";
        List<Paciente> lista = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, "%" + nombre + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar paciente por nombre: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public void actualizarHistorial(int id, String historial) {
        String sql = "UPDATE paciente SET historial=? WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, historial);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar historial: " + e.getMessage(), e);
        }
    }

    // ── Helper de mapeo ────────────────────────────────────────────────────

    private static final String BASE_SELECT =
        "SELECT pe.id, pe.nombre, pe.apellido, pe.telefono, pe.direccion, pe.correo, " +
        "pa.edad, pa.historial, pa.tipo_sangre, pa.alergias " +
        "FROM persona pe JOIN paciente pa ON pe.id = pa.id";

    private Paciente mapear(ResultSet rs) throws SQLException {
        return new Paciente(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("apellido"),
                rs.getString("telefono"),
                rs.getString("direccion"),
                rs.getString("correo"),
                rs.getInt("edad"),
                rs.getString("historial"),
                rs.getString("tipo_sangre"),
                rs.getString("alergias")
        );
    }

    /** Convierte null a cadena vacía para evitar NPE en PreparedStatement. */
    private static String nvl(String s) {
        return s != null ? s : "";
    }
}