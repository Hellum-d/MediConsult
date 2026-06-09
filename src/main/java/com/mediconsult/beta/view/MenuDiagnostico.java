package com.mediconsult.beta.view;
import com.mediconsult.beta.model.Diagnostico;
import com.mediconsult.beta.service.CitaService;
import com.mediconsult.beta.service.DiagnosticoService;
import com.mediconsult.beta.service.PacienteService;
import com.mediconsult.beta.util.ConsoleUtil;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Menú de consola para diagnósticos asistidos por IA en MediConsult-Beta.
 *
 * @author MediConsult Team
 * @version 0.1
 */
public class MenuDiagnostico {

    private final Scanner            sc;
    private final DiagnosticoService diagnosticoService;
    private final CitaService        citaService;
    private final PacienteService    pacienteService;

    public MenuDiagnostico(Scanner sc) {
        this.sc                 = sc;
        this.diagnosticoService = new DiagnosticoService();
        this.citaService        = new CitaService();
        this.pacienteService    = new PacienteService();
    }

    /** Muestra el menú de diagnósticos en bucle. */
    public void mostrar() {
        while (true) {
            System.out.println();
            ConsoleUtil.imprimirEncabezado("DIAGNÓSTICO IA");
            System.out.println("  1. Generar diagnóstico para una cita");
            System.out.println("  2. Ver diagnóstico de una cita");
            System.out.println("  3. Historial de diagnósticos de un paciente");
            System.out.println("  0. Volver");
            ConsoleUtil.imprimirSeparador();

            int opcion = ConsoleUtil.leerEntero(sc, "  Opción: ");
            System.out.println();
            switch (opcion) {
                case 1 -> generarDiagnostico();
                case 2 -> verDiagnosticoPorCita();
                case 3 -> historialPaciente();
                case 0 -> { return; }
                default -> ConsoleUtil.imprimirError("Opción no válida.");
            }
        }
    }

    // ── Sub-opciones ──────────────────────────────────────────────────────

    private void generarDiagnostico() {
        try {
            int citaId     = ConsoleUtil.leerEntero(sc, "  ID de la cita    : ");
            int pacienteId = ConsoleUtil.leerEntero(sc, "  ID del paciente  : ");

            // Verificaciones previas
            if (citaService.obtenerPorId(citaId).isEmpty()) {
                ConsoleUtil.imprimirError("No existe una cita con id=" + citaId);
                return;
            }
            if (pacienteService.obtenerPorId(pacienteId).isEmpty()) {
                ConsoleUtil.imprimirError("No existe un paciente con id=" + pacienteId);
                return;
            }

            String sintomas = ConsoleUtil.leerTexto(sc, "  Síntomas         : ");

            ConsoleUtil.imprimirSeparador();
            ConsoleUtil.imprimirInfo("Consultando OpenAI GPT-4o-mini, por favor espere...");
            ConsoleUtil.imprimirSeparador();

            Diagnostico diagnostico = diagnosticoService.generarDiagnostico(
                    citaId, pacienteId, sintomas);

            ConsoleUtil.imprimirSeparador();
            System.out.println("  RESULTADO IA (id=" + diagnostico.getId() + "):");
            System.out.println();
            for (String linea : diagnostico.getResultadoIa().split("\n")) {
                System.out.println("  " + linea);
            }
            ConsoleUtil.imprimirSeparador();

            // Ofrecer completar la cita tras el diagnóstico
            boolean completar = ConsoleUtil.leerConfirmacion(sc,
                    "  ¿Marcar la cita como COMPLETADA?");
            if (completar) {
                try {
                    citaService.completarCita(citaId);
                    ConsoleUtil.imprimirExito("Cita marcada como COMPLETADA.");
                } catch (IllegalArgumentException e) {
                    ConsoleUtil.imprimirError(e.getMessage());
                }
            }

        } catch (IllegalArgumentException e) {
            ConsoleUtil.imprimirError(e.getMessage());
        } catch (Exception e) {
            ConsoleUtil.imprimirError("Error inesperado: " + e.getMessage());
        }
    }

    private void verDiagnosticoPorCita() {
        int citaId = ConsoleUtil.leerEntero(sc, "  ID de la cita: ");
        Optional<Diagnostico> opt = diagnosticoService.obtenerPorCita(citaId);
        if (opt.isEmpty()) {
            ConsoleUtil.imprimirInfo("No existe diagnóstico para la cita id=" + citaId
                    + ". Genérelo con la opción 1.");
            return;
        }
        Diagnostico d = opt.get();
        ConsoleUtil.imprimirSeparador();
        System.out.println("  Diagnóstico id : " + d.getId());
        System.out.println("  Cita id        : " + d.getCitaId());
        System.out.println("  Síntomas       : " + d.getSintomas());
        System.out.println();
        System.out.println("  Resultado IA   :");
        for (String linea : d.getResultadoIa().split("\n")) {
            System.out.println("    " + linea);
        }
        ConsoleUtil.imprimirSeparador();
    }

    private void historialPaciente() {
        int pacienteId = ConsoleUtil.leerEntero(sc, "  ID del paciente: ");
        if (pacienteService.obtenerPorId(pacienteId).isEmpty()) {
            ConsoleUtil.imprimirError("No existe un paciente con id=" + pacienteId);
            return;
        }
        List<Diagnostico> lista = diagnosticoService.listarPorPaciente(pacienteId);
        if (lista.isEmpty()) {
            ConsoleUtil.imprimirInfo("El paciente no tiene diagnósticos registrados.");
            return;
        }
        ConsoleUtil.imprimirSeparador();
        System.out.printf("  %-5s %-7s %-30s%n", "DX_ID", "CITA_ID", "SÍNTOMAS");
        ConsoleUtil.imprimirSeparador();
        for (Diagnostico d : lista) {
            System.out.printf("  %-5d %-7d %-30s%n",
                    d.getId(), d.getCitaId(),
                    truncar(d.getSintomas(), 30));
        }
        ConsoleUtil.imprimirSeparador();
        ConsoleUtil.imprimirInfo("Use la opción 2 con un ID de cita para ver el resultado completo.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String truncar(String s, int max) {
        if (s == null) return "-";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
