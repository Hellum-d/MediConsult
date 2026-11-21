/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mediconsult.dao.impl;

import com.mediconsult.dao.interfaces.IPacienteDAO;
import com.mediconsult.model.Paciente;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PacienteDAOImpl implements IPacienteDAO {
    private List<Paciente> pacientes = new ArrayList<>();

    @Override
    public void guardar(Paciente paciente) { 
        pacientes.add(paciente); 
    }

    @Override
    public void actualizar(Paciente paciente) {
        for (int i = 0; i < pacientes.size(); i++)
            if (pacientes.get(i).getId() == paciente.getId()) { 
                pacientes.set(i, paciente); return; 
            }
    }

    @Override
    public void eliminar(int id) { 
        pacientes.removeIf(p -> p.getId() == id); 
    }

    @Override
    public Optional<Paciente> buscarPorId(int id) {
        for (Paciente p : pacientes)
            if (p.getId() == id) return Optional.of(p);
        return Optional.empty();
    }

    @Override
    public List<Paciente> listarTodos() { 
        return pacientes; 
    }

    @Override
    public List<Paciente> buscarPorNombre(String nombre) {
        List<Paciente> resultado = new ArrayList<>();
        for (Paciente p : pacientes)
            if (p.getNombre().equalsIgnoreCase(nombre)) resultado.add(p);
        return resultado;
    }
}