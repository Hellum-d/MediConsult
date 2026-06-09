package com.mediconsult.beta.service;

import com.mediconsult.beta.dao.impl.MedicoDAOImpl;
import com.mediconsult.beta.dao.interfaces.IMedicoDAO;
import com.mediconsult.beta.model.Medico;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de negocio para la gestión de médicos en MediConsult-Beta.
 *
 * <p>Aplica reglas de negocio (validaciones, transiciones de estado) antes
 * de delegar la persistencia a {@link IMedicoDAO}.</p>
 *
 * @author MediConsult Team
 * @version 0.1
 */
public class MedicoService {

    private final IMedicoDAO medicoDAO;

    /** Constructor por defecto: instancia el DAO real. */
    public MedicoService() {
        this.medicoDAO = new MedicoDAOImpl();
    }

    /** Constructor para inyección de dependencia (tests).
     * @param medicoDAO */
    public MedicoService(IMedicoDAO medicoDAO) {
        this.medicoDAO = medicoDAO;
    }

    // ── Operaciones de escritura ──────────────────────────────────────────

    /**
     * Registra un nuevo médico en el sistema.
     *
     * <p>El médico se crea con {@code disponible = true} por defecto.</p>
     *
     * @param nombre       nombre completo del médico; no puede estar vacío
     * @param especialidad especialidad médica; no puede estar vacía
     * @return médico persistido con su {@code id} asignado
     * @throws IllegalArgumentException si alguno de los campos es inválido
     */
    public Medico registrarMedico(String nombre, String especialidad) {
        validar(nombre != null && !nombre.trim().isEmpty(), "El nombre del médico no puede estar vacío.");
        validar(especialidad != null && !especialidad.trim().isEmpty(),
                "La especialidad del médico no puede estar vacía.");
       Medico medico = new Medico(nombre.trim(), "", especialidad.trim(), "");
        medicoDAO.guardar(medico);
        return medico;
    }

    /**
     * Registra un nuevo médico con todos los campos individuales.
     * Usado por el formulario admin con creación automática de usuario.
     */
    public Medico registrarMedico(String nombre, String apellido, String especialidad,
                                   String telefono, String correo) {
        validar(nombre != null && !nombre.trim().isEmpty(), "El nombre no puede estar vacío.");
        validar(apellido != null && !apellido.trim().isEmpty(), "El apellido no puede estar vacío.");
        validar(especialidad != null && !especialidad.trim().isEmpty(), "La especialidad no puede estar vacía.");

        Medico medico = new Medico(0, nombre.trim(), apellido.trim(),
                telefono != null ? telefono.trim() : "",
                "",
                correo != null ? correo.trim() : "",
                especialidad.trim(), true, Medico.ESTADO_DISPONIBLE);
        medicoDAO.guardar(medico);
        return medico;
    }

    /**
     * Actualiza los datos de un médico existente.
     *
     * @param medico entidad con datos actualizados; debe tener {@code id != 0}
     * @throws IllegalArgumentException si los datos no son válidos
     */
    public void actualizarMedico(Medico medico) {
        validar(medico != null, "El médico no puede ser nulo.");
        validar(medico.getId() != 0, "El médico no tiene un id válido para actualizar.");
        validar(medico.getNombre() != null && !medico.getNombre().trim().isEmpty(),
                "El nombre del médico no puede estar vacío.");
        validar(medico.getEspecialidad() != null && !medico.getEspecialidad().trim().isEmpty(),
                "La especialidad no puede estar vacía.");

        medicoDAO.actualizar(medico);
    }

    /**
     * Inactiva un médico del sistema (soft delete — el registro permanece en BD).
     *
     * @param id identificador del médico a inactivar
     */
    public void eliminarMedico(int id) {
        medicoDAO.eliminar(id);
    }

    /** Retorna todos los médicos inactivos (solo para vista de admin). */
    public List<Medico> listarInactivos() {
        return medicoDAO.listarInactivos();
    }

    /** Reactiva un médico previamente inactivado. */
    public void reactivarMedico(int id) {
        medicoDAO.reactivar(id);
    }

    /**
     * Activa o desactiva la disponibilidad de un médico para nuevas citas.
     *
     * @param medicoId   identificador del médico
     * @param disponible {@code true} para activar, {@code false} para desactivar
     * @throws IllegalArgumentException si el médico no existe
     */
    public void cambiarDisponibilidad(int medicoId, boolean disponible) {
        Medico medico = medicoDAO.buscarPorId(medicoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un médico con id=" + medicoId));
        medico.setDisponible(disponible);
        medicoDAO.actualizar(medico);
    }

    /**
     * Cambia el estado clínico de un médico a uno de los cinco valores válidos:
     * Disponible, En consulta, Descansando, Fuera de turno, Emergencia.
     *
     * <p>El flag {@code disponible} se sincroniza automáticamente:
     * solo "Disponible" produce {@code disponible = true}.</p>
     *
     * @param medicoId identificador del médico
     * @param estado   uno de {@link Medico#ESTADO_DISPONIBLE}, etc.
     * @throws IllegalArgumentException si el médico no existe o el estado no es válido
     */
    public void cambiarEstado(int medicoId, String estado) {
        validar(estado != null && !estado.isBlank(), "El estado no puede estar vacío.");
        Medico medico = medicoDAO.buscarPorId(medicoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un médico con id=" + medicoId));
        medico.setEstado(estado);
        medicoDAO.actualizar(medico);
    }

    // ── Operaciones de consulta ───────────────────────────────────────────

    /**
     * Busca un médico por su identificador único.
     *
     * @param id identificador del médico
     * @return {@link Optional} con el médico, o vacío si no existe
     */
    public Optional<Medico> obtenerPorId(int id) {
        return medicoDAO.buscarPorId(id);
    }

    /**
     * Retorna todos los médicos registrados.
     *
     * @return lista de médicos; vacía si no hay registros
     */
    public List<Medico> listarTodos() {
        return medicoDAO.listarTodos();
    }

    /**
     * Retorna solo los médicos disponibles para recibir nuevas citas.
     *
     * @return lista de médicos disponibles; vacía si ninguno está activo
     */
    public List<Medico> listarDisponibles() {
        return medicoDAO.listarDisponibles();
    }

    /**
     * Busca médicos por especialidad (búsqueda parcial).
     *
     * @param especialidad cadena de búsqueda
     * @return lista de médicos coincidentes; vacía si no hay resultados
     * @throws IllegalArgumentException si la cadena de búsqueda está vacía
     */
    public List<Medico> buscarPorEspecialidad(String especialidad) {
        validar(especialidad != null && !especialidad.trim().isEmpty(),
                "La especialidad de búsqueda no puede estar vacía.");
        return medicoDAO.buscarPorEspecialidad(especialidad.trim());
    }

    // ── Validación interna ────────────────────────────────────────────────

    private void validar(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalArgumentException(mensaje);
    }
}
