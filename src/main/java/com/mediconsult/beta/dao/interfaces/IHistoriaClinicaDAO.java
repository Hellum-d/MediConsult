package com.mediconsult.beta.dao.interfaces;

import com.mediconsult.beta.model.HistoriaClinica;

import java.util.List;
import java.util.Optional;

public interface IHistoriaClinicaDAO {

    void guardar(HistoriaClinica h);
    void actualizar(HistoriaClinica h);
    Optional<HistoriaClinica> buscarPorCita(int citaId);
    List<HistoriaClinica> listarPorPaciente(int pacienteId);
    List<HistoriaClinica> listarPorMedico(int medicoId);
}
