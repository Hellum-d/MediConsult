package com.mediconsult.dao.interfaces;

import com.mediconsult.beta.model.Paciente;
import java.util.List;
import java.util.Optional;

/**
 * Contrato CRUD para la entidad Paciente.
 * No expone SQLException a capas superiores.
 */
public interface IPacienteDAO {
    void guardar(Paciente paciente);
    void actualizar(Paciente paciente);
    void eliminar(int id);
    Optional<Paciente> buscarPorId(int id);
    List<Paciente> listarTodos();
    List<Paciente> buscarPorNombre(String nombre);
}
