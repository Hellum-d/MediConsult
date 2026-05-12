package com.mediconsult.beta.dao.interfaces;

import com.mediconsult.beta.model.Cita;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad Cita.
 *
 * Define las operaciones básicas de persistencia y algunas consultas
 * necesarias para el flujo de agendamiento (por paciente, médico y
 * validación de disponibilidad).
 *
 * Regla de arquitectura: solo la capa service debe usar este DAO.
 */
public interface ICitaDAO {

    /**
     * Guarda una nueva cita en el CSV.
     *
     * El id se genera automáticamente (max(id) + 1) y se asigna al objeto.
     * @param cita
     */
    void guardar(Cita cita);

    /**
     * Actualiza una cita existente.
     *
     * Se usa principalmente para cambiar fecha o estado de la cita.
     * @param cita
     */
    void actualizar(Cita cita);

    /**
     * Elimina una cita por su id.
     *
     * Nota: en la práctica se prefiere cancelar la cita antes que borrarla
     * para no perder historial.
     * @param id
     */
    void eliminar(int id);

    /**
     * Busca una cita por su id.
     * @param id
     * @return 
     */
    Optional<Cita> buscarPorId(int id);

    /**
     * Retorna todas las citas registradas.
     * @return 
     */
    List<Cita> listarTodos();

    /**
     * Lista las citas de un paciente, ordenadas de la más reciente a la más antigua.
     * @param pacienteId
     * @return 
     */
    List<Cita> listarPorPaciente(int pacienteId);

    /**
     * Lista las citas de un médico en orden cronológico.
     * @param medicoId
     * @return 
     */
    List<Cita> listarPorMedico(int medicoId);

    /**
     * Verifica si un médico ya tiene una cita activa en una fecha específica.
     *
     * Se usa antes de agendar para evitar choques de disponibilidad.
     * @param medicoId
     * @param fecha
     * @return 
     */
    boolean existeCitaEnFecha(int medicoId, LocalDate fecha);
}