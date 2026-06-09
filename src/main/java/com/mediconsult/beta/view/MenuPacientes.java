package com.mediconsult.beta.view;

import com.mediconsult.beta.model.Paciente;
import com.mediconsult.beta.service.PacienteService;
import com.mediconsult.beta.util.ConsoleUtil;
import com.mediconsult.beta.model.Usuario;
import com.mediconsult.beta.util.Sesion;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Menú de consola para la gestión de pacientes en MediConsult-Beta.
 *
 * @author MediConsult Team
 * @version 0.1
 */
public class MenuPacientes {

    private final Scanner        sc;
    private final PacienteService pacienteService;

    public MenuPacientes(Scanner sc) {
        this.sc              = sc;
        this.pacienteService = new PacienteService();
    }

    /** Muestra el menú de pacientes y gestiona las opciones en bucle. */
    public void mostrar() {
    boolean esAdmin = Sesion.getUsuario().getRol().equals(Usuario.ROL_ADMIN);

    while (true) {
        System.out.println();
        ConsoleUtil.imprimirEncabezado("GESTIÓN DE PACIENTES");
        System.out.println("  1. Listar todos los pacientes");
        System.out.println("  2. Buscar paciente por nombre");
        System.out.println("  3. Ver detalle de paciente");
        if (esAdmin) {
            System.out.println("  4. Registrar nuevo paciente");
            System.out.println("  5. Actualizar paciente");
            System.out.println("  6. Eliminar paciente");
        }
        System.out.println("  0. Volver");
        ConsoleUtil.imprimirSeparador();

        int opcion = ConsoleUtil.leerEntero(sc, "  Opción: ");
        System.out.println();
        switch (opcion) {
            case 1 -> listarTodos();
            case 2 -> buscarPorNombre();
            case 3 -> verDetalle();
            case 4 -> { if (esAdmin) registrarPaciente();
                        else ConsoleUtil.imprimirError("Acceso restringido al administrador."); }
            case 5 -> { if (esAdmin) actualizarPaciente();
                        else ConsoleUtil.imprimirError("Acceso restringido al administrador."); }
            case 6 -> { if (esAdmin) eliminarPaciente();
                        else ConsoleUtil.imprimirError("Acceso restringido al administrador."); }
            case 0 -> { return; }
            default -> ConsoleUtil.imprimirError("Opción no válida.");
        }
    }
}

    // ── Sub-opciones ──────────────────────────────────────────────────────

    private void listarTodos() {
        List<Paciente> lista = pacienteService.listarTodos();
        if (lista.isEmpty()) {
            ConsoleUtil.imprimirInfo("No hay pacientes registrados.");
            return;
        }
        imprimirCabecera();
        lista.forEach(this::imprimirFila);
        ConsoleUtil.imprimirSeparador();
        ConsoleUtil.imprimirInfo("Total: " + lista.size() + " paciente(s).");
    }

    private void buscarPorNombre() {
        try {
            String nombre = ConsoleUtil.leerTexto(sc, "  Nombre a buscar: ");
            List<Paciente> lista = pacienteService.buscarPorNombre(nombre);
            if (lista.isEmpty()) {
                ConsoleUtil.imprimirInfo("No se encontraron pacientes con ese nombre.");
                return;
            }
            imprimirCabecera();
            lista.forEach(this::imprimirFila);
            ConsoleUtil.imprimirSeparador();
        } catch (IllegalArgumentException e) {
            ConsoleUtil.imprimirError(e.getMessage());
        }
    }

    private void verDetalle() {
        int id = ConsoleUtil.leerEntero(sc, "  ID del paciente: ");
        Optional<Paciente> opt = pacienteService.obtenerPorId(id);
        if (opt.isEmpty()) {
            ConsoleUtil.imprimirError("No existe un paciente con id=" + id);
            return;
        }
        Paciente p = opt.get();
        ConsoleUtil.imprimirSeparador();
        System.out.println("  ID       : " + p.getId());
        System.out.println("  Nombre   : " + p.getNombre());
        System.out.println("  Edad     : " + p.getEdad());
        System.out.println("  Teléfono : " + nvl(p.getTelefono()));
        System.out.println("  Historial:");
        String hist = p.getHistorial();
        if (hist == null || hist.isBlank()) {
            System.out.println("    (sin historial)");
        } else {
            for (String linea : hist.split("\n")) {
                System.out.println("    " + linea);
            }
        }
        ConsoleUtil.imprimirSeparador();
    }

