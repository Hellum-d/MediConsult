/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mediconsult.dao.impl;

import com.mediconsult.dao.interfaces.IUsuarioDAO;
import com.mediconsult.model.Usuario;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioDAOImpl implements IUsuarioDAO {
    private List<Usuario> usuarios = new ArrayList<>();

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        for (Usuario u : usuarios)
            if (u.getUsername().equalsIgnoreCase(username)) 
                return Optional.of(u);
        return Optional.empty();
    }
}