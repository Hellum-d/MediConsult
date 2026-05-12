package com.mediconsult.beta.dao.interfaces;

import com.mediconsult.beta.model.Paciente;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para Paciente.
 *
 * Expone operaciones CRUD y consultas básicas sobre pacientes,
 * incluyendo búsqueda por nombre y actualización de historial médico.
 *
 * Regla de arquitectura: solo la capa service debe usar este DAO.
 */
public interface IPacienteDAO {

    /**
     * Guarda un paciente en el CSV.
     *
     * El id se genera automáticamente (max(id) + 1).
     * @param paciente
     */
    void guardar(Paciente paciente);

    /**
     * Actualiza un paciente existente.
     * @param paciente
     */
    void actualizar(Paciente paciente);

    /**
     * Elimina un paciente por id.
     *
     * El service debe validar dependencias antes de borrar.
     * @param id
     */
    void eliminar(int id);

    /**
     * Busca un paciente por id.
     * @param id
     * @return 
     */
    Optional<Paciente> buscarPorId(int id);

    /**
     * Lista todos los pacientes registrados.
     * @return 
     */
    List<Paciente> listarTodos();

    /**
     * Busca pacientes por nombre (búsqueda parcial, no sensible a mayúsculas).
     * @param nombre
     * @return 
     */
    List<Paciente> buscarPorNombre(String nombre);

    /**
     * Actualiza únicamente el historial médico de un paciente.
     *
     * Usado principalmente al registrar diagnósticos.
     * @param id
     * @param historial
     */
    void actualizarHistorial(int id, String historial);
}