package com.mediconsult.beta.controller;

import com.mediconsult.beta.model.*;
import com.mediconsult.beta.service.*;
import com.mediconsult.beta.util.Sesion;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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

public class AltaMedicaController {

    /* ── Contexto estático (pasado por ConsultaEspecializadaController) ── */

    private static int remisionId;
    private static int consultaEspecializadaId;
    private static int pacienteId;

    public static void setContexto(int remisionId, int consultaEspecializadaId, int pacienteId) {
        AltaMedicaController.remisionId              = remisionId;
        AltaMedicaController.consultaEspecializadaId = consultaEspecializadaId;
        AltaMedicaController.pacienteId              = pacienteId;
    }

    /* ── Inyección FXML — Panel izquierdo ───────────────────── */

    @FXML private Label infoAltaLabel;
    @FXML private Label nombrePacienteLabel;
    @FXML private Label correoPacienteLabel;
    @FXML private Label edadLabel;
    @FXML private Label tipoSangreLabel;
    @FXML private VBox  consultaResumenView;

    /* ── Inyección FXML — Panel derecho ─────────────────────── */

    @FXML private TextField  diagnosticoFinalField;
    @FXML private ComboBox<String> tipoAltaCombo;
    @FXML private DatePicker fechaAltaPicker;
    @FXML private TextArea   indicacionesPacienteArea;
    @FXML private TextArea   indicacionesMedicoArea;
    @FXML private TextArea   observacionesArea;
    @FXML private Label      statusLabel;
    @FXML private Button     emitirAltaBtn;

    /* ── Servicios ──────────────────��───────────────────────── */

    private final PacienteService              pacienteService  = new PacienteService();
    private final ConsultaEspecializadaService consultaService  = new ConsultaEspecializadaService();
    private final AltaMedicaService            altaService      = new AltaMedicaService();

    /* ── Inicialización ────────────���────────────────────────── */

    @FXML
    public void initialize() {
        tipoAltaCombo.getItems().addAll(
                AltaMedica.TIPO_CURADO,
                AltaMedica.TIPO_MEJORADO,
                AltaMedica.TIPO_SEGUIMIENTO,
                AltaMedica.TIPO_TRASLADO,
                AltaMedica.TIPO_VOLUNTARIA);

        fechaAltaPicker.setValue(LocalDate.now());

        infoAltaLabel.setText("Remisión #" + remisionId + "  ·  Paciente ID " + pacienteId);

        cargarDatosPaciente();
        cargarResumenConsulta();
        verificarAltaExistente();
    }

    private void cargarDatosPaciente() {
        try {
            Optional<Paciente> opt = pacienteService.obtenerPorId(pacienteId);
            if (opt.isPresent()) {
                Paciente p = opt.get();
                nombrePacienteLabel.setText(p.getNombreCompleto());
                correoPacienteLabel.setText(p.getCorreo() != null && !p.getCorreo().isBlank()
                        ? p.getCorreo() : "—");
                edadLabel.setText(p.getEdad() + " años");
                tipoSangreLabel.setText(p.getTipoSangre() != null && !p.getTipoSangre().isBlank()
                        ? p.getTipoSangre() : "No registrado");
                infoAltaLabel.setText("Remisión #" + remisionId + "  ·  " + p.getNombreCompleto());
            } else {
                nombrePacienteLabel.setText("Paciente ID " + pacienteId);
            }
        } catch (Exception e) {
            nombrePacienteLabel.setText("Error al cargar datos");
        }
    }

    private void cargarResumenConsulta() {
        consultaResumenView.getChildren().clear();
        try {
            Optional<ConsultaEspecializada> opt =
                    consultaService.obtenerPorRemision(remisionId);
            if (opt.isEmpty()) {
                Label warn = new Label("Sin consulta especializada registrada.");
                warn.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f; -fx-font-style:italic;");
                consultaResumenView.getChildren().add(warn);
                return;
            }
            ConsultaEspecializada c = opt.get();

            if (!c.getDiagnostico().isBlank()) {
                diagnosticoFinalField.setText(c.getDiagnostico());
            }

            VBox card = new VBox(6);
            card.setStyle("-fx-background-color:#f0f4f8; -fx-background-radius:10; " +
                          "-fx-padding:10 14; -fx-border-color:rgba(26,58,92,0.15); " +
                          "-fx-border-radius:10; -fx-border-width:1;");

            String fechaStr = c.getFechaRegistro() != null
                    ? c.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
            Label fechaL = new Label("📋  Consulta del " + fechaStr);
            fechaL.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;");

            if (!c.getDiagnostico().isBlank()) {
                Label dxL = new Label("Dx: " + c.getDiagnostico());
                dxL.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#0b1c30; -fx-wrap-text:true;");
                dxL.setWrapText(true);
                card.getChildren().addAll(fechaL, dxL);
            } else {
                card.getChildren().add(fechaL);
            }

            if (!c.getTratamiento().isBlank()) {
                String tx = c.getTratamiento().length() > 80
                        ? c.getTratamiento().substring(0, 80) + "…" : c.getTratamiento();
                Label txL = new Label("Tx: " + tx);
                txL.setStyle("-fx-font-size:12px; -fx-text-fill:#43474e; -fx-wrap-text:true;");
                txL.setWrapText(true);
                card.getChildren().add(txL);
            }

            Label estadoL = new Label("Estado: " + c.getEstado());
            estadoL.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f;");
            card.getChildren().add(estadoL);

            consultaResumenView.getChildren().add(card);

        } catch (Exception e) {
            Label err = new Label("Error al cargar consulta: " + e.getMessage());
            err.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");
            consultaResumenView.getChildren().add(err);
        }
    }

