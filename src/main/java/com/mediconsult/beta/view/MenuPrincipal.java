package com.mediconsult.beta.view;

import com.mediconsult.beta.model.Usuario;
import com.mediconsult.beta.service.UsuarioService;
import com.mediconsult.beta.util.ConsoleUtil;
import com.mediconsult.beta.util.Sesion;
import java.util.Optional;
import java.util.Scanner;

public class MenuPrincipal {

    private final Scanner        sc;
    private final UsuarioService usuarioService;

    public MenuPrincipal(Scanner sc) {
        this.sc             = sc;
        this.usuarioService = new UsuarioService();
    }
    public Optional<Usuario> mostrarLogin() {
        ConsoleUtil.imprimirEncabezado("INICIO DE SESIÓN");
        System.out.println();
        try {
            String username = ConsoleUtil.leerTexto(sc, "  Usuario   : ");
            String password = ConsoleUtil.leerTexto(sc, "  Contraseña: ");
            Optional<Usuario> opt = usuarioService.autenticar(username, password);
            if (opt.isEmpty()) {
                ConsoleUtil.imprimirError("Credenciales incorrectas. Intente de nuevo.");
            }
            return opt;
        } catch (IllegalArgumentException e) {
            ConsoleUtil.imprimirError(e.getMessage());
            return Optional.empty();
        }
    }
    public void mostrarMenuPrincipal() {
    while (true) {
        String rol = Sesion.getUsuario().getRol();
        boolean esAdmin = rol.equals(Usuario.ROL_ADMIN);

        System.out.println();
        ConsoleUtil.imprimirEncabezado("MEDICONSULT BETA v0.1");
        System.out.println("  Bienvenido: " + Sesion.getUsuario().getUsername()
                + "  [" + rol + "]");
        ConsoleUtil.imprimirSeparador();

        if (esAdmin) {
            System.out.println("  1. Gestión de Pacientes");
            System.out.println("  2. Gestión de Médicos");
        }
        System.out.println("  3. Gestión de Citas");
        System.out.println("  4. Diagnóstico IA");
        System.out.println("  0. Cerrar sesión");
        ConsoleUtil.imprimirSeparador();

        int opcion = ConsoleUtil.leerEntero(sc, "  Opción: ");
        switch (opcion) {
            case 1 -> { if (esAdmin) new MenuPacientes(sc).mostrar();
                        else ConsoleUtil.imprimirError("Acceso restringido al administrador."); }
            case 2 -> { if (esAdmin) new MenuMedicos(sc).mostrar();
                        else ConsoleUtil.imprimirError("Acceso restringido al administrador."); }
            case 3 -> new MenuCitas(sc).mostrar();
            case 4 -> new MenuDiagnostico(sc).mostrar();
            case 0 -> { return; }
            default -> ConsoleUtil.imprimirError("Opción no válida.");
        }
    }
}
}
