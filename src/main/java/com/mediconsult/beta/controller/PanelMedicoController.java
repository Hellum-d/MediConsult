package com.mediconsult.beta.controller;

import com.mediconsult.beta.model.Cita;
import com.mediconsult.beta.model.Medico;
import com.mediconsult.beta.model.Paciente;
import com.mediconsult.beta.service.CitaService;
import com.mediconsult.beta.service.MedicoService;
import com.mediconsult.beta.service.PacienteService;
import com.mediconsult.beta.util.Sesion;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller de panel_medico.fxml — CLINEX
 *
 * <p>Panel inicial del médico tras el login. Muestra sus citas pendientes
 * y permite acceder a la consulta de cada paciente.</p>
 */
public class PanelMedicoController {

    /* ── Inyección FXML ─────────────────────────────────────── */

    @FXML private Label nombreMedicoLabel;
    @FXML private Label especialidadLabel;
    @FXML private Label statPendientesLabel;
    @FXML private Label statHoyLabel;
    @FXML private VBox  citasPendientesView;

    /* ── Servicios ──────────────────────────────────────────── */

    private final CitaService    citaService    = new CitaService();
    private final MedicoService  medicoService  = new MedicoService();
    private final PacienteService pacienteService = new PacienteService();

    private int medicoId;

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        if (Sesion.getUsuario() != null) {
            medicoId = Sesion.getUsuario().getMedicoId();

            if (medicoId > 0) {
                medicoService.obtenerPorId(medicoId).ifPresent(m -> {
                    nombreMedicoLabel.setText("Dr(a). " + m.getNombreCompleto());
                    especialidadLabel.setText(m.getEspecialidadDisplay());
                });
            } else {
                nombreMedicoLabel.setText("Usuario: " + Sesion.getUsuario().getUsername());
                especialidadLabel.setText("⚠ Sin expediente médico vinculado");
            }
        }
        cargarCitas();
    }

    /* ── Cargar y renderizar citas ──────────────────────────── */

    private void cargarCitas() {
        citasPendientesView.getChildren().clear();

        if (medicoId <= 0) {
            Label warn = new Label("⚠  No se encontró el expediente médico vinculado a esta cuenta.\n"
                    + "Contacta al administrador para vincular tu usuario.");
            warn.setStyle("-fx-font-size:13px; -fx-text-fill:#92400E; -fx-wrap-text:true;");
            warn.setWrapText(true);
            citasPendientesView.getChildren().add(warn);
            return;
        }

        try {
            List<Cita> citas = citaService.listarPorMedico(medicoId).stream()
                    .filter(c -> Cita.ESTADO_PENDIENTE.equals(c.getEstado()))
                    .sorted((a, b) -> a.getFecha().compareTo(b.getFecha()))
                    .toList();

            LocalDate hoy = LocalDate.now();
            long hoyCount = citas.stream()
                    .filter(c -> hoy.equals(c.getFecha())).count();
            statPendientesLabel.setText(String.valueOf(citas.size()));
            statHoyLabel.setText(String.valueOf(hoyCount));

            if (citas.isEmpty()) {
                Label empty = new Label("No tienes citas pendientes.");
                empty.setStyle("-fx-font-size:14px; -fx-text-fill:#73777f; -fx-font-style:italic; -fx-padding:16 0;");
                citasPendientesView.getChildren().add(empty);
                return;
            }

            for (Cita c : citas) {
                citasPendientesView.getChildren().add(crearTarjetaCita(c));
            }

        } catch (Exception e) {
            Label err = new Label("Error al cargar citas: " + e.getMessage());
            err.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");
            citasPendientesView.getChildren().add(err);
        }
    }

    private HBox crearTarjetaCita(Cita c) {
        HBox card = new HBox(16);
        card.getStyleClass().add("appointment-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Fecha chip + hora
        VBox fechaBox = new VBox(4);
        fechaBox.setAlignment(Pos.CENTER);
        fechaBox.setMinWidth(90);

        boolean esHoy = LocalDate.now().equals(c.getFecha());
        String diaStr = c.getFecha() != null
                ? c.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
        Label diaLabel = new Label(diaStr);
        diaLabel.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:"
                + (esHoy ? "#065F46" : "#0b1c30") + ";");

        Label horaLabel = new Label(c.getHora() != null ? c.getHora() : "08:00");
        horaLabel.getStyleClass().add("time-chip");

        if (esHoy) {
            Label hoyBadge = new Label("HOY");
            hoyBadge.setStyle("-fx-background-color:#D1FAE5; -fx-background-radius:10; "
                    + "-fx-padding:1 6; -fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#065F46;");
            fechaBox.getChildren().addAll(diaLabel, hoyBadge, horaLabel);
        } else {
            fechaBox.getChildren().addAll(diaLabel, horaLabel);
        }

        // Paciente info
        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        String nombrePaciente = "Paciente ID " + c.getPacienteId();
        try {
            Optional<Paciente> p = pacienteService.obtenerPorId(c.getPacienteId());
            if (p.isPresent()) nombrePaciente = p.get().getNombreCompleto();
        } catch (Exception ignored) {}

        Label pacLabel = new Label(nombrePaciente);
        pacLabel.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");

        String motivo = (c.getMotivo() != null && !c.getMotivo().isBlank()) ? c.getMotivo() : "Sin motivo especificado";
        if (motivo.length() > 55) motivo = motivo.substring(0, 55) + "…";
        Label motivoLabel = new Label(motivo);
        motivoLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f;");

        infoBox.getChildren().addAll(pacLabel, motivoLabel);

        // Botón iniciar consulta
        Button consultarBtn = new Button("Iniciar Consulta →");
        consultarBtn.getStyleClass().add("btn-primary");
        consultarBtn.setStyle("-fx-font-size:13px; -fx-padding:10 16;");

        final int citaId = c.getId();
        final int pacienteId = c.getPacienteId();
        consultarBtn.setOnAction(ev -> abrirConsulta(citaId, pacienteId));

        card.getChildren().addAll(fechaBox, infoBox, consultarBtn);
        return card;
    }

    private void abrirConsulta(int citaId, int pacienteId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/mediconsult/beta/view/fxml/consulta_medica.fxml"));
            Parent root = loader.load();
            ConsultaMedicaController ctrl = loader.getController();
            ctrl.inicializar(citaId, pacienteId);
            Stage stage = (Stage) citasPendientesView.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle("CLINEX — Consulta Médica");
        } catch (Exception e) {
            String causa = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            Alert a = new Alert(Alert.AlertType.ERROR,
                    "No se pudo abrir la consulta.\n" + (causa != null ? causa : e.toString()),
                    ButtonType.OK);
            a.setTitle("Error — CLINEX");
            a.setHeaderText(null);
            a.showAndWait();
        }
    }

    /* ── Cerrar sesión ──────────────────────────────────────── */

    @FXML
    public void onCerrarSesion(ActionEvent event) {
        Sesion.cerrar();
        navegarA("/com/mediconsult/beta/view/fxml/login.fxml", "CLINEX — Iniciar Sesión");
    }

    private void navegarA(String fxmlPath, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) citasPendientesView.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(titulo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
