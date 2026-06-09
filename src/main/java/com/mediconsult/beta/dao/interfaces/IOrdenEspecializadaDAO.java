package com.mediconsult.beta.dao.interfaces;

import com.mediconsult.beta.model.OrdenEspecializada;
import java.util.List;

public interface IOrdenEspecializadaDAO {
    void guardar(OrdenEspecializada orden);
    List<OrdenEspecializada> listarPorConsulta(int consultaEspecializadaId);
    List<OrdenEspecializada> listarPorPaciente(int pacienteId);
    void eliminar(int id);
}
