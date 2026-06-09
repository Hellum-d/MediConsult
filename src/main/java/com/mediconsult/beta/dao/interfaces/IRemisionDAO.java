package com.mediconsult.beta.dao.interfaces;

import com.mediconsult.beta.model.Remision;
import java.util.List;
import java.util.Optional;

public interface IRemisionDAO {
    void guardar(Remision remision);
    Optional<Remision> buscarPorId(int id);
    List<Remision> listarPorCita(int citaId);
    List<Remision> listarPorPaciente(int pacienteId);
    List<Remision> listarPorEspecialidad(String especialidad);
    List<Remision> listarPorEspecialista(int medicoEspecialistaId);
    List<Remision> listarTodas();
    void actualizar(Remision remision);
    void eliminar(int id);
}
