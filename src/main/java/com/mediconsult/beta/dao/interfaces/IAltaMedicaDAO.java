package com.mediconsult.beta.dao.interfaces;

import com.mediconsult.beta.model.AltaMedica;
import java.util.List;
import java.util.Optional;

public interface IAltaMedicaDAO {
    void guardar(AltaMedica alta);
    Optional<AltaMedica> buscarPorRemision(int remisionId);
    List<AltaMedica> listarPorPaciente(int pacienteId);
}
