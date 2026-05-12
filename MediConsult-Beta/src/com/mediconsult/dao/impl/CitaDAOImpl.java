/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mediconsult.dao.impl;

import com.mediconsult.dao.interfaces.ICitaDAO;
import com.mediconsult.model.Cita;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CitaDAOImpl implements ICitaDAO {
    private List<Cita> citas = new ArrayList<>();

    @Override
    public void guardar(Cita cita) {
        citas.add(cita);
    }

    @Override
    public void actualizar(Cita cita) {
        for (int i = 0; i < citas.size(); i++)
            if (citas.get(i).getId() == cita.getId()) {
                citas.set(i, cita); return;
            }
    }

    @Override
    public void eliminar(int id) {
        citas.removeIf(c -> c.getId() == id);
    }

    @Override
    public Optional<Cita> buscarPorId(int id) {
        for (Cita c : citas)
            if (c.getId() == id) return Optional.of(c);
        return Optional.empty();
    }

    @Override
    public List<Cita> listarTodas() {
        return citas;
    }

    @Override
    public List<Cita> listarPorPaciente(int pacienteId) {
        List<Cita> resultado = new ArrayList<>();
        for (Cita c : citas)
            if (c.getPacienteId() == pacienteId) resultado.add(c);
        return resultado;
    }

    @Override
    public boolean existeCitaEnFecha(int medicoId, LocalDate fecha) {
        for (Cita c : citas)
            if (c.getMedicoId() == medicoId && c.getFecha().equals(fecha))
                return true;
        return false;
    }
}