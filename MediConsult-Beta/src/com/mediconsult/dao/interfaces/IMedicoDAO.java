package com.mediconsult.beta.dao.interfaces;

import com.mediconsult.beta.model.Medico;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para Médico.
 *
 * Expone operaciones CRUD y consultas necesarias para gestión de médicos,
 * incluyendo disponibilidad y búsqueda por especialidad.
 *
 * Regla de arquitectura: solo la capa service debe usar este DAO.
 */
public interface IMedicoDAO {

    /**
     * Guarda un médico en el CSV.
     *
     * El id se genera automáticamente (max(id) + 1).
     * @param medico
     */
    void guardar(Medico medico);

    /**
     * Actualiza un médico existente.
     * @param medico
     */
    void actualizar(Medico medico);

    /**
     * Elimina un médico por id.
     *
     * Nota: el service debe validar dependencias (citas) antes de borrar.
     * @param id
     */
    void eliminar(int id);

    /**
     * Busca un médico por id.
     * @param id
     * @return 
     */
    Optional<Medico> buscarPorId(int id);

    /**
     * Lista todos los médicos registrados.
     * @return 
     */
    List<Medico> listarTodos();

    /**
     * Retorna solo los médicos disponibles para nuevas citas.
     * @return 
     */
    List<Medico> listarDisponibles();

    /**
     * Busca médicos por especialidad (búsqueda parcial, no sensible a mayúsculas).
     * @param especialidad
     * @return 
     */
    List<Medico> buscarPorEspecialidad(String especialidad);
}