package com.mediconsult.beta.dao.interfaces;

import com.mediconsult.beta.model.Usuario;

import java.util.Optional;

/**
 * Contrato de acceso a datos para Usuario.
 *
 * Se usa únicamente para autenticación en consola.
 *
 * Regla de arquitectura: solo UsuarioService debe usar este DAO.
 */
public interface IUsuarioDAO {

    /**
     * Busca un usuario por su nombre de usuario.
     *
     * Usado en el proceso de login.
     * @param username
     * @return 
     */
    Optional<Usuario> buscarPorUsername(String username);
}