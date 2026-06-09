package com.mediconsult.beta.dao.interfaces;

import com.mediconsult.beta.model.OrdenMedica;
import java.util.List;

public interface IOrdenMedicaDAO {
    void guardar(OrdenMedica orden);
    List<OrdenMedica> listarPorCita(int citaId);
    List<OrdenMedica> listarPorPaciente(int pacienteId);
    void eliminar(int id);
}
