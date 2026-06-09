package com.mediconsult.beta.controller;

import com.mediconsult.beta.model.*;
import com.mediconsult.beta.service.*;
import com.mediconsult.beta.util.Sesion;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ConsultaEspecializadaController {

    /* ── Inyección FXML — Panel izquierdo ───────────────────── */

    @FXML private Label infoRemisionLabel;
    @FXML private Label nombrePacienteLabel;
    @FXML private Label correoPacienteLabel;
    @FXML private Label edadLabel;
    @FXML private Label tipoSangreLabel;
    @FXML private Label alergiasLabel;
    @FXML private VBox  remisionDetalleView;
    @FXML private VBox  historiasAnterioresView;

    /* ── Inyección FXML — Panel derecho ─────────────────────── */

    @FXML private TextArea  motivoConsultaArea;
    @FXML private TextArea  hallazgosArea;
    @FXML private TextField diagnosticoField;
    @FXML private TextArea  tratamientoArea;
    @FXML private TextArea  evolucionArea;
    @FXML private TextArea  observacionesArea;
    @FXML private VBox      ordenesEspView;
    @FXML private Button    nuevaOrdenEspBtn;
    @FXML private Label     statusLabel;
    @FXML private Button    guardarBtn;
    @FXML private Button    altaMedicaBtn;

    /* ── Servicios ──────────────────────────────────────────── */

    private final PacienteService              pacienteService    = new PacienteService();
    private final RemisionService              remisionService    = new RemisionService();
    private final HistoriaClinicaService       historiaService    = new HistoriaClinicaService();
    private final ConsultaEspecializadaService consultaService    = new ConsultaEspecializadaService();
    private final OrdenEspecializadaService    ordenService       = new OrdenEspecializadaService();

    /* ── Estado local ───────────────────────────────────────── */

    private int                   remisionId;
    private int                   pacienteId;
    private ConsultaEspecializada consultaActual;

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        remisionId = PanelEspecialistaController.getRemisionIdActual();
        pacienteId = PanelEspecialistaController.getPacienteIdActual();

        cargarDatosPaciente();
        cargarDetallesRemision();
        cargarHistoriasAnteriores();
        cargarConsultaExistente();
        actualizarEstadoRemision();
        cargarOrdenes();
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
                alergiasLabel.setText(p.getAlergias() != null && !p.getAlergias().isBlank()
                        ? p.getAlergias() : "Ninguna");
            } else {
                nombrePacienteLabel.setText("Paciente ID " + pacienteId);
            }
        } catch (Exception e) {
            nombrePacienteLabel.setText("Error al cargar datos");
        }
    }

    private void cargarDetallesRemision() {
        remisionDetalleView.getChildren().clear();
        try {
            Optional<Remision> opt = remisionService.buscarPorId(remisionId);
            if (opt.isEmpty()) {
                Label empty = new Label("Remisión #" + remisionId + " no encontrada.");
                empty.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");
                remisionDetalleView.getChildren().add(empty);
                return;
            }
            Remision r = opt.get();

            String fecha = r.getFechaRegistro() != null
                    ? r.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
            infoRemisionLabel.setText("Remisión #" + remisionId + "  ·  " + r.getEspecialidadDestino()
                    + "  ·  " + fecha);

            VBox card = new VBox(6);
            card.setStyle("-fx-background-color:#EEF2F8; -fx-background-radius:10; " +
                          "-fx-border-color:rgba(26,58,92,0.2); -fx-border-radius:10; " +
                          "-fx-border-width:1; -fx-padding:10 14;");

            Label espL = new Label("🔗  " + r.getEspecialidadDestino());
            espL.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;");

            Label fechaL = new Label("Fecha: " + fecha);
            fechaL.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f;");

            Label motivoL = new Label(r.getMotivoRemision() != null ? r.getMotivoRemision() : "");
            motivoL.setStyle("-fx-font-size:13px; -fx-text-fill:#0b1c30; -fx-wrap-text:true;");
            motivoL.setWrapText(true);

            card.getChildren().addAll(espL, fechaL, motivoL);

            if (r.getObservaciones() != null && !r.getObservaciones().isBlank()) {
                Label obsL = new Label("Obs: " + r.getObservaciones());
                obsL.setStyle("-fx-font-size:12px; -fx-text-fill:#43474e; " +
                              "-fx-font-style:italic; -fx-wrap-text:true;");
                obsL.setWrapText(true);
                card.getChildren().add(obsL);
            }

            remisionDetalleView.getChildren().add(card);

        } catch (Exception e) {
            Label err = new Label("Error al cargar remisión: " + e.getMessage());
            err.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");
            remisionDetalleView.getChildren().add(err);
        }
    }

    private void cargarHistoriasAnteriores() {
        historiasAnterioresView.getChildren().clear();
        try {
            List<HistoriaClinica> lista = historiaService.listarPorPaciente(pacienteId)
                    .stream().limit(3).toList();
            if (lista.isEmpty()) {
                Label empty = new Label("Sin historias clínicas previas.");
                empty.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f; -fx-font-style:italic;");
                historiasAnterioresView.getChildren().add(empty);
                return;
            }
            for (HistoriaClinica h : lista) {
                VBox card = new VBox(4);
                card.setStyle("-fx-background-color:#f0f4f8; -fx-background-radius:8; -fx-padding:8 10;");
                String fecha = h.getFechaRegistro() != null
                        ? h.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
                Label fechaL = new Label("📋  " + fecha);
                fechaL.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#0060aa;");
                String dx = h.getDiagnostico() != null && !h.getDiagnostico().isBlank()
                        ? "Dx: " + h.getDiagnostico() : "Diagnóstico pendiente";
                if (dx.length() > 70) dx = dx.substring(0, 70) + "…";
                Label dxL = new Label(dx);
                dxL.setStyle("-fx-font-size:12px; -fx-text-fill:#0b1c30;");
                card.getChildren().addAll(fechaL, dxL);
                historiasAnterioresView.getChildren().add(card);
            }
        } catch (Exception ignored) {}
    }

    private void cargarConsultaExistente() {
        try {
            Optional<ConsultaEspecializada> opt = consultaService.obtenerPorRemision(remisionId);
            if (opt.isPresent()) {
                consultaActual = opt.get();
                motivoConsultaArea.setText(consultaActual.getMotivoConsulta());
                hallazgosArea.setText(consultaActual.getHallazgos());
                diagnosticoField.setText(consultaActual.getDiagnostico());
                tratamientoArea.setText(consultaActual.getTratamiento());
                evolucionArea.setText(consultaActual.getEvolucion());
                observacionesArea.setText(consultaActual.getObservaciones());
                statusLabel.setText("Consulta cargada — guardada el "
                        + (consultaActual.getFechaRegistro() != null
                                ? consultaActual.getFechaRegistro()
                                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                : "—"));
            }
        } catch (Exception e) {
            statusLabel.setText("No se pudo cargar consulta previa.");
        }
    }

    private void actualizarEstadoRemision() {
        try {
            remisionService.buscarPorId(remisionId).ifPresent(r -> {
                if (Remision.ESTADO_PENDIENTE.equals(r.getEstado())
                        || Remision.ESTADO_ACEPTADA.equals(r.getEstado())) {
                    r.setEstado(Remision.ESTADO_EN_ATENCION);
                    remisionService.actualizar(r);
                }
            });
        } catch (Exception e) {
            System.err.println("[ConsultaEsp] No se pudo actualizar estado remisión: " + e.getMessage());
        }
    }

    /* ── Órdenes Especializadas ─────────────────────────────── */

    private void cargarOrdenes() {
        ordenesEspView.getChildren().clear();
        if (consultaActual == null) {
            Label hint = new Label("Guarda la consulta primero para agregar órdenes.");
            hint.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f; -fx-font-style:italic;");
            ordenesEspView.getChildren().add(hint);
            return;
        }
        try {
            List<OrdenEspecializada> ordenes = ordenService.listarPorConsulta(consultaActual.getId());
            if (ordenes.isEmpty()) {
                Label empty = new Label("Sin órdenes especializadas registradas.");
                empty.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f; -fx-font-style:italic;");
                ordenesEspView.getChildren().add(empty);
                return;
            }
            for (OrdenEspecializada o : ordenes) {
                ordenesEspView.getChildren().add(crearTarjetaOrden(o));
            }
        } catch (Exception e) {
            Label err = new Label("Error al cargar órdenes: " + e.getMessage());
            err.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");
            ordenesEspView.getChildren().add(err);
        }
    }

    private VBox crearTarjetaOrden(OrdenEspecializada orden) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:10; " +
                      "-fx-border-color:rgba(26,58,92,0.2); -fx-border-radius:10; " +
                      "-fx-border-width:1; -fx-padding:10 14;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label tipoBadge = new Label(getTipoLabel(orden.getTipo()));
        tipoBadge.setStyle(getTipoStyle(orden.getTipo()));

        String fecha = orden.getFechaRegistro() != null
                ? orden.getFechaRegistro().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";
        Label fechaL = new Label(fecha);
        fechaL.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button elimBtn = new Button("×");
        elimBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#ba1a1a; " +
                         "-fx-font-size:16px; -fx-cursor:hand; -fx-padding:0 4;");
        elimBtn.setOnAction(e -> {
            try {
                ordenService.eliminar(orden.getId());
                cargarOrdenes();
            } catch (Exception ex) {
                statusLabel.setText("⚠  Error al eliminar orden: " + ex.getMessage());
            }
        });

        header.getChildren().addAll(tipoBadge, fechaL, spacer, elimBtn);

        Label descL = new Label(orden.getDescripcion());
        descL.setStyle("-fx-font-size:13px; -fx-text-fill:#0b1c30; -fx-wrap-text:true;");
        descL.setWrapText(true);

        card.getChildren().addAll(header, descL);

        if (orden.getObservaciones() != null && !orden.getObservaciones().isBlank()) {
            Label obsL = new Label("Obs: " + orden.getObservaciones());
            obsL.setStyle("-fx-font-size:12px; -fx-text-fill:#43474e; " +
                          "-fx-font-style:italic; -fx-wrap-text:true;");
            obsL.setWrapText(true);
            card.getChildren().add(obsL);
        }

        return card;
    }

    private String getTipoLabel(String tipo) {
        if (tipo == null) return "—";
        return switch (tipo) {
            case OrdenEspecializada.TIPO_MEDICAMENTO   -> "💊  Medicamento";
            case OrdenEspecializada.TIPO_PROCEDIMIENTO -> "🩺  Procedimiento";
            case OrdenEspecializada.TIPO_EXAMEN        -> "🔬  Examen";
            default -> tipo;
        };
    }

    private String getTipoStyle(String tipo) {
        if (tipo == null) return "";
        return switch (tipo) {
            case OrdenEspecializada.TIPO_MEDICAMENTO ->
                "-fx-background-color:#DBEAFE; -fx-background-radius:12; -fx-padding:3 10; " +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1E40AF;";
            case OrdenEspecializada.TIPO_PROCEDIMIENTO ->
                "-fx-background-color:#EDE9FE; -fx-background-radius:12; -fx-padding:3 10; " +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#5B21B6;";
            case OrdenEspecializada.TIPO_EXAMEN ->
                "-fx-background-color:#D1FAE5; -fx-background-radius:12; -fx-padding:3 10; " +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#065F46;";
            default ->
                "-fx-background-color:#e0e0e0; -fx-background-radius:12; -fx-padding:3 10; -fx-font-size:11px;";
        };
    }

    @FXML
    public void onNuevaOrdenEsp(ActionEvent event) {
        if (consultaActual == null) {
            statusLabel.setText("⚠  Guarda la consulta antes de agregar órdenes.");
            return;
        }
        Stage owner = (Stage) nuevaOrdenEspBtn.getScene().getWindow();
        Stage modal = new Stage();
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.initOwner(owner);
        modal.initModality(Modality.APPLICATION_MODAL);

        ComboBox<String> tipoCombo = new ComboBox<>();
        tipoCombo.getItems().addAll(
                OrdenEspecializada.TIPO_MEDICAMENTO,
                OrdenEspecializada.TIPO_PROCEDIMIENTO,
                OrdenEspecializada.TIPO_EXAMEN);
        tipoCombo.setPromptText("Seleccionar tipo...");
        tipoCombo.setMaxWidth(Double.MAX_VALUE);
        tipoCombo.setStyle("-fx-background-radius:8; -fx-border-radius:8; " +
                           "-fx-border-color:rgba(26,58,92,0.3); -fx-font-size:13px;");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Describe la orden especializada en detalle...");
        descArea.setPrefRowCount(4);
        descArea.setWrapText(true);
        descArea.setStyle("-fx-background-radius:8; -fx-border-color:rgba(26,58,92,0.3); " +
                          "-fx-border-radius:8; -fx-font-size:13px; -fx-padding:8;");

        TextArea obsArea = new TextArea();
        obsArea.setPromptText("Observaciones adicionales (opcional)...");
        obsArea.setPrefRowCount(2);
        obsArea.setWrapText(true);
        obsArea.setStyle("-fx-background-radius:8; -fx-border-color:rgba(26,58,92,0.3); " +
                         "-fx-border-radius:8; -fx-font-size:13px; -fx-padding:8;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");

        Button guardarModalBtn = new Button("Guardar Orden");
        guardarModalBtn.setStyle("-fx-background-color:#1a3a5c; -fx-text-fill:white; " +
                                 "-fx-font-weight:bold; -fx-background-radius:10; " +
                                 "-fx-padding:10 24; -fx-cursor:hand; -fx-font-size:13px;");

        Button cancelarBtn = new Button("Cancelar");
        cancelarBtn.setStyle("-fx-background-color:transparent; " +
                             "-fx-border-color:rgba(26,58,92,0.35); -fx-border-radius:10; " +
                             "-fx-background-radius:10; -fx-text-fill:#1a3a5c; " +
                             "-fx-font-weight:bold; -fx-padding:10 24; -fx-cursor:hand; -fx-font-size:13px;");
        cancelarBtn.setOnAction(e -> modal.close());

        guardarModalBtn.setOnAction(e -> {
            if (tipoCombo.getValue() == null) {
                errorLabel.setText("Selecciona el tipo de orden.");
                return;
            }
            if (descArea.getText().trim().isEmpty()) {
                errorLabel.setText("La descripción es obligatoria.");
                return;
            }
            int medicoId = Sesion.getUsuario() != null ? Sesion.getUsuario().getMedicoId() : 0;
            OrdenEspecializada orden = new OrdenEspecializada(
                    consultaActual.getId(), pacienteId, medicoId);
            orden.setTipo(tipoCombo.getValue());
            orden.setDescripcion(descArea.getText().trim());
            orden.setObservaciones(obsArea.getText().trim());
            try {
                ordenService.guardar(orden);
                modal.close();
                cargarOrdenes();
            } catch (Exception ex) {
                errorLabel.setText("Error: " + ex.getMessage());
            }
        });

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.getChildren().addAll(cancelarBtn, guardarModalBtn);

        Label titleL = new Label("Nueva Orden Especializada");
        titleL.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");

        Label lblTipo = new Label("TIPO DE ORDEN");
        lblTipo.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;");
        Label lblDesc = new Label("DESCRIPCIÓN");
        lblDesc.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;");
        Label lblObs  = new Label("OBSERVACIONES");
        lblObs.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;");

        VBox card = new VBox(12);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:16; " +
                      "-fx-padding:28; -fx-max-width:480; " +
                      "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.22),24,0,0,6);");
        card.getChildren().addAll(titleL, lblTipo, tipoCombo, lblDesc, descArea,
                                  lblObs, obsArea, errorLabel, btnRow);
        VBox.setMargin(btnRow, new Insets(4, 0, 0, 0));

        StackPane overlay = new StackPane(card);
        overlay.setStyle("-fx-background-color:rgba(0,36,68,0.55);");
        StackPane.setAlignment(card, Pos.CENTER);

        Scene scene = new Scene(overlay, owner.getWidth(), owner.getHeight());
        scene.setFill(Color.TRANSPARENT);
        modal.setScene(scene);
        modal.show();
    }

    /* ── Guardar consulta ───────────────────────────────────── */

    @FXML
    public void onGuardar(ActionEvent event) {
        int medicoId = Sesion.getUsuario() != null ? Sesion.getUsuario().getMedicoId() : 0;

        ConsultaEspecializada c = new ConsultaEspecializada(remisionId, pacienteId, medicoId);
        if (consultaActual != null) c.setId(consultaActual.getId());
        c.setMotivoConsulta(motivoConsultaArea.getText().trim());
        c.setHallazgos(hallazgosArea.getText().trim());
        c.setDiagnostico(diagnosticoField.getText().trim());
        c.setTratamiento(tratamientoArea.getText().trim());
        c.setEvolucion(evolucionArea.getText().trim());
        c.setObservaciones(observacionesArea.getText().trim());
        c.setEstado(ConsultaEspecializada.ESTADO_ACTIVA);

        guardarBtn.setDisable(true);
        guardarBtn.setText("Guardando…");

        Task<ConsultaEspecializada> task = new Task<>() {
            @Override protected ConsultaEspecializada call() {
                return consultaService.guardarOActualizar(c);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            consultaActual = task.getValue();
            statusLabel.setText("✓  Consulta guardada correctamente.");
            guardarBtn.setDisable(false);
            guardarBtn.setText("💾  Guardar Consulta");
            cargarOrdenes();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            String msg = task.getException() != null
                    ? task.getException().getMessage() : "Error desconocido";
            statusLabel.setText("⚠  Error al guardar: " + msg);
            guardarBtn.setDisable(false);
            guardarBtn.setText("💾  Guardar Consulta");
        }));
        new Thread(task, "ce-save-thread").start();
    }

    /* ── Alta Médica ────────────────────────────────────────── */

    @FXML
    public void onAltaMedica(ActionEvent event) {
        if (consultaActual == null) {
            statusLabel.setText("⚠  Guarda la consulta antes de emitir el alta.");
            return;
        }
        AltaMedicaController.setContexto(remisionId, consultaActual.getId(), pacienteId);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/mediconsult/beta/view/fxml/alta_medica.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) guardarBtn.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle("CLINEX — Alta Médica");
        } catch (IOException e) {
            statusLabel.setText("Error al abrir Alta Médica: " + e.getMessage());
        }
    }

    /* ── Navegación ─────────────────────────────────────────── */

    @FXML
    public void onVolver(ActionEvent event) {
        navegarA("/com/mediconsult/beta/view/fxml/panel_especialista.fxml",
                 "CLINEX — Panel Especialista");
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
            Stage stage = (Stage) guardarBtn.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(titulo);
        } catch (IOException e) {
            statusLabel.setText("Error de navegación: " + e.getMessage());
        }
    }
}
