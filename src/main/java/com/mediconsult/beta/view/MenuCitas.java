package com.mediconsult.beta.view;

import com.mediconsult.beta.model.Cita;
import com.mediconsult.beta.service.CitaService;
import com.mediconsult.beta.service.MedicoService;
import com.mediconsult.beta.service.PacienteService;
import com.mediconsult.beta.util.ConsoleUtil;
import com.mediconsult.beta.model.Usuario;
import com.mediconsult.beta.util.Sesion;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Menú de consola para la gestión de citas en MediConsult-Beta.
 *
 * @author MediConsult Team
 * @version 0.1
 */
public class MenuCitas {

    private final Scanner        sc;
    private final CitaService    citaService;
    private final MedicoService  medicoService;
    private final PacienteService pacienteService;

    public MenuCitas(Scanner sc) {
        this.sc              = sc;
        this.citaService     = new CitaService();
        this.medicoService   = new MedicoService();
        this.pacienteService = new PacienteService();
    }

    /** Muestra el menú de citas en bucle. */
public void mostrar() {
    boolean esAdmin = Sesion.getUsuario().getRol().equals(Usuario.ROL_ADMIN);

    while (true) {
        System.out.println();
        ConsoleUtil.imprimirEncabezado("GESTIÓN DE CITAS");

        if (esAdmin) {
            System.out.println("  1. Listar todas las citas");
            System.out.println("  2. Ver citas de un paciente");
            System.out.println("  3. Ver agenda de un médico");
            System.out.println("  4. Agendar nueva cita");
        } else {
            System.out.println("  3. Ver mi agenda");
            System.out.println("  4. Agendar cita de seguimiento");
        }
        System.out.println("  5. Completar cita");
        System.out.println("  6. Cancelar cita");
        System.out.println("  7. Verificar disponibilidad de médico");
        System.out.println("  0. Volver");
        ConsoleUtil.imprimirSeparador();

        int opcion = ConsoleUtil.leerEntero(sc, "  Opción: ");
        System.out.println();
        switch (opcion) {
            case 1 -> { if (esAdmin) listarTodas();
                        else ConsoleUtil.imprimirError("Acceso restringido al administrador."); }
            case 2 -> { if (esAdmin) listarPorPaciente();
                        else ConsoleUtil.imprimirError("Acceso restringido al administrador."); }
            case 3 -> listarPorMedico(); // médico ve su propia agenda
            case 4 -> agendarCita();     // médico solo puede agendarse a sí mismo (validado en service)
            case 5 -> completarCita();
            case 6 -> cancelarCita();
            case 7 -> verificarDisponibilidad();
            case 0 -> { return; }
            default -> ConsoleUtil.imprimirError("Opción no válida.");
        }
    }
}

    // ── Sub-opciones ──────────────────────────────────────────────────────

    private void listarTodas() {
        List<Cita> lista = citaService.listarTodas();
        if (lista.isEmpty()) {
            ConsoleUtil.imprimirInfo("No hay citas registradas.");
            return;
        }
        imprimirCabecera();
        lista.forEach(this::imprimirFila);
        ConsoleUtil.imprimirSeparador();
        ConsoleUtil.imprimirInfo("Total: " + lista.size() + " cita(s).");
    }

    private void listarPorPaciente() {
        int pid = ConsoleUtil.leerEntero(sc, "  ID del paciente: ");
        // Verificar existencia
        if (pacienteService.obtenerPorId(pid).isEmpty()) {
            ConsoleUtil.imprimirError("No existe un paciente con id=" + pid);
            return;
        }
        List<Cita> lista = citaService.listarPorPaciente(pid);
        if (lista.isEmpty()) {
            ConsoleUtil.imprimirInfo("El paciente no tiene citas registradas.");
            return;
        }
        imprimirCabecera();
        lista.forEach(this::imprimirFila);
        ConsoleUtil.imprimirSeparador();
    }

private void listarPorMedico() {
    boolean esAdmin = Sesion.getUsuario().getRol().equals(Usuario.ROL_ADMIN);
    int mid;

    if (esAdmin) {
        mid = ConsoleUtil.leerEntero(sc, "  ID del médico: ");
        if (medicoService.obtenerPorId(mid).isEmpty()) {
            ConsoleUtil.imprimirError("No existe un médico con id=" + mid);
            return;
        }
    } else {
        // El médico ve automáticamente su propia agenda
        // Por ahora pedimos el ID hasta que se vincule usuario-médico en BD
        mid = ConsoleUtil.leerEntero(sc, "  Tu ID de médico: ");
    }

    List<Cita> lista = citaService.listarPorMedico(mid);
    if (lista.isEmpty()) {
        ConsoleUtil.imprimirInfo("No hay citas agendadas.");
        return;
    }
    imprimirCabecera();
    lista.forEach(this::imprimirFila);
    ConsoleUtil.imprimirSeparador();
}

