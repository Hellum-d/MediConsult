/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mediconsult.dao.impl;

import com.mediconsult.dao.interfaces.IDiagnosticoDAO;
import com.mediconsult.model.Diagnostico;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DiagnosticoDAOImpl implements IDiagnosticoDAO {
    private List<Diagnostico> diagnosticos = new ArrayList<>();

    @Override
    public void guardar(Diagnostico diagnostico) {
        diagnosticos.add(diagnostico);
    }

    @Override
    public void actualizar(Diagnostico diagnostico) {
        for (int i = 0; i < diagnosticos.size(); i++)
            if (diagnosticos.get(i).getId() == diagnostico.getId()) {
                diagnosticos.set(i, diagnostico); return;
            }
    }

    @Override
    public Optional<Diagnostico> buscarPorCita(int citaId) {
        for (Diagnostico d : diagnosticos)
            if (d.getCitaId() == citaId) return Optional.of(d);
        return Optional.empty();
    }

    @Override
    public List<Diagnostico> listarPorPaciente(int pacienteId) {
        List<Diagnostico> resultado = new ArrayList<>();
        for (Diagnostico d : diagnosticos)
            if (d.getCitaId() == pacienteId) resultado.add(d);
        return resultado;
    }
}