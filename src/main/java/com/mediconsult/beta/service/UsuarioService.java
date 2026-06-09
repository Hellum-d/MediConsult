package com.mediconsult.beta.service;

import com.mediconsult.beta.dao.impl.UsuarioDAOImpl;
import com.mediconsult.beta.dao.interfaces.IUsuarioDAO;
import com.mediconsult.beta.model.Usuario;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servicio de autenticación, registro y recuperación de contraseña para CLINEX.
 *
 * @author MediConsult Team
 * @version 0.3 — registro de pacientes/médicos, recuperación con código temporal
 */
public class UsuarioService {

    private final IUsuarioDAO usuarioDAO;

    public UsuarioService() {
        this.usuarioDAO = new UsuarioDAOImpl();
    }

    public UsuarioService(IUsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    // ── Autenticación ──────────────────────────────────────────────────────

    public Optional<Usuario> autenticar(String username, String password) {
        System.out.println("Hash generado: " + Usuario.hashPassword(password));

        validar(username != null && !username.trim().isEmpty(), "El usuario no puede estar vacío.");
        validar(password != null && !password.trim().isEmpty(), "La contraseña no puede estar vacía.");

        Optional<Usuario> opt = usuarioDAO.buscarPorUsername(username.trim());
        if (opt.isEmpty()) return Optional.empty();

        Usuario usuario = opt.get();
        if (Usuario.hashPassword(password).equals(usuario.getPassword())) {
            return Optional.of(usuario);
        }
        return Optional.empty();
    }

    // ── Registro ───────────────────────────────────────────────────────────

    /**
     * Registra un usuario vinculado a un paciente existente.
     *
     * @param username    nombre de usuario único
     * @param password    contraseña en texto plano (se hashea internamente)
     * @param pacienteId  FK al registro de paciente
     * @return usuario creado con id asignado
     * @throws IllegalArgumentException si el username ya existe o los datos son inválidos
     */
    public Usuario registrarUsuarioPaciente(String username, String password, int pacienteId) {
        validar(username != null && !username.trim().isEmpty(), "El nombre de usuario es obligatorio.");
        validar(password != null && password.length() >= 6, "La contraseña debe tener al menos 6 caracteres.");
        validar(!usuarioDAO.existeUsername(username.trim()),
                "El nombre de usuario «" + username.trim() + "» ya está en uso.");

        Usuario u = new Usuario(username.trim(), password, Usuario.ROL_PACIENTE);
        u.setPacienteId(pacienteId);
        usuarioDAO.guardar(u);
        return u;
    }

    /**
     * Registra un usuario médico con el rol indicado.
     * Si el username ya existe, agrega un sufijo numérico automáticamente.
     *
     * @param usernameBase nombre de usuario base (puede modificarse si hay colisión)
     * @param password     contraseña en texto plano
     * @param rol          uno de ROL_MEDICO_GENERAL o ROL_MEDICO_ESPECIALISTA
     * @return usuario creado con id asignado
     */
    public Usuario registrarUsuarioMedico(String usernameBase, String password, String rol) {
        validar(usernameBase != null && !usernameBase.trim().isEmpty(), "El username es obligatorio.");
        validar(password != null && !password.isBlank(), "La contraseña es obligatoria.");

        String username = usernameBase.trim().toLowerCase();
        int sufijo = 1;
        while (usuarioDAO.existeUsername(username)) {
            username = usernameBase.trim().toLowerCase() + sufijo++;
        }

        Usuario u = new Usuario(username, password, rol);
        usuarioDAO.guardar(u);
        return u;
    }

    /**
     * Registra un usuario médico vinculado a un registro de médico existente.
     *
     * @param usernameBase base del nombre de usuario (se resuelven colisiones)
     * @param password     contraseña en texto plano
     * @param rol          ROL_MEDICO_GENERAL o ROL_MEDICO_ESPECIALISTA
     * @param medicoId     FK al registro de médico en la tabla medico
     * @return usuario creado con id asignado
     */
    public Usuario registrarUsuarioMedico(String usernameBase, String password,
                                           String rol, int medicoId) {
        Usuario u = registrarUsuarioMedico(usernameBase, password, rol);
        if (medicoId > 0) {
            u.setMedicoId(medicoId);
            usuarioDAO.actualizarMedicoId(u.getId(), medicoId);
        }
        return u;
    }

    /** @return true si el username ya está registrado */
    public boolean existeUsername(String username) {
        return username != null && usuarioDAO.existeUsername(username.trim());
    }

    // ── Recuperación de contraseña ─────────────────────────────────────────

    /**
     * Genera un código de recuperación de 6 dígitos, lo almacena en la BD
     * con una expiración de 30 minutos y lo devuelve para mostrarlo al usuario.
     *
     * @param username nombre de usuario que solicita la recuperación
     * @return código de 6 dígitos como String
     * @throws IllegalArgumentException si el usuario no existe
     */
    public String solicitarResetToken(String username) {
        Optional<Usuario> opt = usuarioDAO.buscarPorUsername(username.trim());
        validar(opt.isPresent(), "No existe ninguna cuenta con el usuario «" + username + "».");

        String token = String.valueOf(100000 + new SecureRandom().nextInt(900000));
        LocalDateTime expires = LocalDateTime.now().plusMinutes(30);
        usuarioDAO.guardarResetToken(opt.get().getId(), token, expires);
        return token;
    }

    /**
     * Valida el código temporal y restablece la contraseña si es correcto.
     *
     * @param token       código de 6 dígitos introducido por el usuario
     * @param newPassword nueva contraseña en texto plano
     * @throws IllegalArgumentException si el token es inválido o expiró
     */
    public void resetearConToken(String token, String newPassword) {
        validar(token != null && !token.isBlank(), "El código no puede estar vacío.");
        validar(newPassword != null && newPassword.length() >= 6,
                "La nueva contraseña debe tener al menos 6 caracteres.");

        Optional<Usuario> opt = usuarioDAO.buscarPorResetToken(token.trim());
        validar(opt.isPresent(), "El código es incorrecto o ya expiró. Solicita uno nuevo.");

        Usuario u = opt.get();
        usuarioDAO.actualizarPasswordHash(u.getId(), Usuario.hashPassword(newPassword));
        usuarioDAO.limpiarResetToken(u.getId());
    }

    // ── Consultas ──────────────────────────────────────────────────────────

    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioDAO.buscarPorUsername(username.trim());
    }

    // ── Validación interna ─────────────────────────────────────────────────

    private void validar(boolean cond, String mensaje) {
        if (!cond) throw new IllegalArgumentException(mensaje);
    }
}
