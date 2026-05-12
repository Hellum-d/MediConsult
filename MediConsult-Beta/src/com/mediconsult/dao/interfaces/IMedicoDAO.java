package com.mediconsult.dao.interfaces;

import com.mediconsult.beta.model.Medico;
import java.util.List;
import java.util.Optional;

/**
 * Contrato CRUD para la entidad Medico.
 * Incluye busqueda por especialidad y filtro de disponibles.
 */
public interface IMedicoDAO {
    void guardar(Medico medico);
    void actualizar(Medico medico);
    void eliminar(int id);
    Optional<Medico> buscarPorId(int id);
    List<Medico> listarTodos();
    List<Medico> buscarPorEspecialidad(String especialidad);
    List<Medico> listarDisponibles();
}