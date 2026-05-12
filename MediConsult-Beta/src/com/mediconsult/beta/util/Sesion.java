package com.mediconsult.beta.util;

import com.mediconsult.beta.model.Usuario;

/**
 * Gestiona la sesión del usuario autenticado en MediConsult-Beta.
 *
 * <p>Mantiene una referencia estática al {@link Usuario} que inició sesión,
 * permitiendo que cualquier vista (view) acceda al usuario actual sin necesidad
 * de pasarlo entre menús manualmente.</p>
 *
 * <p><strong>Uso típico:</strong></p>
 * <pre>{@code
 *   // Al autenticar (MenuPrincipal):
 *   Sesion.iniciar(usuario);
 *
 *   // En cualquier menú:
 *   String nombre = Sesion.getUsuario().getUsername();
 *
 *   // Al cerrar sesión:
 *   Sesion.cerrar();
 * }</pre>
 *
 * @author MediConsult Team
 * @version 0.1
 */
public class Sesion {

    /** Usuario actualmente autenticado. {@code null} si no hay sesión activa. */
    private static Usuario usuarioActual;

    /** Constructor privado: clase de utilidad, no instanciable. */
    private Sesion() {}

    /**
     * Inicia una sesión con el usuario autenticado.
     *
     * @param usuario usuario que superó la validación de credenciales
     */
    public static void iniciar(Usuario usuario) {
        usuarioActual = usuario;
    }

    /**
     * Retorna el usuario de la sesión actual.
     *
     * @return usuario autenticado, o {@code null} si no hay sesión activa
     */
    public static Usuario getUsuario() {
        return usuarioActual;
    }

    /**
     * Cierra la sesión actual, eliminando la referencia al usuario.
     */
    public static void cerrar() {
        usuarioActual = null;
    }

    /**
     * Verifica si el usuario activo tiene rol de administrador.
     *
     * @return {@code true} si el usuario tiene rol {@code ADMIN}
     */
    public static boolean isAdmin() {
        return usuarioActual != null
                && Usuario.ROL_ADMIN.equals(usuarioActual.getRol());
    }

    /**
     * Verifica si hay una sesión activa.
     *
     * @return {@code true} si hay un usuario autenticado
     */
    public static boolean isActiva() {
        return usuarioActual != null;
    }
}
