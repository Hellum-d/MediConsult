package com.mediconsult.beta.service;

import com.mediconsult.beta.dao.impl.AltaMedicaDAOImpl;
import com.mediconsult.beta.dao.impl.ConsultaEspecializadaDAOImpl;
import com.mediconsult.beta.dao.impl.RemisionDAOImpl;
import com.mediconsult.beta.dao.interfaces.IAltaMedicaDAO;
import com.mediconsult.beta.dao.interfaces.IConsultaEspecializadaDAO;
import com.mediconsult.beta.dao.interfaces.IRemisionDAO;
import com.mediconsult.beta.model.AltaMedica;
import com.mediconsult.beta.model.ConsultaEspecializada;
import com.mediconsult.beta.model.Remision;

import java.util.List;
import java.util.Optional;

public class AltaMedicaService {

    private final IAltaMedicaDAO           altaDAO     = new AltaMedicaDAOImpl();
    private final IConsultaEspecializadaDAO consultaDAO = new ConsultaEspecializadaDAOImpl();
    private final IRemisionDAO             remisionDAO  = new RemisionDAOImpl();

    public AltaMedica emitirAlta(AltaMedica alta) {
        if (alta.getTipoAlta() == null || alta.getTipoAlta().isBlank())
            throw new IllegalArgumentException("El tipo de alta es obligatorio.");
        if (alta.getFechaAlta() == null)
            throw new IllegalArgumentException("La fecha de alta es obligatoria.");

        altaDAO.guardar(alta);

        consultaDAO.buscarPorRemision(alta.getRemisionId()).ifPresent(c -> {
            c.setEstado(ConsultaEspecializada.ESTADO_COMPLETADA);
            consultaDAO.actualizar(c);
        });

        remisionDAO.buscarPorId(alta.getRemisionId()).ifPresent(r -> {
            r.setEstado(Remision.ESTADO_COMPLETADA);
            remisionDAO.actualizar(r);
        });

        return alta;
    }

    public Optional<AltaMedica> buscarPorRemision(int remisionId) {
        return altaDAO.buscarPorRemision(remisionId);
    }

    public List<AltaMedica> listarPorPaciente(int pacienteId) {
        return altaDAO.listarPorPaciente(pacienteId);
    }
}
