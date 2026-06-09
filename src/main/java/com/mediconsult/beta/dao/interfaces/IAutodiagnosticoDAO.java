package com.mediconsult.beta.dao.interfaces;

import com.mediconsult.beta.model.Autodiagnostico;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para {@link Autodiagnostico}.
 */
public interface IAutodiagnosticoDAO {

    /** Persiste un nuevo autodiagnóstico. Asigna el id generado al objeto. */
    void guardar(Autodiagnostico a);

    /** Actualiza el resultado de la IA en un autodiagnóstico ya persistido. */
    void actualizarResultado(int id, String resultadoIa);

    /** Retorna todos los autodiagnósticos de un paciente, más reciente primero. */
    List<Autodiagnostico> listarPorPaciente(int pacienteId);

    /** Busca un autodiagnóstico por id. */
    Optional<Autodiagnostico> buscarPorId(int id);
}
