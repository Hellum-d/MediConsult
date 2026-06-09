package com.mediconsult.beta.dao.interfaces;

import com.mediconsult.beta.model.Usuario;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad {@link Usuario}.
 *
 * <p>Gestiona autenticación, registro, recuperación de contraseña
 * y consultas por credenciales o token.</p>
 *
 * @author MediConsult Team
 * @version 0.3 — registro, recuperación y roles expandidos
 */
public interface IUsuarioDAO {

    /** Busca un usuario por su nombre de usuario. */
    Optional<Usuario> buscarPorUsername(String username);

    /** Persiste un nuevo usuario. Asigna el id generado al objeto. */
    void guardar(Usuario usuario);

    /** Actualiza el hash de contraseña de un usuario existente. */
    void actualizarPasswordHash(int userId, String nuevoHash);

    /**
     * Almacena un token de recuperación con su fecha de expiración.
     * Si ya existía un token previo, lo sobreescribe.
     */
    void guardarResetToken(int userId, String token, LocalDateTime expires);

    /**
     * Busca el usuario propietario de un token de recuperación que
     * aún no haya expirado.
     *
     * @param token código de 6 dígitos generado por el sistema
     * @return Optional con el usuario si el token es válido y vigente
     */
    Optional<Usuario> buscarPorResetToken(String token);

    /** Elimina el token de recuperación del usuario (tras usarlo). */
    void limpiarResetToken(int userId);

    /** @return true si ya existe un usuario con ese username */
    boolean existeUsername(String username);

    /** Busca el usuario vinculado a un paciente específico. */
    Optional<Usuario> buscarPorPacienteId(int pacienteId);

    /** Vincula un usuario a un médico (actualiza medico_id). */
    void actualizarMedicoId(int userId, int medicoId);

    /** Marca un usuario como inactivo (no puede iniciar sesión). */
    void inactivar(int id);

    /** Reactiva un usuario previamente inactivado. */
    void reactivar(int id);
}
