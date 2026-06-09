package com.mediconsult.beta.controller;

import com.mediconsult.beta.model.Cita;
import com.mediconsult.beta.model.Medico;
import com.mediconsult.beta.model.Paciente;
import com.mediconsult.beta.service.CitaService;
import com.mediconsult.beta.service.MedicoService;
import com.mediconsult.beta.service.PacienteService;
import com.mediconsult.beta.util.TopBarUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class ListaEsperaController {

    /* ── Inyección FXML ─────────────────────────────────────── */

    @FXML private HBox       topBar;
    @FXML private BorderPane rootPane;
    @FXML private Label      fechaHoyLabel;
    @FXML private Label      statHoyLabel;
    @FXML private Label      statSemanaLabel;
    @FXML private Label      statMedicosLabel;
    @FXML private VBox       cargaMedicosView;
    @FXML private VBox       colaHoyView;
    @FXML private VBox       colaProximaView;

    /* ── Servicios ──────────────────────────────────────────── */

    private final CitaService     citaService     = new CitaService();
    private final PacienteService pacienteService = new PacienteService();
    private final MedicoService   medicoService   = new MedicoService();

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        TopBarUtil.configurar(topBar);
        LocalDate hoy = LocalDate.now();
        String diaStr = hoy.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, new Locale("es", "CO"));
        fechaHoyLabel.setText(diaStr.substring(0, 1).toUpperCase() + diaStr.substring(1)
                + ", " + hoy.format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",
                        new Locale("es", "CO"))));
        cargarLista();
    }

    private void cargarLista() {
        colaHoyView.getChildren().clear();
        colaProximaView.getChildren().clear();
        cargaMedicosView.getChildren().clear();

        LocalDate hoy    = LocalDate.now();
        LocalDate limite = hoy.plusDays(7);

        try {
            List<Cita> pendientes = citaService.listarTodas().stream()
                    .filter(c -> Cita.ESTADO_PENDIENTE.equals(c.getEstado())
                              && c.getFecha() != null
                              && !c.getFecha().isBefore(hoy)
                              && !c.getFecha().isAfter(limite))
                    .sorted(Comparator
                            .comparing(Cita::getFecha)
                            .thenComparing(c -> c.getHora() != null ? c.getHora() : ""))
                    .toList();

            List<Cita> hoyList    = pendientes.stream()
                    .filter(c -> hoy.equals(c.getFecha())).toList();
            List<Cita> proximaList = pendientes.stream()
                    .filter(c -> c.getFecha().isAfter(hoy)).toList();

            /* ── Stats ── */
            long medicosHoy = hoyList.stream()
                    .map(Cita::getMedicoId).distinct().count();
            statHoyLabel.setText(String.valueOf(hoyList.size()));
            statSemanaLabel.setText(String.valueOf(pendientes.size()));
            statMedicosLabel.setText(String.valueOf(medicosHoy));

            /* ── Carga por médico (hoy) ── */
            Map<Integer, Long> cargaMedico = hoyList.stream()
                    .collect(Collectors.groupingBy(Cita::getMedicoId, Collectors.counting()));
            cargaMedico.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                    .forEach(entry -> {
                        String nombre = "Médico ID " + entry.getKey();
                        try {
                            Optional<Medico> m = medicoService.obtenerPorId(entry.getKey());
                            if (m.isPresent()) nombre = "Dr(a). " + m.get().getNombreCompleto();
                        } catch (Exception ignored) {}

                        HBox row = new HBox(8);
                        row.setAlignment(Pos.CENTER_LEFT);
                        Label nomL = new Label(nombre);
                        nomL.setStyle("-fx-font-size:12px; -fx-text-fill:#0b1c30; -fx-wrap-text:true;");
                        nomL.setWrapText(true);
                        HBox.setHgrow(nomL, Priority.ALWAYS);
                        Label cntL = new Label(entry.getValue() + " paciente"
                                + (entry.getValue() > 1 ? "s" : ""));
                        cntL.setStyle("-fx-background-color:#DBEAFE; -fx-background-radius:10; " +
                                      "-fx-padding:2 8; -fx-font-size:11px; " +
                                      "-fx-font-weight:bold; -fx-text-fill:#1E40AF;");
                        row.getChildren().addAll(nomL, cntL);
                        cargaMedicosView.getChildren().add(row);
                    });

            if (cargaMedico.isEmpty()) {
                Label empty = new Label("Sin médicos con citas hoy.");
                empty.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f; -fx-font-style:italic;");
                cargaMedicosView.getChildren().add(empty);
            }

            /* ── Cola de hoy ── */
            if (hoyList.isEmpty()) {
                Label empty = new Label("Sin pacientes en espera hoy.");
                empty.setStyle("-fx-font-size:13px; -fx-text-fill:#73777f; -fx-font-style:italic; -fx-padding:8 0;");
                colaHoyView.getChildren().add(empty);
            } else {
                int pos = 1;
                for (Cita c : hoyList) {
                    colaHoyView.getChildren().add(crearTarjeta(c, pos++, true));
                }
            }

            /* ── Cola próxima semana ── */
            if (proximaList.isEmpty()) {
                Label empty = new Label("Sin citas en los próximos 7 días.");
                empty.setStyle("-fx-font-size:13px; -fx-text-fill:#73777f; -fx-font-style:italic; -fx-padding:8 0;");
                colaProximaView.getChildren().add(empty);
            } else {
                for (Cita c : proximaList) {
                    colaProximaView.getChildren().add(crearTarjeta(c, -1, false));
                }
            }

        } catch (Exception e) {
            Label err = new Label("Error al cargar lista de espera: " + e.getMessage());
            err.setStyle("-fx-font-size:13px; -fx-text-fill:#ba1a1a;");
            colaHoyView.getChildren().add(err);
        }
    }

    /* ── Tarjeta de cita ────────────────────────────────────── */

    private HBox crearTarjeta(Cita cita, int posicion, boolean esHoy) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:12; " +
                      "-fx-border-color:rgba(46,134,222,0.18); -fx-border-radius:12; " +
                      "-fx-border-width:1; -fx-padding:14 18; " +
                      "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.06),8,0,0,2);");

        /* Número de posición (solo para hoy) o fecha */
        VBox posBox = new VBox(2);
        posBox.setAlignment(Pos.CENTER);
        posBox.setMinWidth(56);
        if (esHoy) {
            Label posL = new Label(String.valueOf(posicion));
            posL.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:#ba1a1a;");
            Label enEsperaL = new Label("en espera");
            enEsperaL.setStyle("-fx-font-size:9px; -fx-text-fill:#73777f;");
            posBox.getChildren().addAll(posL, enEsperaL);
        } else {
            String diaStr = cita.getFecha() != null
                    ? cita.getFecha().format(DateTimeFormatter.ofPattern("dd/MM")) : "—";
            Label diaL = new Label(diaStr);
            diaL.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#F59E0B;");
            posBox.getChildren().add(diaL);
        }

        /* Hora */
        VBox horaBox = new VBox(2);
        horaBox.setAlignment(Pos.CENTER);
        horaBox.setMinWidth(60);
        String hora = cita.getHora() != null
                ? cita.getHora().substring(0, Math.min(5, cita.getHora().length())) : "—";
        Label horaL = new Label(hora);
        horaL.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");
        Label horaLabel = new Label("hora");
        horaLabel.setStyle("-fx-font-size:9px; -fx-text-fill:#73777f;");
        horaBox.getChildren().addAll(horaL, horaLabel);

        /* Separador vertical */
        Region vSep = new Region();
        vSep.setStyle("-fx-background-color:rgba(195,198,207,0.5); -fx-min-width:1; -fx-max-width:1; -fx-min-height:40;");

        /* Información paciente + médico */
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        String nombrePaciente = "Paciente ID " + cita.getPacienteId();
        try {
            Optional<Paciente> p = pacienteService.obtenerPorId(cita.getPacienteId());
            if (p.isPresent()) nombrePaciente = p.get().getNombreCompleto();
        } catch (Exception ignored) {}

        String nombreMedico = "Médico ID " + cita.getMedicoId();
        try {
            Optional<Medico> m = medicoService.obtenerPorId(cita.getMedicoId());
            if (m.isPresent()) nombreMedico = "Dr(a). " + m.get().getNombreCompleto();
        } catch (Exception ignored) {}

        Label pacL = new Label(nombrePaciente);
        pacL.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");

        Label medL = new Label(nombreMedico);
        medL.setStyle("-fx-font-size:12px; -fx-text-fill:#43474e;");

        info.getChildren().addAll(pacL, medL);

        if (cita.getMotivo() != null && !cita.getMotivo().isBlank()) {
            String motivo = cita.getMotivo().length() > 50
                    ? cita.getMotivo().substring(0, 50) + "…" : cita.getMotivo();
            Label motivoL = new Label(motivo);
            motivoL.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f; -fx-font-style:italic;");
            info.getChildren().add(motivoL);
        }

        /* Badge PENDIENTE */
        Label badge = new Label("⏳ PENDIENTE");
        badge.setStyle("-fx-background-color:#FEF3C7; -fx-background-radius:10; -fx-padding:4 10; " +
                       "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#92400E;");

        card.getChildren().addAll(posBox, horaBox, vSep, info, badge);
        return card;
    }

    /* ── Navegación ─────────────────────────────────────────── */

    @FXML public void onNavPacientes(ActionEvent e)    { navegarA("/com/mediconsult/beta/view/fxml/pacientes.fxml",         "CLINEX — Pacientes"); }
    @FXML public void onNavMedicos(ActionEvent e)      { navegarA("/com/mediconsult/beta/view/fxml/medicos.fxml",           "CLINEX — Médicos"); }
    @FXML public void onNavCitas(ActionEvent e)        { navegarA("/com/mediconsult/beta/view/fxml/citas.fxml",             "CLINEX — Citas"); }
    @FXML public void onNavDiagnostico(ActionEvent e)  { navegarA("/com/mediconsult/beta/view/fxml/diagnostico.fxml",       "CLINEX — Diagnóstico IA"); }
    @FXML public void onNavRemisiones(ActionEvent e)   { navegarA("/com/mediconsult/beta/view/fxml/gestion_remisiones.fxml","CLINEX — Remisiones"); }
    @FXML public void onNavListaEspera(ActionEvent e)  { /* ya estamos aquí */ }
    @FXML public void onNotificaciones(ActionEvent e) { navegarA("/com/mediconsult/beta/view/fxml/notificaciones_admin.fxml", "CLINEX — Notificaciones"); }

    private void navegarA(String fxmlPath, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(titulo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