    private void registrarPaciente() {
        try {
            String nombre   = ConsoleUtil.leerTexto(sc, "  Nombre   : ");
            int    edad     = ConsoleUtil.leerEntero(sc, "  Edad     : ");
            String telefono = ConsoleUtil.leerTextoOpcional(sc, "  Teléfono (Enter para omitir): ");
            Paciente p = pacienteService.registrarPaciente(nombre, edad, telefono);
            ConsoleUtil.imprimirExito("Paciente registrado con id=" + p.getId());
        } catch (IllegalArgumentException e) {
            ConsoleUtil.imprimirError(e.getMessage());
        }
    }

    private void actualizarPaciente() {
        int id = ConsoleUtil.leerEntero(sc, "  ID del paciente a actualizar: ");
        Optional<Paciente> opt = pacienteService.obtenerPorId(id);
        if (opt.isEmpty()) {
            ConsoleUtil.imprimirError("No existe un paciente con id=" + id);
            return;
        }
        try {
            Paciente p = opt.get();
            System.out.println("  (Enter para mantener valor actual)");
            String nombre = ConsoleUtil.leerTextoOpcional(sc,
                    "  Nombre [" + p.getNombre() + "]: ");
            String edadStr = ConsoleUtil.leerTextoOpcional(sc,
                    "  Edad   [" + p.getEdad() + "]: ");
            String telefono = ConsoleUtil.leerTextoOpcional(sc,
                    "  Teléfono [" + nvl(p.getTelefono()) + "]: ");

            if (!nombre.isBlank())   p.setNombre(nombre);
            if (!edadStr.isBlank())  p.setEdad(Integer.parseInt(edadStr));
            if (!telefono.isBlank()) p.setTelefono(telefono);

            pacienteService.actualizarPaciente(p);
            ConsoleUtil.imprimirExito("Paciente actualizado correctamente.");
        } catch (IllegalArgumentException e) {
            ConsoleUtil.imprimirError("Datos inválidos: " + e.getMessage());
        }
    }

    private void eliminarPaciente() {
        int id = ConsoleUtil.leerEntero(sc, "  ID del paciente a eliminar: ");
        Optional<Paciente> opt = pacienteService.obtenerPorId(id);
        if (opt.isEmpty()) {
            ConsoleUtil.imprimirError("No existe un paciente con id=" + id);
            return;
        }
        System.out.println("  Paciente: " + opt.get().getNombre());
        boolean confirmar = ConsoleUtil.leerConfirmacion(sc, "  ¿Confirmar eliminación?");
        if (confirmar) {
            pacienteService.eliminarPaciente(id);
            ConsoleUtil.imprimirExito("Paciente eliminado.");
        } else {
            ConsoleUtil.imprimirInfo("Operación cancelada.");
        }
    }

    // ── Helpers de formato ────────────────────────────────────────────────

    private void imprimirCabecera() {
        ConsoleUtil.imprimirSeparador();
        System.out.printf("  %-4s %-25s %-5s %-15s%n", "ID", "NOMBRE", "EDAD", "TELÉFONO");
        ConsoleUtil.imprimirSeparador();
    }

    private void imprimirFila(Paciente p) {
        System.out.printf("  %-4d %-25s %-5d %-15s%n",
                p.getId(), truncar(p.getNombre(), 25),
                p.getEdad(), truncar(nvl(p.getTelefono()), 15));
    }

    private String nvl(String s)               { return (s == null || s.isBlank()) ? "-" : s; }
    private String truncar(String s, int max)  { return s.length() > max ? s.substring(0, max - 1) + "…" : s; }
}
