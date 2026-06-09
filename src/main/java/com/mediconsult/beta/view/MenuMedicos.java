package com.mediconsult.beta.view;

import com.mediconsult.beta.model.Medico;
import com.mediconsult.beta.service.MedicoService;
import com.mediconsult.beta.util.ConsoleUtil;
import com.mediconsult.beta.model.Usuario;
import com.mediconsult.beta.util.Sesion;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Menú de consola para la gestión de médicos en MediConsult-Beta.
 *
 * @author MediConsult Team
 * @version 0.1
 */
public class MenuMedicos {

    private final Scanner      sc;
    private final MedicoService medicoService;

    public MenuMedicos(Scanner sc) {
        this.sc            = sc;
        this.medicoService = new MedicoService();
    }

    /** Muestra el menú de médicos y gestiona las opciones en bucle. */
    public void mostrar() {
    boolean esAdmin = Sesion.getUsuario().getRol().equals(Usuario.ROL_ADMIN);

    while (true) {
        System.out.println();
        ConsoleUtil.imprimirEncabezado("GESTIÓN DE MÉDICOS");
        System.out.println("  1. Listar todos los médicos");
        System.out.println("  2. Listar médicos disponibles");
        System.out.println("  3. Buscar por especialidad");
        if (esAdmin) {
            System.out.println("  4. Registrar nuevo médico");
            System.out.println("  5. Actualizar médico");
            System.out.println("  6. Cambiar disponibilidad");
            System.out.println("  7. Eliminar médico");
        }
        System.out.println("  0. Volver");
        ConsoleUtil.imprimirSeparador();

        int opcion = ConsoleUtil.leerEntero(sc, "  Opción: ");
        System.out.println();
        switch (opcion) {
            case 1 -> listarTodos();
            case 2 -> listarDisponibles();
            case 3 -> buscarPorEspecialidad();
            case 4 -> { if (esAdmin) registrarMedico();
                        else ConsoleUtil.imprimirError("Acceso restringido al administrador."); }
            case 5 -> { if (esAdmin) actualizarMedico();
                        else ConsoleUtil.imprimirError("Acceso restringido al administrador."); }
            case 6 -> { if (esAdmin) cambiarDisponibilidad();
                        else ConsoleUtil.imprimirError("Acceso restringido al administrador."); }
            case 7 -> { if (esAdmin) eliminarMedico();
                        else ConsoleUtil.imprimirError("Acceso restringido al administrador."); }
            case 0 -> { return; }
            default -> ConsoleUtil.imprimirError("Opción no válida.");
        }
    }
}

    // ── Sub-opciones ──────────────────────────────────────────────────────

    private void listarTodos() {
        List<Medico> lista = medicoService.listarTodos();
        if (lista.isEmpty()) {
            ConsoleUtil.imprimirInfo("No hay médicos registrados.");
            return;
        }
        imprimirCabecera();
        lista.forEach(this::imprimirFila);
        ConsoleUtil.imprimirSeparador();
        ConsoleUtil.imprimirInfo("Total: " + lista.size() + " médico(s).");
    }

    private void listarDisponibles() {
        List<Medico> lista = medicoService.listarDisponibles();
        if (lista.isEmpty()) {
            ConsoleUtil.imprimirInfo("No hay médicos disponibles actualmente.");
            return;
        }
        imprimirCabecera();
        lista.forEach(this::imprimirFila);
        ConsoleUtil.imprimirSeparador();
    }

    private void buscarPorEspecialidad() {
        try {
            String esp = ConsoleUtil.leerTexto(sc, "  Especialidad: ");
            List<Medico> lista = medicoService.buscarPorEspecialidad(esp);
            if (lista.isEmpty()) {
                ConsoleUtil.imprimirInfo("No se encontraron médicos con esa especialidad.");
                return;
            }
            imprimirCabecera();
            lista.forEach(this::imprimirFila);
            ConsoleUtil.imprimirSeparador();
        } catch (IllegalArgumentException e) {
            ConsoleUtil.imprimirError(e.getMessage());
        }
    }

