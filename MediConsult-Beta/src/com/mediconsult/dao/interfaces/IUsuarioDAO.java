package com.mediconsult.dao.interfaces;

import com.mediconsult.beta.model.Usuario;
import java.util.Optional;

/**
 * Contrato para autenticacion de usuarios.
 * Interfaz minima: solo expone lo necesario para el login.
 */
public interface IUsuarioDAO {
    Optional<Usuario> buscarPorUsername(String username);
}