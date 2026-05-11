package com.mediconsult.dao.interfaces;

import com.mediconsult.model.Diagnostico;
import java.util.List;
import java.util.Optional;

/**
 * Contrato para la entidad Diagnostico.
 * Incluye listarPorPaciente con JOIN a citas.
 */
public interface IDiagnosticoDAO {
    void guardar(Diagnostico diagnostico);
    void actualizar(Diagnostico diagnostico);
    Optional<Diagnostico> buscarPorCita(int citaId);
    List<Diagnostico> listarPorPaciente(int pacienteId);
}