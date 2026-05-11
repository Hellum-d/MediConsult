package com.mediconsult.dao.interfaces;

import com.mediconsult.model.Cita;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrato CRUD para la entidad Cita.
 * Incluye verificacion de disponibilidad por fecha y medico.
 */
public interface ICitaDAO {
    void guardar(Cita cita);
    void actualizar(Cita cita);
    void eliminar(int id);
    Optional<Cita> buscarPorId(int id);
    List<Cita> listarTodas();
    List<Cita> listarPorPaciente(int pacienteId);
    boolean existeCitaEnFecha(int medicoId, LocalDate fecha);
}
