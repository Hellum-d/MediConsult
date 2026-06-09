package com.mediconsult.beta.dao.impl;

import com.mediconsult.beta.dao.interfaces.IOrdenEspecializadaDAO;
import com.mediconsult.beta.model.OrdenEspecializada;
import com.mediconsult.beta.util.ConexionDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrdenEspecializadaDAOImpl implements IOrdenEspecializadaDAO {

    private final Connection conn;

    public OrdenEspecializadaDAOImpl() {
        this.conn = ConexionDB.getInstance().getConexion();
        crearTablaIfNotExists();
    }

    private void crearTablaIfNotExists() {
        // FK a consulta_especializada se omite aquí: esa tabla es auto-creada y puede no existir aún.
        // La integridad se garantiza en la capa de servicio y en DB.sql (instalación limpia).
        String sql =
            "CREATE TABLE IF NOT EXISTS orden_especializada (" +
            "  id                        INT AUTO_INCREMENT PRIMARY KEY," +
            "  consulta_especializada_id INT         NOT NULL," +
            "  paciente_id               INT         NOT NULL," +
            "  medico_id                 INT         NOT NULL," +
            "  tipo                      VARCHAR(30) NOT NULL," +
            "  descripcion               TEXT        NOT NULL," +
            "  observaciones             TEXT," +
            "  estado                    VARCHAR(20) NOT NULL DEFAULT 'ACTIVA'," +
            "  fecha_registro            TIMESTAMP   DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (paciente_id) REFERENCES paciente(id) ON DELETE CASCADE," +
            "  FOREIGN KEY (medico_id)   REFERENCES medico(id)   ON DELETE CASCADE" +
            ")";
        try {
            conn.createStatement().execute(sql);
            System.out.println("[OrdenEspDAO] Tabla 'orden_especializada' verificada.");
        } catch (SQLException e) {
            System.err.println("[OrdenEspDAO] Error en migración: " + e.getMessage());
        }
    }

    @Override
    public void guardar(OrdenEspecializada o) {
        String sql = "INSERT INTO orden_especializada " +
                     "(consulta_especializada_id, paciente_id, medico_id, " +
                     " tipo, descripcion, observaciones, estado) " +
                     "VALUES (?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, o.getConsultaEspecializadaId());
            ps.setInt(2, o.getPacienteId());
            ps.setInt(3, o.getMedicoId());
            ps.setString(4, o.getTipo());
            ps.setString(5, o.getDescripcion());
            ps.setString(6, o.getObservaciones());
            ps.setString(7, o.getEstado() != null ? o.getEstado() : OrdenEspecializada.ESTADO_ACTIVA);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) o.setId(keys.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar orden especializada: " + e.getMessage(), e);
        }
    }

    @Override
    public List<OrdenEspecializada> listarPorConsulta(int consultaEspecializadaId) {
        String sql = BASE_SELECT + " WHERE consulta_especializada_id=? ORDER BY fecha_registro DESC";
        List<OrdenEspecializada> lista = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, consultaEspecializadaId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar órdenes especializadas: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public List<OrdenEspecializada> listarPorPaciente(int pacienteId) {
        String sql = BASE_SELECT + " WHERE paciente_id=? ORDER BY fecha_registro DESC";
        List<OrdenEspecializada> lista = new ArrayList<>();
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
        String sql = "DELETE FROM orden_especializada WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar orden especializada: " + e.getMessage(), e);
        }
    }

    private static final String BASE_SELECT =
        "SELECT id, consulta_especializada_id, paciente_id, medico_id, " +
        "tipo, descripcion, observaciones, estado, fecha_registro FROM orden_especializada";

    private OrdenEspecializada mapear(ResultSet rs) throws SQLException {
        OrdenEspecializada o = new OrdenEspecializada();
        o.setId(rs.getInt("id"));
        o.setConsultaEspecializadaId(rs.getInt("consulta_especializada_id"));
        o.setPacienteId(rs.getInt("paciente_id"));
        o.setMedicoId(rs.getInt("medico_id"));
        o.setTipo(rs.getString("tipo"));
        o.setDescripcion(rs.getString("descripcion"));
        o.setObservaciones(rs.getString("observaciones"));
        o.setEstado(rs.getString("estado"));
        Timestamp ts = rs.getTimestamp("fecha_registro");
        if (ts != null) o.setFechaRegistro(ts.toLocalDateTime());
        return o;
    }
}
