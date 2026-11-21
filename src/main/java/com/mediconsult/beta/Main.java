package com.mediconsult.beta;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import com.mediconsult.beta.model.Usuario;
import com.mediconsult.beta.util.ArchivoUtil;
import com.mediconsult.beta.util.ConsoleUtil;
import com.mediconsult.beta.util.Sesion;
import com.mediconsult.beta.view.MenuPrincipal;

/**
 * Punto de entrada del modo consola de MediConsult-Beta (heredado de la v0.1).
 *
 * <p>La interfaz principal del sistema es JavaFX ({@link AppFX},
 * {@code mvn javafx:run}); este modo se conserva como referencia.</p>
 *
 * <p>Responsabilidades al arrancar:</p>
 * <ol>
 *   <li>Garantizar que {@code data/} y los CSV existen
 *       ({@link ArchivoUtil} lo hace en su bloque estático).</li>
 *   <li>Sembrar usuarios por defecto si {@code usuarios.csv} está vacío.</li>
 *   <li>Abrir el bucle de sesión: login → menú principal → logout → login…</li>
 * </ol>
 *
 * <p>El {@link Scanner} se crea una sola vez aquí y se pasa por constructor
 * a todos los menús, para evitar abrir múltiples streams sobre {@code System.in}.</p>
 *
 * @author MediConsult Team
 * @version 0.2
 */
public class Main {

    /** Archivo CSV de usuarios. */
    private static final String ARCHIVO_USUARIOS = "usuarios.csv";

    public static void main(String[] args) {

        // ── 1. Semilla de datos ──────────────────────────────────────────
        sembrarUsuarios();

        // ── 2. Scanner único para toda la aplicación ─────────────────────
        Scanner sc = new Scanner(System.in);

        // ── 3. Splash ────────────────────────────────────────────────────
        System.out.println();
        ConsoleUtil.imprimirEncabezado("MEDICONSULT BETA  v0.2  (modo consola)");
        System.out.println("  Sistema de Gestión Hospitalaria");
        System.out.println("  Persistencia: MySQL  |  IA: OpenAI GPT-4.1");
        ConsoleUtil.imprimirSeparador();

        // ── 4. Bucle de sesión ───────────────────────────────────────────
        MenuPrincipal menuPrincipal = new MenuPrincipal(sc);

        while (true) {
            Optional<com.mediconsult.beta.model.Usuario> optUsuario =
                    menuPrincipal.mostrarLogin();

            if (!optUsuario.isPresent()) {
                // Ofrecer salir tras credenciales incorrectas
                boolean salir = ConsoleUtil.leerConfirmacion(sc,
                        "  ¿Desea salir de la aplicación?");
                if (salir) break;
                continue; 
            }

            Sesion.iniciar(optUsuario.get());
            menuPrincipal.mostrarMenuPrincipal();
            Sesion.cerrar();

            // Tras cerrar sesión, preguntar si quiere volver al login o salir
            System.out.println();
            boolean salir = ConsoleUtil.leerConfirmacion(sc,
                    "  ¿Desea salir de la aplicación?");
            if (salir) break;
        }

        // ── 5. Cierre ordenado ───────────────────────────────────────────
        // NOTA: no cerramos el Scanner para no cerrar System.in.
        System.out.println();
        ConsoleUtil.imprimirSeparador();
        System.out.println("  Gracias por usar MediConsult Beta. ¡Hasta pronto!");
        ConsoleUtil.imprimirSeparador();
    }

    // ── Semilla de usuarios por defecto ───────────────────────────────────

    /**
     * Si {@code usuarios.csv} está vacío, inserta dos usuarios por defecto:
     * <ul>
     *   <li>{@code admin} / {@code admin123} — rol ADMIN</li>
     *   <li>{@code medico} / {@code medico123} — rol MEDICO</li>
     * </ul>
     *
     * <p>Los hashes se calculan con {@link Usuario#hashPassword(String)}
     * para garantizar consistencia con el resto del sistema.</p>
     */
    private static void sembrarUsuarios() {
        System.out.println("Hash medico123: " + Usuario.hashPassword("medico123"));
        List<String> lineas = ArchivoUtil.leerLineas(ARCHIVO_USUARIOS);
        if (!lineas.isEmpty()) return; // ya tiene datos

        System.out.println("[Main] Creando usuarios por defecto...");

        // usuario admin
        String hashAdmin = Usuario.hashPassword("admin123");
        ArchivoUtil.agregarLinea(ARCHIVO_USUARIOS,
                "1" + ArchivoUtil.SEPARADOR
                + "admin" + ArchivoUtil.SEPARADOR
                + hashAdmin + ArchivoUtil.SEPARADOR
                + Usuario.ROL_ADMIN);

        // usuario medico
        String hashMedico = Usuario.hashPassword("medico123");
        ArchivoUtil.agregarLinea(ARCHIVO_USUARIOS,
                "2" + ArchivoUtil.SEPARADOR
                + "medico" + ArchivoUtil.SEPARADOR
                + hashMedico + ArchivoUtil.SEPARADOR
                + Usuario.ROL_MEDICO);

        System.out.println("[Main] Usuarios creados:");
        System.out.println("       admin  / admin123  (ADMIN)");
        System.out.println("       medico / medico123 (MEDICO)");
        ConsoleUtil.imprimirSeparador();
    } 
}
