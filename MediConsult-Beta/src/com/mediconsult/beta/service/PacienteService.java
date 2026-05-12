package com.mediconsult.beta.service;

import com.mediconsult.beta.dao.impl.PacienteDAOImpl;
import com.mediconsult.beta.dao.interfaces.IPacienteDAO;
import com.mediconsult.beta.model.Paciente;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de negocio para pacientes.
 *
 * <p>Valida datos y coordina la persistencia sin exponer CSV a la vista.</p>
 *
 * <p>Permite inyección de dependencias para pruebas o variantes del DAO.</p>
 */
public class PacienteService {

    private final IPacienteDAO pacienteDAO;

    /** Usa el DAO real por defecto. */
    public PacienteService() {
        this.pacienteDAO = new PacienteDAOImpl();
    }

    /** Inyección de dependencia (tests). */
    public PacienteService(IPacienteDAO pacienteDAO) {
        this.pacienteDAO = pacienteDAO;
    }

    // ── Escritura ─────────────────────────────────────────────

    /**
     * Registra un paciente nuevo.
     */
    public Paciente registrarPaciente(String nombre, int edad, String telefono) {
        validar(nombre != null && !nombre.isBlank(), "Nombre vacío");
        validar(edad > 0, "Edad inválida");

        Paciente paciente = new Paciente(nombre.trim(), edad, telefono);
        pacienteDAO.guardar(paciente);
        return paciente;
    }

    /**
     * Actualiza datos de un paciente existente.
     */
    public void actualizarPaciente(Paciente paciente) {
        validar(paciente != null, "Paciente nulo");
        validar(paciente.getId() != 0, "ID inválido");
        validar(paciente.getNombre() != null && !paciente.getNombre().isBlank(), "Nombre vacío");
        validar(paciente.getEdad() > 0, "Edad inválida");

        pacienteDAO.actualizar(paciente);
    }

    /**
     * Elimina un paciente por ID.
     */
    public void eliminarPaciente(int id) {
        pacienteDAO.eliminar(id);
    }

    /**
     * Agrega una entrada al historial (se conserva lo anterior).
     */
    public void actualizarHistorial(int pacienteId, String nuevaEntrada) {
        validar(nuevaEntrada != null && !nuevaEntrada.isBlank(), "Entrada vacía");

        Paciente paciente = pacienteDAO.buscarPorId(pacienteId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));

        String actual = paciente.getHistorial() == null ? "" : paciente.getHistorial();
        String entrada = "[" + LocalDate.now() + "] " + nuevaEntrada.trim();

        String nuevoHistorial = actual.isBlank()
                ? entrada
                : entrada + "\n" + actual;

        pacienteDAO.actualizarHistorial(pacienteId, nuevoHistorial);
    }

    // ── Consulta ─────────────────────────────────────────────

    public Optional<Paciente> obtenerPorId(int id) {
        return pacienteDAO.buscarPorId(id);
    }

    public List<Paciente> listarTodos() {
        return pacienteDAO.listarTodos();
    }

    public List<Paciente> buscarPorNombre(String nombre) {
        validar(nombre != null && !nombre.isBlank(), "Búsqueda vacía");
        return pacienteDAO.buscarPorNombre(nombre.trim());
    }

    // ── Util ────────────────────────────────────────────────

    private void validar(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalArgumentException(mensaje);
    }
}