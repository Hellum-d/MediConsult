package com.mediconsult.beta.service;

import com.mediconsult.beta.dao.impl.UsuarioDAOImpl;
import com.mediconsult.beta.dao.interfaces.IUsuarioDAO;
import com.mediconsult.beta.model.Usuario;

import java.util.Optional;

/**
 * Servicio de login.
 *
 * <p>Valida credenciales contra usuarios en CSV usando hash SHA-256.</p>
 */
public class UsuarioService {

    private final IUsuarioDAO usuarioDAO;

    public UsuarioService() {
        this.usuarioDAO = new UsuarioDAOImpl();
    }

    public UsuarioService(IUsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    /**
     * Autentica usuario por username y password.
     */
    public Optional<Usuario> autenticar(String username, String passwordPlainText) {
        validar(username != null && !username.isBlank(), "Usuario vacío");
        validar(passwordPlainText != null && !passwordPlainText.isBlank(), "Contraseña vacía");

        Optional<Usuario> opt = usuarioDAO.buscarPorUsername(username.trim());
        if (opt.isEmpty()) return Optional.empty();

        Usuario usuario = opt.get();
        String hash = Usuario.hashPassword(passwordPlainText);

        return hash.equals(usuario.getPassword())
                ? Optional.of(usuario)
                : Optional.empty();
    }

    private void validar(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalArgumentException(mensaje);
    }
}