    private void verificarAltaExistente() {
        try {
            Optional<AltaMedica> opt = altaService.buscarPorRemision(remisionId);
            if (opt.isPresent()) {
                AltaMedica a = opt.get();
                diagnosticoFinalField.setText(a.getDiagnosticoFinal());
                tipoAltaCombo.setValue(a.getTipoAlta());
                if (a.getFechaAlta() != null) fechaAltaPicker.setValue(a.getFechaAlta());
                indicacionesPacienteArea.setText(a.getIndicacionesPaciente());
                indicacionesMedicoArea.setText(a.getIndicacionesMedicoRemitente());
                observacionesArea.setText(a.getObservaciones());
                emitirAltaBtn.setDisable(true);
                emitirAltaBtn.setText("✓  Alta ya emitida");
                statusLabel.setText("Alta emitida el "
                        + (a.getFechaAlta() != null
                                ? a.getFechaAlta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                : "—"));
            }
        } catch (Exception e) {
            statusLabel.setText("No se pudo verificar alta previa.");
        }
    }

    /* ── Emitir Alta ────────���───────────────────────────────── */

    @FXML
    public void onEmitirAlta(ActionEvent event) {
        if (tipoAltaCombo.getValue() == null) {
            statusLabel.setText("⚠  Selecciona el tipo de alta.");
            return;
        }
        if (fechaAltaPicker.getValue() == null) {
            statusLabel.setText("⚠  Selecciona la fecha de alta.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Confirmar el alta médica del paciente?\n\n" +
                "Tipo: " + tipoAltaCombo.getValue() + "\n" +
                "Fecha: " + fechaAltaPicker.getValue()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n\n" +
                "La consulta especializada y la remisión quedarán COMPLETADAS.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar Alta Médica — CLINEX");
        confirm.setHeaderText(null);

        confirm.showAndWait().ifPresent(resp -> {
            if (resp != ButtonType.YES) return;

            int medicoId = Sesion.getUsuario() != null ? Sesion.getUsuario().getMedicoId() : 0;
            AltaMedica alta = new AltaMedica(
                    remisionId, consultaEspecializadaId, pacienteId, medicoId);
            alta.setDiagnosticoFinal(diagnosticoFinalField.getText().trim());
            alta.setTipoAlta(tipoAltaCombo.getValue());
            alta.setFechaAlta(fechaAltaPicker.getValue());
            alta.setIndicacionesPaciente(indicacionesPacienteArea.getText().trim());
            alta.setIndicacionesMedicoRemitente(indicacionesMedicoArea.getText().trim());
            alta.setObservaciones(observacionesArea.getText().trim());

            emitirAltaBtn.setDisable(true);
            emitirAltaBtn.setText("Procesando…");

            Task<AltaMedica> task = new Task<>() {
                @Override protected AltaMedica call() {
                    return altaService.emitirAlta(alta);
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                Alert ok = new Alert(Alert.AlertType.INFORMATION,
                        "Alta médica emitida correctamente.\n" +
                        "La remisión y la consulta han sido cerradas.",
                        ButtonType.OK);
                ok.setTitle("Alta emitida — CLINEX");
                ok.setHeaderText(null);
                ok.showAndWait();
                navegarA("/com/mediconsult/beta/view/fxml/panel_especialista.fxml",
                         "CLINEX — Panel Especialista");
            }));
            task.setOnFailed(e -> Platform.runLater(() -> {
                String msg = task.getException() != null
                        ? task.getException().getMessage() : "Error desconocido";
                statusLabel.setText("⚠  Error al emitir alta: " + msg);
                emitirAltaBtn.setDisable(false);
                emitirAltaBtn.setText("✓  Confirmar y Emitir Alta Médica");
            }));
            new Thread(task, "alta-medica-thread").start();
        });
    }

    /* ── Navegación ─────────────��───────────────────────────── */

    @FXML
    public void onVolver(ActionEvent event) {
        navegarA("/com/mediconsult/beta/view/fxml/consulta_especializada.fxml",
                 "CLINEX — Consulta Especializada");
    }

    @FXML
    public void onCerrarSesion(ActionEvent event) {
        Sesion.cerrar();
        navegarA("/com/mediconsult/beta/view/fxml/login.fxml", "CLINEX — Iniciar Sesión");
    }

    private void navegarA(String fxmlPath, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) emitirAltaBtn.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(titulo);
        } catch (IOException e) {
            statusLabel.setText("Error de navegación: " + e.getMessage());
        }
    }
}
