package com.mediconsult.beta.dao.interfaces;

import com.mediconsult.beta.model.Diagnostico;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para Diagnóstico.
 *
 * Maneja la persistencia de diagnósticos generados por IA y consultas
 * relacionadas con citas y pacientes.
 *
 * Regla de arquitectura: solo la capa service debe usar este DAO.
 */
public interface IDiagnosticoDAO {

    /**
     * Guarda un diagnóstico en el CSV.
     *
     * El id se genera automáticamente (max(id) + 1).
     * El resultado de la IA puede venir vacío inicialmente.
     * @param diagnostico
     */
    void guardar(Diagnostico diagnostico);

    /**
     * Actualiza un diagnóstico existente.
     *
     * Se usa principalmente para guardar el resultado de la IA o corregir síntomas.
     * @param diagnostico
     */
    void actualizar(Diagnostico diagnostico);

    /**
     * Busca un diagnóstico por su id.
     * @param id
     * @return 
     */
    Optional<Diagnostico> buscarPorId(int id);

    /**
     * Busca el diagnóstico asociado a una cita.
     *
     * La relación es uno a uno, por eso se usa Optional.
     * @param citaId
     * @return 
     */
    Optional<Diagnostico> buscarPorCita(int citaId);

    /**
     * Lista todos los diagnósticos de un paciente.
     *
     * Internamente requiere cruzar citas con diagnósticos.
     * @param pacienteId
     * @return 
     */
    List<Diagnostico> listarPorPaciente(int pacienteId);
}