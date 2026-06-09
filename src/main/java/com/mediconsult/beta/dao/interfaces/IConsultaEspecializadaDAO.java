package com.mediconsult.beta.dao.interfaces;

import com.mediconsult.beta.model.ConsultaEspecializada;
import java.util.List;
import java.util.Optional;

public interface IConsultaEspecializadaDAO {
    void guardar(ConsultaEspecializada c);
    void actualizar(ConsultaEspecializada c);
    Optional<ConsultaEspecializada> buscarPorRemision(int remisionId);
    List<ConsultaEspecializada> listarPorPaciente(int pacienteId);
}
