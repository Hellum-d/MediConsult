/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mediconsult.dao.impl;

import com.mediconsult.dao.interfaces.IMedicoDAO;
import com.mediconsult.model.Medico;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MedicoDAOImpl implements IMedicoDAO {
    private List<Medico> medicos = new ArrayList<>();

    @Override
    public void guardar(Medico medico) {
        medicos.add(medico);
    }

    @Override
    public void actualizar(Medico medico) {
        for (int i = 0; i < medicos.size(); i++)
            if (medicos.get(i).getId() == medico.getId()) {
                medicos.set(i, medico); return;
            }
    }

    @Override
    public void eliminar(int id) {
        medicos.removeIf(m -> m.getId() == id);
    }

    @Override
    public Optional<Medico> buscarPorId(int id) {
        for (Medico m : medicos)
            if (m.getId() == id) return Optional.of(m);
        return Optional.empty();
    }

    @Override
    public List<Medico> listarTodos() {
        return medicos;
    }

    @Override
    public List<Medico> buscarPorEspecialidad(String especialidad) {
        List<Medico> resultado = new ArrayList<>();
        for (Medico m : medicos)
            if (m.getEspecialidad().equalsIgnoreCase(especialidad))
                resultado.add(m);
        return resultado;
    }

    @Override
    public List<Medico> listarDisponibles() {
        List<Medico> resultado = new ArrayList<>();
        for (Medico m : medicos)
            if (m.isDisponible()) resultado.add(m);
        return resultado;
    }
}