package com.mediconsult.beta.controller;

import com.mediconsult.beta.model.Medico;
import com.mediconsult.beta.model.Paciente;
import com.mediconsult.beta.model.Remision;
import com.mediconsult.beta.service.MedicoService;
import com.mediconsult.beta.service.PacienteService;
import com.mediconsult.beta.service.RemisionService;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class PanelEspecialistaController {

    /* ── Contexto estático (leído por ConsultaEspecializadaController) ── */

    private static int remisionIdActual;
    private static int pacienteIdActual;

    public static int getRemisionIdActual()  { return remisionIdActual; }
    public static int getPacienteIdActual()  { return pacienteIdActual; }

    /* ── Inyección FXML ─────────────────────────────────────── */

    @FXML private Label nombreEspecialistaLabel;
    @FXML private Label especialidadLabel;
    @FXML private Label statPendientesLabel;
    @FXML private Label statEnAtencionLabel;
    @FXML private Label tituloListaLabel;
    @FXML private VBox  remisionesView;

    /* ── Servicios ──────────────────────────────────────────── */

    private final MedicoService    medicoService    = new MedicoService();
    private final PacienteService  pacienteService  = new PacienteService();
    private final RemisionService  remisionService  = new RemisionService();

    private int    medicoId;
    private String especialidad;

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        medicoId = Sesion.getUsuario() != null ? Sesion.getUsuario().getMedicoId() : 0;

        if (medicoId > 0) {
            Optional<Medico> opt = medicoService.obtenerPorId(medicoId);
            if (opt.isPresent()) {
                Medico m = opt.get();
                especialidad = m.getEspecialidad();
                nombreEspecialistaLabel.setText("Dr(a). " + m.getNombreCompleto());
                especialidadLabel.setText(especialidad);
                tituloListaLabel.setText("Remisiones — " + especialidad);
            }
        } else {
            nombreEspecialistaLabel.setText("Dr(a). " + (Sesion.getUsuario() != null
                    ? Sesion.getUsuario().getUsername() : "Especialista"));
        }

        cargarRemisiones();
    }

    /* ── Cargar y renderizar remisiones ─────────────────────── */

    private void cargarRemisiones() {
        remisionesView.getChildren().clear();

        if (medicoId <= 0 || especialidad == null || especialidad.isBlank()) {
            Label warn = new Label(
                "⚠  No se encontró el expediente médico vinculado a esta cuenta.\n" +
                "Contacta al administrador para vincular tu usuario.");
            warn.setStyle("-fx-font-size:13px; -fx-text-fill:#92400E; -fx-wrap-text:true;");
            warn.setWrapText(true);
            remisionesView.getChildren().add(warn);
            return;
        }

        try {
            // Solo muestra remisiones explícitamente asignadas a este especialista.
            // Las PENDIENTE sin asignar (medicoEspecialistaId=0) no deben aparecer
            // hasta que el admin las asigne mediante Gestión de Remisiones.
            List<Remision> remisiones = remisionService.listarPorEspecialidad(especialidad)
                    .stream()
                    .filter(r -> r.getMedicoEspecialistaId() == medicoId
                              && !Remision.ESTADO_COMPLETADA.equals(r.getEstado())
                              && !Remision.ESTADO_CANCELADA.equals(r.getEstado()))
                    .toList();

            long pendientes  = remisiones.stream()
                    .filter(r -> Remision.ESTADO_PENDIENTE.equals(r.getEstado())).count();
            long enAtencion  = remisiones.stream()
                    .filter(r -> Remision.ESTADO_EN_ATENCION.equals(r.getEstado())).count();
            statPendientesLabel.setText(String.valueOf(pendientes));
            statEnAtencionLabel.setText(String.valueOf(enAtencion));

            if (remisiones.isEmpty()) {
                Label empty = new Label("No hay remisiones activas para tu especialidad.");
                empty.setStyle("-fx-font-size:14px; -fx-text-fill:#73777f; " +
                               "-fx-font-style:italic; -fx-padding:16 0;");
                remisionesView.getChildren().add(empty);
                return;
            }

            for (Remision r : remisiones) {
                remisionesView.getChildren().add(crearTarjetaRemision(r));
            }

        } catch (Exception e) {
            Label err = new Label("Error al cargar remisiones: " + e.getMessage());
            err.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");
            remisionesView.getChildren().add(err);
        }
    }

    private HBox crearTarjetaRemision(Remision r) {
        HBox card = new HBox(16);
        card.getStyleClass().add("appointment-card");
        card.setAlignment(Pos.CENTER_LEFT);

        /* ── Fecha ── */
        VBox fechaBox = new VBox(4);
        fechaBox.setAlignment(Pos.CENTER);
        fechaBox.setMinWidth(90);

        String fechaStr = r.getFechaRegistro() != null
                ? r.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
        Label fechaL = new Label(fechaStr);
        fechaL.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");

        Label estadoBadge = new Label(getEstadoLabel(r.getEstado()));
        estadoBadge.setStyle(getEstadoStyle(r.getEstado()));

        fechaBox.getChildren().addAll(fechaL, estadoBadge);

        /* ── Información del paciente y motivo ── */
        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        String nombrePaciente = "Paciente ID " + r.getPacienteId();
        try {
            Optional<Paciente> p = pacienteService.obtenerPorId(r.getPacienteId());
            if (p.isPresent()) nombrePaciente = p.get().getNombreCompleto();
        } catch (Exception ignored) {}

        Label pacLabel = new Label(nombrePaciente);
        pacLabel.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");

        String motivo = r.getMotivoRemision() != null ? r.getMotivoRemision() : "";
        if (motivo.length() > 70) motivo = motivo.substring(0, 70) + "…";
        Label motivoL = new Label(motivo);
        motivoL.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f; -fx-wrap-text:true;");
        motivoL.setWrapText(true);

        Label remisionIdL = new Label("Remisión #" + r.getId());
        remisionIdL.setStyle("-fx-font-size:11px; -fx-text-fill:#adb5bd;");

        infoBox.getChildren().addAll(pacLabel, motivoL, remisionIdL);

        /* ── Botón atender ── */
        Button atenderBtn = new Button("Atender →");
        atenderBtn.setStyle("-fx-background-color:#1a3a5c; -fx-text-fill:white; " +
                            "-fx-font-weight:bold; -fx-background-radius:10; " +
                            "-fx-padding:10 16; -fx-cursor:hand; -fx-font-size:13px;");

        final int rId  = r.getId();
        final int pId  = r.getPacienteId();
        atenderBtn.setOnAction(ev -> abrirConsultaEspecializada(rId, pId));

        card.getChildren().addAll(fechaBox, infoBox, atenderBtn);
        return card;
    }

    private void abrirConsultaEspecializada(int remisionId, int pacienteId) {
        remisionIdActual = remisionId;
        pacienteIdActual = pacienteId;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/mediconsult/beta/view/fxml/consulta_especializada.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) remisionesView.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle("CLINEX — Consulta Especializada");
        } catch (IOException e) {
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    "La consulta especializada estará disponible próximamente.\n" +
                    "Remisión #" + remisionId + " seleccionada.",
                    ButtonType.OK);
            a.setTitle("CLINEX — Próximamente");
            a.setHeaderText(null);
            a.showAndWait();
        }
    }

    /* ── Utilidades de visualización ────────────────────────── */

    private String getEstadoLabel(String estado) {
        if (estado == null) return "PENDIENTE";
        return switch (estado) {
            case Remision.ESTADO_PENDIENTE   -> "⏳ Pendiente";
            case Remision.ESTADO_ACEPTADA    -> "✓ Aceptada";
            case Remision.ESTADO_EN_ATENCION -> "⚕ En atención";
            case Remision.ESTADO_COMPLETADA  -> "✔ Completada";
            case Remision.ESTADO_CANCELADA   -> "✗ Cancelada";
            default -> estado;
        };
    }

    private String getEstadoStyle(String estado) {
        if (estado == null) return "";
        return switch (estado) {
            case Remision.ESTADO_PENDIENTE ->
                "-fx-background-color:#FEF3C7; -fx-background-radius:10; -fx-padding:2 8; " +
                "-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#92400E;";
            case Remision.ESTADO_ACEPTADA ->
                "-fx-background-color:#D1FAE5; -fx-background-radius:10; -fx-padding:2 8; " +
                "-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#065F46;";
            case Remision.ESTADO_EN_ATENCION ->
                "-fx-background-color:#DBEAFE; -fx-background-radius:10; -fx-padding:2 8; " +
                "-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#1E40AF;";
            default ->
                "-fx-background-color:#e0e0e0; -fx-background-radius:10; -fx-padding:2 8; -fx-font-size:10px;";
        };
    }

    /* ── Navegación ─────────────────────────────────────────── */

    @FXML
    public void onCerrarSesion(ActionEvent event) {
        Sesion.cerrar();
        navegarA("/com/mediconsult/beta/view/fxml/login.fxml", "CLINEX — Iniciar Sesión");
    }

    private void navegarA(String fxmlPath, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) remisionesView.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(titulo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