    private void registrarMedico() {
        try {
            String nombre = ConsoleUtil.leerTexto(sc, "  Nombre       : ");
            String esp    = ConsoleUtil.leerTexto(sc, "  Especialidad : ");
            Medico m = medicoService.registrarMedico(nombre, esp);
            ConsoleUtil.imprimirExito("Médico registrado con id=" + m.getId()
                    + " (disponible=true por defecto).");
        } catch (IllegalArgumentException e) {
            ConsoleUtil.imprimirError(e.getMessage());
        }
    }

    private void actualizarMedico() {
        int id = ConsoleUtil.leerEntero(sc, "  ID del médico a actualizar: ");
        Optional<Medico> opt = medicoService.obtenerPorId(id);
        if (opt.isEmpty()) {
            ConsoleUtil.imprimirError("No existe un médico con id=" + id);
            return;
        }
        try {
            Medico m = opt.get();
            System.out.println("  (Enter para mantener valor actual)");
            String nombre = ConsoleUtil.leerTextoOpcional(sc,
                    "  Nombre [" + m.getNombre() + "]: ");
            String esp = ConsoleUtil.leerTextoOpcional(sc,
                    "  Especialidad [" + m.getEspecialidad() + "]: ");

            if (!nombre.isBlank()) m.setNombre(nombre);
            if (!esp.isBlank())    m.setEspecialidad(esp);

            medicoService.actualizarMedico(m);
            ConsoleUtil.imprimirExito("Médico actualizado correctamente.");
        } catch (IllegalArgumentException e) {
            ConsoleUtil.imprimirError(e.getMessage());
        }
    }

    private void cambiarDisponibilidad() {
        int id = ConsoleUtil.leerEntero(sc, "  ID del médico: ");
        Optional<Medico> opt = medicoService.obtenerPorId(id);
        if (opt.isEmpty()) {
            ConsoleUtil.imprimirError("No existe un médico con id=" + id);
            return;
        }
        Medico m = opt.get();
        System.out.println("  Médico: " + m.getNombre()
                + " | Disponible actual: " + (m.isDisponible() ? "SÍ" : "NO"));
        boolean nueva = ConsoleUtil.leerConfirmacion(sc, "  ¿Activar disponibilidad?");
        try {
            medicoService.cambiarDisponibilidad(id, nueva);
            ConsoleUtil.imprimirExito("Disponibilidad actualizada a: " + (nueva ? "SÍ" : "NO"));
        } catch (IllegalArgumentException e) {
            ConsoleUtil.imprimirError(e.getMessage());
        }
    }

    private void eliminarMedico() {
        int id = ConsoleUtil.leerEntero(sc, "  ID del médico a eliminar: ");
        Optional<Medico> opt = medicoService.obtenerPorId(id);
        if (opt.isEmpty()) {
            ConsoleUtil.imprimirError("No existe un médico con id=" + id);
            return;
        }
        System.out.println("  Médico: " + opt.get().getNombre());
        boolean confirmar = ConsoleUtil.leerConfirmacion(sc, "  ¿Confirmar eliminación?");
        if (confirmar) {
            medicoService.eliminarMedico(id);
            ConsoleUtil.imprimirExito("Médico eliminado.");
        } else {
            ConsoleUtil.imprimirInfo("Operación cancelada.");
        }
    }

    // ── Formato ───────────────────────────────────────────────────────────

    private void imprimirCabecera() {
        ConsoleUtil.imprimirSeparador();
        System.out.printf("  %-4s %-22s %-18s %-10s%n",
                "ID", "NOMBRE", "ESPECIALIDAD", "DISPONIBLE");
        ConsoleUtil.imprimirSeparador();
    }

    private void imprimirFila(Medico m) {
        System.out.printf("  %-4d %-22s %-18s %-10s%n",
                m.getId(),
                truncar(m.getNombre(), 22),
                truncar(m.getEspecialidad(), 18),
                m.isDisponible() ? "SÍ" : "NO");
    }

    private String truncar(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
