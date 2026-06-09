package com.mediconsult.beta.service;

import com.mediconsult.beta.dao.impl.PacienteDAOImpl;
import com.mediconsult.beta.dao.interfaces.IPacienteDAO;
import com.mediconsult.beta.model.Paciente;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de negocio para la gestión de pacientes en MediConsult-Beta.
 *
 * @author MediConsult Team
 * @version 0.3 — campos tipo_sangre y alergias
 */
public class PacienteService {

    private final IPacienteDAO pacienteDAO;

    /** Constructor por defecto: instancia el DAO real. */
    public PacienteService() {
        this.pacienteDAO = new PacienteDAOImpl();
    }

    /** Constructor para inyección de dependencia (tests o alternativas). */
    public PacienteService(IPacienteDAO pacienteDAO) {
        this.pacienteDAO = pacienteDAO;
    }

    // ── Operaciones de escritura ──────────────────────────────────────────

    /**
     * Registra un nuevo paciente en el sistema.
     *
     * @param nombre     nombre completo; no puede ser nulo ni vacío
     * @param edad       edad en años; debe ser mayor que 0
     * @param telefono   teléfono de contacto (puede ser null o vacío)
     * @param tipoSangre tipo de sangre (ej: "O+"); puede ser null o vacío
     * @param alergias   alergias separadas por coma; puede ser null o vacío
     * @return paciente persistido con su {@code id} asignado
     */
    public Paciente registrarPaciente(String nombre, int edad, String telefono,
                                      String tipoSangre, String alergias) {
        validar(nombre != null && !nombre.trim().isEmpty(), "El nombre del paciente no puede estar vacío.");
        validar(edad > 0, "La edad debe ser mayor que cero.");

        // Separar nombre en nombre + apellido (el controller ya los envía separados por espacio)
        String[] partes   = nombre.trim().split(" ", 2);
        String nomParte   = partes[0];
        String apeParte   = partes.length > 1 ? partes[1] : "";

        Paciente paciente = new Paciente(nomParte, apeParte, edad, telefono, "",
                                         tipoSangre, alergias);
        pacienteDAO.guardar(paciente);
        return paciente;
    }

    /**
     * Registra un nuevo paciente (versión legacy sin datos médicos).
     * Delega al método completo con campos vacíos.
     */
    public Paciente registrarPaciente(String nombre, int edad, String telefono) {
        return registrarPaciente(nombre, edad, telefono, "", "");
    }

    /**
     * Registra un nuevo paciente con todos los campos individuales.
     * Usado por el formulario de auto-registro de pacientes.
     */
    public Paciente registrarPaciente(String nombre, String apellido, int edad,
                                      String telefono, String correo, String direccion,
                                      String tipoSangre, String alergias) {
        validar(nombre != null && !nombre.trim().isEmpty(), "El nombre no puede estar vacío.");
        validar(apellido != null && !apellido.trim().isEmpty(), "El apellido no puede estar vacío.");
        validar(edad > 0 && edad <= 120, "La edad debe estar entre 1 y 120 años.");

        Paciente paciente = new Paciente(0, nombre.trim(), apellido.trim(),
                nvl(telefono), nvl(direccion), nvl(correo),
                edad, "", nvl(tipoSangre), nvl(alergias));
        pacienteDAO.guardar(paciente);
        return paciente;
    }

    /** Versión sin dirección — mantiene compatibilidad con código existente. */
    public Paciente registrarPaciente(String nombre, String apellido, int edad,
                                      String telefono, String correo,
                                      String tipoSangre, String alergias) {
        return registrarPaciente(nombre, apellido, edad, telefono, correo, "", tipoSangre, alergias);
    }

    private static String nvl(String s) {
        return s != null ? s.trim() : "";
    }

    /**
     * Actualiza los datos de un paciente existente.
     */
    public void actualizarPaciente(Paciente paciente) {
        validar(paciente != null, "El paciente no puede ser nulo.");
        validar(paciente.getId() != 0, "El paciente no tiene un id válido para actualizar.");
        validar(paciente.getNombre() != null && !paciente.getNombre().trim().isEmpty(),
                "El nombre del paciente no puede estar vacío.");
        validar(paciente.getEdad() > 0, "La edad debe ser mayor que cero.");

        pacienteDAO.actualizar(paciente);
    }

    /**
     * Inactiva un paciente del sistema (soft delete — el registro permanece en BD).
     */
    public void eliminarPaciente(int id) {
        pacienteDAO.eliminar(id);
    }

    /** Retorna todos los pacientes inactivos (solo para vista de admin). */
    public List<Paciente> listarInactivos() {
        return pacienteDAO.listarInactivos();
    }

    /** Reactiva un paciente previamente inactivado. */
    public void reactivarPaciente(int id) {
        pacienteDAO.reactivar(id);
    }

    /**
     * Agrega una nueva entrada al historial médico de un paciente.
     */
    public void actualizarHistorial(int pacienteId, String nuevaEntrada) {
        validar(nuevaEntrada != null && !nuevaEntrada.trim().isEmpty(),
                "La entrada del historial no puede estar vacía.");

        Optional<Paciente> opt = pacienteDAO.buscarPorId(pacienteId);
        // CORRECCIÓN: condición invertida estaba incorrecta en versión anterior
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("No existe un paciente con id=" + pacienteId);
        }

        Paciente paciente = opt.get();
        String historialActual = (paciente.getHistorial() == null) ? "" : paciente.getHistorial();
        String entrada = "[" + LocalDate.now() + "] " + nuevaEntrada.trim();
        String historialNuevo = historialActual.trim().isEmpty()
                ? entrada
                : entrada + "\n" + historialActual;

        pacienteDAO.actualizarHistorial(pacienteId, historialNuevo);
    }

    // ── Operaciones de consulta ───────────────────────────────────────────

    public Optional<Paciente> obtenerPorId(int id) {
        return pacienteDAO.buscarPorId(id);
    }

    public List<Paciente> listarTodos() {
        return pacienteDAO.listarTodos();
    }

    public List<Paciente> buscarPorNombre(String nombre) {
        validar(nombre != null && !nombre.trim().isEmpty(),
                "El texto de búsqueda no puede estar vacío.");
        return pacienteDAO.buscarPorNombre(nombre.trim());
    }

    // ── Validación interna ────────────────────────────────────────────────

    private void validar(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalArgumentException(mensaje);
    }
}