    private void agendarCita() {
        try {
            int pid   = ConsoleUtil.leerEntero(sc, "  ID del paciente : ");
            int mid   = ConsoleUtil.leerEntero(sc, "  ID del médico   : ");
            String fs = ConsoleUtil.leerTexto(sc,   "  Fecha (yyyy-MM-dd): ");
            LocalDate fecha = LocalDate.parse(fs.trim());

            Cita cita = citaService.agendarCita(pid, mid, fecha);
            ConsoleUtil.imprimirExito("Cita agendada — id=" + cita.getId()
                    + " | fecha=" + cita.getFecha()
                    + " | estado=" + cita.getEstado());
        } catch (DateTimeParseException e) {
            ConsoleUtil.imprimirError("Formato de fecha inválido. Use yyyy-MM-dd (ej: 2025-12-31).");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ConsoleUtil.imprimirError(e.getMessage());
        }
    }

    private void completarCita() {
        int id = ConsoleUtil.leerEntero(sc, "  ID de la cita a completar: ");
        try {
            citaService.completarCita(id);
            ConsoleUtil.imprimirExito("Cita id=" + id + " marcada como COMPLETADA.");
        } catch (IllegalArgumentException e) {
            ConsoleUtil.imprimirError(e.getMessage());
        }
    }

    private void cancelarCita() {
        int id = ConsoleUtil.leerEntero(sc, "  ID de la cita a cancelar: ");
        boolean confirmar = ConsoleUtil.leerConfirmacion(sc, "  ¿Confirmar cancelación?");
        if (!confirmar) {
            ConsoleUtil.imprimirInfo("Operación cancelada.");
            return;
        }
        try {
            citaService.cancelarCita(id);
            ConsoleUtil.imprimirExito("Cita id=" + id + " cancelada.");
        } catch (IllegalArgumentException e) {
            ConsoleUtil.imprimirError(e.getMessage());
        }
    }

    private void verificarDisponibilidad() {
        try {
            int mid   = ConsoleUtil.leerEntero(sc, "  ID del médico: ");
            String fs = ConsoleUtil.leerTexto(sc,   "  Fecha (yyyy-MM-dd): ");
            LocalDate fecha = LocalDate.parse(fs.trim());

            boolean disponible = citaService.verificarDisponibilidad(mid, fecha);
            if (disponible) {
                ConsoleUtil.imprimirExito("El médico está DISPONIBLE el " + fecha + ".");
            } else {
                ConsoleUtil.imprimirError("El médico NO está disponible el " + fecha + ".");
            }
        } catch (DateTimeParseException e) {
            ConsoleUtil.imprimirError("Formato de fecha inválido. Use yyyy-MM-dd.");
        }
    }

    // ── Formato ───────────────────────────────────────────────────────────

    private void imprimirCabecera() {
        ConsoleUtil.imprimirSeparador();
        System.out.printf("  %-4s %-6s %-6s %-12s %-12s%n",
                "ID", "PAC_ID", "MED_ID", "FECHA", "ESTADO");
        ConsoleUtil.imprimirSeparador();
    }

    private void imprimirFila(Cita c) {
        System.out.printf("  %-4d %-6d %-6d %-12s %-12s%n",
                c.getId(), c.getPacienteId(), c.getMedicoId(),
                c.getFecha(), c.getEstado());
    }
}
