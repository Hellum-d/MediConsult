package com.mediconsult.beta.controller;

import com.mediconsult.beta.model.Paciente;
import com.mediconsult.beta.model.Usuario;
import com.mediconsult.beta.service.PacienteService;
import com.mediconsult.beta.util.NotificacionUtil;
import com.mediconsult.beta.util.Sesion;
import com.mediconsult.beta.util.TopBarUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import java.util.Arrays;
import java.util.List;

/**
 * Controller de pacientes.fxml — CLINEX
 *
 * <p>Gestiona el directorio de pacientes: listado, búsqueda,
 * panel de detalle lateral y registro/eliminación (solo admin).</p>
 *
 * @author MediConsult Team
 * @version 0.3 — datos médicos completos (tipo de sangre, alergias, descripción)
 */
public class PacientesController {

    /* ── Inyección FXML ─────────────────────────────────────── */

    @FXML private HBox       topBar;
    @FXML private TextField  searchField;

    // Estadísticas
    @FXML private Label statTotalLabel;
    @FXML private Label statActivosLabel;
    @FXML private Label statNuevosLabel;

    // Grid de tarjetas
    @FXML private FlowPane patientGridPane;

    // Panel lateral de detalle
    @FXML private VBox      detailPanel;
    @FXML private Label     detailAvatarLabel;
    @FXML private Label     detailNameLabel;
    @FXML private Label     detailIdLabel;
    @FXML private Label     detailEdadLabel;
    @FXML private Label     detailTelefonoLabel;
    @FXML private Label     detailSangreLabel;   // ya existe en el FXML
    @FXML private VBox      detailContactInfo;
    @FXML private FlowPane  detailAlergias;
    @FXML private TextArea  detailHistorialArea;
    @FXML private Button    closeDetailBtn;

    // FAB y navegación
    @FXML private Button addPatientBtn;
    @FXML private Button verHistorialBtn;
    @FXML private Button navPacientesBtn;
    @FXML private Button navMedicosBtn;
    @FXML private Button navCitasBtn;
    @FXML private Button navDiagnosticoBtn;

    /* ── Servicios ──────────────────────────────────────────── */

    private final PacienteService pacienteService = new PacienteService();

    private List<Paciente> todosPacientes;
    private Paciente       pacienteEnDetalle = null;
    private boolean        esAdmin           = false;

    // Tipos de sangre disponibles
    private static final String[] TIPOS_SANGRE = {
        "", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
    };

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        esAdmin = Sesion.getUsuario() != null
                && Sesion.getUsuario().getRol().equals(Usuario.ROL_ADMIN);

        TopBarUtil.configurar(topBar);
        cargarDatos();

        searchField.textProperty().addListener((obs, oldVal, newVal) ->
                filtrarPacientes(newVal.trim()));

        addPatientBtn.setVisible(esAdmin);
        addPatientBtn.setManaged(esAdmin);

        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
    }

    /* ── Carga de datos ─────────────────────────────────────── */

    private void cargarDatos() {
        try {
            todosPacientes = pacienteService.listarTodos();
        } catch (Exception e) {
            todosPacientes = List.of();
            mostrarError("No se pudo conectar a la base de datos: " + e.getMessage());
        }

        statTotalLabel.setText(String.valueOf(todosPacientes.size()));
        statActivosLabel.setText(String.valueOf(todosPacientes.size()));
        statNuevosLabel.setText("—");

        renderizarGrid(todosPacientes);

        // Solo el admin ve los registros inactivos
        if (esAdmin) {
            try {
                List<Paciente> inactivos = pacienteService.listarInactivos();
                if (!inactivos.isEmpty()) {
                    agregarSeccionInactivos(inactivos);
                }
            } catch (Exception e) {
                // Si falla listarInactivos (ej. columna aún no migrada), ignorar silenciosamente
            }
        }
    }

    private void agregarSeccionInactivos(List<Paciente> inactivos) {
        // Encabezado de sección (ancho completo dentro del FlowPane)
        Label header = new Label("REGISTROS INACTIVOS  (" + inactivos.size() + ")");
        header.setStyle(
            "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#73777f;" +
            "-fx-padding:6 12; -fx-background-color:#f2f3f7;" +
            "-fx-background-radius:8; -fx-pref-width:920;"
        );
        patientGridPane.getChildren().add(header);

        for (Paciente p : inactivos) {
            patientGridPane.getChildren().add(crearTarjetaPacienteInactivo(p));
        }
    }

    private VBox crearTarjetaPacienteInactivo(Paciente p) {
        VBox card = new VBox(12);
        card.getStyleClass().add("item-card");
        card.setPrefWidth(280);
        card.setStyle(card.getStyle() + "-fx-opacity:0.6;");

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane avatarPane = new StackPane();
        avatarPane.getStyleClass().add("avatar-circle");
        avatarPane.setStyle("-fx-background-color:#c3c6cf; -fx-background-radius:50%;");
        String inicial = p.getNombre() != null && !p.getNombre().isEmpty()
                ? String.valueOf(p.getNombre().charAt(0)).toUpperCase() : "?";
        Label avatarLabel = new Label(inicial);
        avatarLabel.getStyleClass().add("avatar-circle-label");
        avatarPane.getChildren().add(avatarLabel);

        VBox datosCol = new VBox(4);
        Label nombreLabel = new Label(p.getNombreCompleto());
        nombreLabel.getStyleClass().add("item-card-name");
        nombreLabel.setStyle("-fx-text-fill:#73777f;");
        Label subLabel = new Label("Inactivo · ID: " + p.getId());
        subLabel.getStyleClass().add("item-card-sub");
        datosCol.getChildren().addAll(nombreLabel, subLabel);
        header.getChildren().addAll(avatarPane, datosCol);

        HBox accionesRow = new HBox(8);
        Button reactivarBtn = new Button("✓ Reactivar");
        reactivarBtn.setStyle(
            "-fx-background-color:#22c55e; -fx-text-fill:white; -fx-font-weight:bold;" +
            "-fx-background-radius:10; -fx-cursor:hand; -fx-padding:7 14;"
        );
        reactivarBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(reactivarBtn, Priority.ALWAYS);
        reactivarBtn.setOnAction(e -> {
            try {
                pacienteService.reactivarPaciente(p.getId());
                cargarDatos();
            } catch (Exception ex) {
                mostrarError("No se pudo reactivar: " + ex.getMessage());
            }
        });
        accionesRow.getChildren().add(reactivarBtn);

        card.getChildren().addAll(header, accionesRow);
        return card;
    }

    private void filtrarPacientes(String query) {
        if (query.isEmpty()) { renderizarGrid(todosPacientes); return; }
        String q = query.toLowerCase();
        List<Paciente> filtrados = todosPacientes.stream()
                .filter(p -> p.getNombreCompleto().toLowerCase().contains(q)
                          || String.valueOf(p.getId()).contains(q)
                          || (p.getTelefono() != null && p.getTelefono().contains(q)))
                .toList();
        renderizarGrid(filtrados);
    }

    /* ── Renderizado de tarjetas ────────────────────────────── */

    private void renderizarGrid(List<Paciente> lista) {
        patientGridPane.getChildren().clear();
        if (lista.isEmpty()) {
            Label vacio = new Label("No se encontraron pacientes.");
            vacio.getStyleClass().add("label-body-lg");
            patientGridPane.getChildren().add(vacio);
            return;
        }
        for (Paciente p : lista) {
            patientGridPane.getChildren().add(crearTarjetaPaciente(p));
        }
    }

    private VBox crearTarjetaPaciente(Paciente p) {
        VBox card = new VBox(12);
        card.getStyleClass().add("item-card");
        card.setPrefWidth(280);

        // ── Avatar + datos básicos ──────────────────────────
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane avatarPane = new StackPane();
        avatarPane.getStyleClass().add("avatar-circle");
        String inicial = p.getNombre() != null && !p.getNombre().isEmpty()
                ? String.valueOf(p.getNombre().charAt(0)).toUpperCase() : "?";
        Label avatarLabel = new Label(inicial);
        avatarLabel.getStyleClass().add("avatar-circle-label");
        avatarPane.getChildren().add(avatarLabel);

        VBox datosCol = new VBox(4);
        HBox nombreRow = new HBox(8);
        nombreRow.setAlignment(Pos.CENTER_LEFT);
        Label nombreLabel = new Label(p.getNombreCompleto());
        nombreLabel.getStyleClass().add("item-card-name");
        nombreRow.getChildren().add(nombreLabel);

        // Chip de tipo de sangre (si existe)
        if (p.getTipoSangre() != null && !p.getTipoSangre().isBlank()) {
            Label chipSangre = new Label(p.getTipoSangre());
            chipSangre.getStyleClass().add("chip-blood-type");
            nombreRow.getChildren().add(chipSangre);
        }

        Label subLabel = new Label(p.getEdad() + " años"
                + (p.getTelefono() != null && !p.getTelefono().isBlank()
                   ? " • " + p.getTelefono() : ""));
        subLabel.getStyleClass().add("item-card-sub");

        datosCol.getChildren().addAll(nombreRow, subLabel);
        header.getChildren().addAll(avatarPane, datosCol);

        // ── Chips de alergias / historial ──────────────────
        FlowPane chips = new FlowPane();
        chips.setHgap(6);
        chips.setVgap(6);

        String alergias = p.getAlergias();
        if (alergias != null && !alergias.isBlank()) {
            // Mostrar cada alergia como chip individual
            for (String alergia : alergias.split(",")) {
                String a = alergia.trim();
                if (!a.isEmpty()) {
                    Label chip = new Label(a);
                    chip.getStyleClass().add("chip-allergy");
                    chips.getChildren().add(chip);
                }
            }
        } else {
            if (p.getHistorial() != null && !p.getHistorial().isBlank()) {
                Label chipHistorial = new Label("Con historial");
                chipHistorial.getStyleClass().add("chip-neutral");
                chips.getChildren().add(chipHistorial);
            } else {
                Label chipVacio = new Label("Sin historial");
                chipVacio.getStyleClass().add("chip-neutral");
                chips.getChildren().add(chipVacio);
            }
        }

        // ── Botones de acción ────────────────────────────────
        HBox accionesRow = new HBox(8);

        Button verBtn = new Button("Ver detalle");
        verBtn.getStyleClass().add("btn-secondary");
        HBox.setHgrow(verBtn, Priority.ALWAYS);
        verBtn.setMaxWidth(Double.MAX_VALUE);
        verBtn.setOnAction(e -> mostrarDetalle(p));

        accionesRow.getChildren().add(verBtn);

        if (esAdmin) {
            Button inactivarBtn = new Button("⊘");
            inactivarBtn.getStyleClass().add("btn-danger");
            inactivarBtn.setPrefWidth(40);
            inactivarBtn.setMinWidth(40);
            inactivarBtn.setTooltip(new Tooltip("Inactivar paciente"));
            inactivarBtn.setOnAction(e -> confirmarYEliminar(p));
            accionesRow.getChildren().add(inactivarBtn);
        }

        card.getChildren().addAll(header, chips, accionesRow);
        return card;
    }

    /* ── Panel de detalle lateral ───────────────────────────── */

    private void mostrarDetalle(Paciente p) {
        pacienteEnDetalle = p;

        String inicial = p.getNombre() != null && !p.getNombre().isEmpty()
                ? String.valueOf(p.getNombre().charAt(0)).toUpperCase() : "?";
        detailAvatarLabel.setText(inicial);
        detailNameLabel.setText(p.getNombreCompleto());
        detailIdLabel.setText("ID: " + p.getId());
        detailEdadLabel.setText(p.getEdad() + " años");
        detailTelefonoLabel.setText(
                p.getTelefono() != null && !p.getTelefono().isBlank()
                        ? p.getTelefono() : "—");

        // ── Tipo de sangre ──────────────────────────────────
        String ts = p.getTipoSangre();
        if (detailSangreLabel != null) {
            if (ts != null && !ts.isBlank()) {
                detailSangreLabel.setText(ts);
                detailSangreLabel.setStyle(
                    "-fx-background-color:#d5e4f8; -fx-background-radius:8;" +
                    "-fx-padding:2 10; -fx-font-weight:bold; -fx-font-size:13px;" +
                    "-fx-text-fill:#0b1c30;"
                );
            } else {
                detailSangreLabel.setText("No registrado");
                detailSangreLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#73777f;");
            }
        }

        // ── Alergias ────────────────────────────────────────
        detailAlergias.getChildren().clear();
        String alergias = p.getAlergias();
        if (alergias != null && !alergias.isBlank()) {
            for (String alergia : alergias.split(",")) {
                String a = alergia.trim();
                if (!a.isEmpty()) {
                    Label chip = new Label(a);
                    chip.getStyleClass().add("chip-allergy");
                    detailAlergias.getChildren().add(chip);
                }
            }
        } else {
            Label sinAlerg = new Label("No registradas");
            sinAlerg.getStyleClass().add("chip-neutral");
            detailAlergias.getChildren().add(sinAlerg);
        }

        // ── Historial ───────────────────────────────────────
        String hist = p.getHistorial();
        detailHistorialArea.setText(
                hist != null && !hist.isBlank() ? hist : "Sin historial registrado.");

        detailPanel.setVisible(true);
        detailPanel.setManaged(true);
    }

    @FXML
    public void onCloseDetail(ActionEvent event) {
        pacienteEnDetalle = null;
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
    }

    /* ── Eliminación con confirmación ───────────────────────── */

    private void confirmarYEliminar(Paciente paciente) {
        boolean existeEnLista = todosPacientes.stream()
                .anyMatch(p -> p.getId() == paciente.getId());
        if (!existeEnLista) {
            mostrarError("El paciente ya no existe en el sistema.");
            cargarDatos();
            return;
        }

        Stage ownerStage = (Stage) searchField.getScene().getWindow();
        Stage confirmStage = new Stage();
        confirmStage.initOwner(ownerStage);
        confirmStage.initModality(Modality.WINDOW_MODAL);
        confirmStage.initStyle(StageStyle.TRANSPARENT);
        confirmStage.setResizable(false);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,36,68,0.55);");

        VBox confirmCard = new VBox(0);
        confirmCard.setMaxWidth(440);
        confirmCard.setMinWidth(440);
        confirmCard.setStyle(
            "-fx-background-color:#ffffff;" +
            "-fx-background-radius:20;" +
            "-fx-border-color:rgba(186,26,26,0.3);" +
            "-fx-border-radius:20;" +
            "-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.25),32,0,0,8);"
        );

        HBox headerBox = new HBox(14);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setStyle(
            "-fx-background-color:#fff0f0; -fx-background-radius:20 20 0 0;" +
            "-fx-padding:22 24 18 24;" +
            "-fx-border-color:transparent transparent rgba(186,26,26,0.2) transparent;" +
            "-fx-border-width:0 0 1 0;"
        );

        StackPane iconBox = new StackPane();
        iconBox.setMinSize(44,44); iconBox.setMaxSize(44,44);
        iconBox.setStyle("-fx-background-color:#ffdad6; -fx-background-radius:12;");
        Label iconLabel = new Label("⚠");
        iconLabel.setStyle("-fx-font-size:20px;");
        iconBox.getChildren().add(iconLabel);

        VBox headerTexts = new VBox(3);
        HBox.setHgrow(headerTexts, Priority.ALWAYS);
        Label confirmTitle = new Label("Inactivar paciente");
        confirmTitle.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#ba1a1a;");
        Label confirmSub = new Label("El registro se conserva en la base de datos");
        confirmSub.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f;");
        headerTexts.getChildren().addAll(confirmTitle, confirmSub);
        headerBox.getChildren().addAll(iconBox, headerTexts);

        VBox bodyBox = new VBox(12);
        bodyBox.setStyle("-fx-padding:24 24 20 24;");

        Label msgLabel = new Label("¿Estás seguro de que deseas inactivar al paciente:");
        msgLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#444950; -fx-wrap-text:true;");

        HBox pacienteBox = new HBox(10);
        pacienteBox.setAlignment(Pos.CENTER_LEFT);
        pacienteBox.setStyle(
            "-fx-background-color:#f8f9ff; -fx-background-radius:10;" +
            "-fx-padding:12 16; -fx-border-color:rgba(46,134,222,0.15);" +
            "-fx-border-radius:10; -fx-border-width:1;"
        );
        StackPane miniAvatar = new StackPane();
        miniAvatar.setMinSize(36,36); miniAvatar.setMaxSize(36,36);
        miniAvatar.setStyle("-fx-background-color:#2e86de; -fx-background-radius:50%;");
        String ini = paciente.getNombre() != null && !paciente.getNombre().isEmpty()
                ? String.valueOf(paciente.getNombre().charAt(0)).toUpperCase() : "?";
        Label miniIni = new Label(ini);
        miniIni.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:white;");
        miniAvatar.getChildren().add(miniIni);
        VBox pInfo = new VBox(2);
        Label pNombre = new Label(paciente.getNombreCompleto());
        pNombre.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#002444;");
        Label pId = new Label("ID: " + paciente.getId() + " · " + paciente.getEdad() + " años");
        pId.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f;");
        pInfo.getChildren().addAll(pNombre, pId);
        pacienteBox.getChildren().addAll(miniAvatar, pInfo);

        Label warnLabel = new Label("El paciente quedará inactivo. Sus datos e historial clínico se conservan. Solo el administrador podrá reactivarlo.");
        warnLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#b45309; -fx-wrap-text:true;" +
            "-fx-padding:8 12; -fx-background-color:#fffbeb; -fx-background-radius:8;");
        warnLabel.setMaxWidth(Double.MAX_VALUE);
        bodyBox.getChildren().addAll(msgLabel, pacienteBox, warnLabel);

        HBox footerBox = new HBox(10);
        footerBox.setAlignment(Pos.CENTER_RIGHT);
        footerBox.setStyle(
            "-fx-padding:16 24 20 24;" +
            "-fx-border-color:rgba(195,198,207,0.4) transparent transparent transparent;" +
            "-fx-border-width:1 0 0 0;" +
            "-fx-background-color:#f8f9ff; -fx-background-radius:0 0 20 20;"
        );

        Button cancelarBtn = new Button("Cancelar");
        cancelarBtn.getStyleClass().add("btn-secondary");
        cancelarBtn.setPrefWidth(110);
        cancelarBtn.setOnAction(e -> confirmStage.close());

        Button eliminarBtn = new Button("⊘  Inactivar");
        eliminarBtn.setPrefWidth(130);
        String styleRed = "-fx-background-color:#ba1a1a; -fx-text-fill:white;" +
            "-fx-background-radius:10; -fx-font-size:13px; -fx-font-weight:bold;" +
            "-fx-cursor:hand; -fx-padding:9 16;";
        String styleRedHover = "-fx-background-color:#93000a; -fx-text-fill:white;" +
            "-fx-background-radius:10; -fx-font-size:13px; -fx-font-weight:bold;" +
            "-fx-cursor:hand; -fx-padding:9 16;";
        eliminarBtn.setStyle(styleRed);
        eliminarBtn.setOnMouseEntered(ev -> eliminarBtn.setStyle(styleRedHover));
        eliminarBtn.setOnMouseExited(ev  -> eliminarBtn.setStyle(styleRed));
        eliminarBtn.setOnAction(ev -> {
            try {
                pacienteService.eliminarPaciente(paciente.getId());
                confirmStage.close();
                if (pacienteEnDetalle != null && pacienteEnDetalle.getId() == paciente.getId()) {
                    pacienteEnDetalle = null;
                    detailPanel.setVisible(false);
                    detailPanel.setManaged(false);
                }
                cargarDatos();
                Stage st = (Stage) searchField.getScene().getWindow();
                NotificacionUtil.toast(st, NotificacionUtil.Tipo.ADVERTENCIA,
                        "Paciente " + paciente.getNombreCompleto() + " inactivado.");
            } catch (Exception ex) {
                confirmStage.close();
                mostrarError("No se pudo inactivar el paciente: " + ex.getMessage());
            }
        });

        footerBox.getChildren().addAll(cancelarBtn, eliminarBtn);
        confirmCard.getChildren().addAll(headerBox, bodyBox, footerBox);
        StackPane.setAlignment(confirmCard, Pos.CENTER);
        overlay.getChildren().add(confirmCard);

        Scene confirmScene = new Scene(overlay, ownerStage.getScene().getWidth(),
                ownerStage.getScene().getHeight());
        confirmScene.setFill(Color.TRANSPARENT);
        confirmScene.getStylesheets().add(
            getClass().getResource("/com/mediconsult/beta/view/fxml/styles.css").toExternalForm());
        confirmStage.setScene(confirmScene);
        confirmStage.showAndWait();
    }

    /* ══════════════════════════════════════════════════════════
       FAB: Nuevo paciente — Modal CLINEX con datos médicos
       ══════════════════════════════════════════════════════════ */

    @FXML
    public void onAddPatient(ActionEvent event) {
        Stage ownerStage = (Stage) searchField.getScene().getWindow();

        Stage modalStage = new Stage();
        modalStage.initOwner(ownerStage);
        modalStage.initModality(Modality.WINDOW_MODAL);
        modalStage.initStyle(StageStyle.TRANSPARENT);
        modalStage.setResizable(false);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,36,68,0.55);");

        VBox modalCard = new VBox(0);
        modalCard.setMaxWidth(560);
        modalCard.setMinWidth(560);
        modalCard.setStyle(
            "-fx-background-color:#ffffff; -fx-background-radius:20;" +
            "-fx-border-color:rgba(46,134,222,0.2); -fx-border-radius:20;" +
            "-fx-border-width:1; -fx-padding:0;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.25),32,0,0,8);"
        );

        // ─── HEADER ─────────────────────────────────────────────────────
        HBox headerBox = new HBox(16);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setStyle(
            "-fx-background-color:#f8f9ff; -fx-background-radius:20 20 0 0;" +
            "-fx-padding:24 28 20 28;" +
            "-fx-border-color:transparent transparent rgba(195,198,207,0.4) transparent;" +
            "-fx-border-width:0 0 1 0;"
        );

        StackPane iconBox = new StackPane();
        iconBox.setMinSize(48,48); iconBox.setMaxSize(48,48);
        iconBox.setStyle("-fx-background-color:#eff4ff; -fx-background-radius:14;");
        Label iconLabel = new Label("👤");
        iconLabel.setStyle("-fx-font-size:22px;");
        iconBox.getChildren().add(iconLabel);

        VBox headerTexts = new VBox(4);
        HBox.setHgrow(headerTexts, Priority.ALWAYS);
        Label modalTitle = new Label("Nuevo Paciente");
        modalTitle.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#002444;");
        Label modalSubtitle = new Label("Complete los datos del paciente a registrar");
        modalSubtitle.setStyle("-fx-font-size:13px; -fx-text-fill:#73777f;");
        headerTexts.getChildren().addAll(modalTitle, modalSubtitle);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#73777f;" +
            "-fx-font-size:16px; -fx-cursor:hand; -fx-padding:4 8; -fx-background-radius:8;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color:#f2f3f7; -fx-text-fill:#0b1c30;" +
            "-fx-font-size:16px; -fx-cursor:hand; -fx-padding:4 8; -fx-background-radius:8;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#73777f;" +
            "-fx-font-size:16px; -fx-cursor:hand; -fx-padding:4 8; -fx-background-radius:8;"));
        closeBtn.setOnAction(e -> modalStage.close());
        headerBox.getChildren().addAll(iconBox, headerTexts, closeBtn);

        // ─── SCROLL CONTAINER (por si el contenido es largo) ────────────
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color:transparent; -fx-background:transparent;" +
                            "-fx-border-color:transparent; -fx-max-height:480;");

        VBox formBody = new VBox(20);
        formBody.setStyle("-fx-padding:28 28 24 28; -fx-background-color:white;");

        // ══ Sección 1: Información personal ══════════════════════════════
        Label sec1 = new Label("INFORMACIÓN PERSONAL");
        sec1.getStyleClass().add("form-section-title");

        HBox nombreApellidoRow = new HBox(16);

        VBox nombreBox = new VBox(6);
        HBox.setHgrow(nombreBox, Priority.ALWAYS);
        HBox nombreLabelRow = new HBox(2);
        Label lblNombre = new Label("Nombre");
        lblNombre.getStyleClass().add("form-label");
        Label nombreStar = new Label("*");
        nombreStar.getStyleClass().add("required-star");
        nombreLabelRow.getChildren().addAll(lblNombre, nombreStar);
        TextField nombreField = new TextField();
        nombreField.setPromptText("Ej: María");
        nombreField.getStyleClass().add("form-field");
        nombreField.setMaxWidth(Double.MAX_VALUE);
        nombreBox.getChildren().addAll(nombreLabelRow, nombreField);

        VBox apellidoBox = new VBox(6);
        HBox.setHgrow(apellidoBox, Priority.ALWAYS);
        HBox apellidoLabelRow = new HBox(2);
        Label lblApellido = new Label("Apellido");
        lblApellido.getStyleClass().add("form-label");
        Label apellidoStar = new Label("*");
        apellidoStar.getStyleClass().add("required-star");
        apellidoLabelRow.getChildren().addAll(lblApellido, apellidoStar);
        TextField apellidoField = new TextField();
        apellidoField.setPromptText("Ej: López Pérez");
        apellidoField.getStyleClass().add("form-field");
        apellidoField.setMaxWidth(Double.MAX_VALUE);
        apellidoBox.getChildren().addAll(apellidoLabelRow, apellidoField);

        nombreApellidoRow.getChildren().addAll(nombreBox, apellidoBox);

        VBox edadBox = new VBox(6);
        HBox edadLabelRow = new HBox(2);
        Label lblEdad = new Label("Edad");
        lblEdad.getStyleClass().add("form-label");
        Label edadStar = new Label("*");
        edadStar.getStyleClass().add("required-star");
        edadLabelRow.getChildren().addAll(lblEdad, edadStar);
        TextField edadField = new TextField();
        edadField.setPromptText("Ej: 35");
        edadField.getStyleClass().add("form-field");
        edadField.setMaxWidth(Double.MAX_VALUE);
        edadField.textProperty().addListener((obs, oldVal, newVal) -> {
            String solo = newVal.replaceAll("[^0-9]", "");
            if (solo.length() > 3) solo = solo.substring(0, 3);
            if (!newVal.equals(solo)) edadField.setText(solo);
        });
        edadBox.getChildren().addAll(edadLabelRow, edadField);

        Region div1 = new Region();
        div1.getStyleClass().add("form-divider");

        // ══ Sección 2: Contacto ═══════════════════════════════════════════
        Label sec2 = new Label("INFORMACIÓN DE CONTACTO  (opcional)");
        sec2.getStyleClass().add("form-section-title");

        HBox contactRow = new HBox(16);

        VBox telefonoBox = new VBox(6);
        HBox.setHgrow(telefonoBox, Priority.ALWAYS);
        Label telContador = new Label("0 / 10");
        telContador.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f;");
        HBox telHeaderRow = new HBox(8);
        telHeaderRow.setAlignment(Pos.CENTER_LEFT);
        Label lblTel = new Label("Teléfono");
        lblTel.getStyleClass().add("form-label");
        telHeaderRow.getChildren().addAll(lblTel, telContador);
        TextField telField = new TextField();
        telField.setPromptText("Ej: 3001234567");
        telField.getStyleClass().add("form-field");
        telField.setMaxWidth(Double.MAX_VALUE);
        Label telHint = new Label("Solo dígitos · máximo 10 caracteres");
        telHint.getStyleClass().add("field-hint");

        telField.textProperty().addListener((obs, oldVal, newVal) -> {
            String solo = newVal.replaceAll("[^0-9]", "");
            if (solo.length() > 10) solo = solo.substring(0, 10);
            if (!newVal.equals(solo)) { telField.setText(solo); return; }
            int len = solo.length();
            telContador.setText(len + " / 10");
            if (len == 0) telContador.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f;");
            else if (len < 7) telContador.setStyle("-fx-font-size:11px; -fx-text-fill:#e07b00; -fx-font-weight:bold;");
            else if (len == 10) telContador.setStyle("-fx-font-size:11px; -fx-text-fill:#1a6b3c; -fx-font-weight:bold;");
            else telContador.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f;");
        });

        telefonoBox.getChildren().addAll(telHeaderRow, telField, telHint);

        VBox correoBox = new VBox(6);
        HBox.setHgrow(correoBox, Priority.ALWAYS);
        Label lblCorreo = new Label("Correo electrónico");
        lblCorreo.getStyleClass().add("form-label");
        TextField correoField = new TextField();
        correoField.setPromptText("paciente@correo.com");
        correoField.getStyleClass().add("form-field");
        correoField.setMaxWidth(Double.MAX_VALUE);
        correoBox.getChildren().addAll(lblCorreo, correoField);

        contactRow.getChildren().addAll(telefonoBox, correoBox);

        Region div2 = new Region();
        div2.getStyleClass().add("form-divider");

        // ══ Sección 3: Datos Médicos (NUEVA) ════════════════════════════
        Label sec3 = new Label("DATOS MÉDICOS  (opcional)");
        sec3.getStyleClass().add("form-section-title");

        HBox datosMedicosRow = new HBox(16);

        // ── ComboBox Tipo de Sangre ──────────────────────────────────────
        VBox sangreBox = new VBox(6);
        HBox.setHgrow(sangreBox, Priority.ALWAYS);
        Label lblSangre = new Label("Tipo de Sangre");
        lblSangre.getStyleClass().add("form-label");

        ComboBox<String> sangreCombo = new ComboBox<>();
        sangreCombo.getItems().addAll(TIPOS_SANGRE);
        sangreCombo.getSelectionModel().selectFirst(); // selecciona "" (vacío)
        sangreCombo.setPromptText("Seleccionar...");
        sangreCombo.setMaxWidth(Double.MAX_VALUE);
        sangreCombo.setStyle(
            "-fx-background-color:#f8f9ff; -fx-background-radius:10;" +
            "-fx-border-color:rgba(195,198,207,0.6); -fx-border-radius:10;" +
            "-fx-border-width:1.5; -fx-font-size:13px; -fx-text-fill:#0b1c30;" +
            "-fx-padding:8 12; -fx-cursor:hand;"
        );
        sangreBox.getChildren().addAll(lblSangre, sangreCombo);

        // ── TextField Alergias ───────────────────────────────────────────
        VBox alergiasBox = new VBox(6);
        HBox.setHgrow(alergiasBox, Priority.ALWAYS);
        Label lblAlergias = new Label("Alergias conocidas");
        lblAlergias.getStyleClass().add("form-label");
        TextField alergiasField = new TextField();
        alergiasField.setPromptText("Ej: Penicilina, Polen, Látex");
        alergiasField.getStyleClass().add("form-field");
        alergiasField.setMaxWidth(Double.MAX_VALUE);
        Label alergiasHint = new Label("Separar con comas si son varias");
        alergiasHint.getStyleClass().add("field-hint");
        alergiasBox.getChildren().addAll(lblAlergias, alergiasField, alergiasHint);

        datosMedicosRow.getChildren().addAll(sangreBox, alergiasBox);

        // ── Nota informativa ─────────────────────────────────────────────
        Label notaMedica = new Label(
            "💡  Esta información se mostrará en la tarjeta y panel de detalle del paciente."
        );
        notaMedica.setStyle(
            "-fx-font-size:11px; -fx-text-fill:#2e86de;" +
            "-fx-padding:6 12; -fx-background-color:#eff4ff;" +
            "-fx-background-radius:8; -fx-wrap-text:true;"
        );
        notaMedica.setMaxWidth(Double.MAX_VALUE);

        // ── Nota de campos obligatorios ──────────────────────────────────
        HBox requiredNote = new HBox(4);
        requiredNote.setAlignment(Pos.CENTER_LEFT);
        Label starNote = new Label("*");
        starNote.getStyleClass().add("required-star");
        Label noteText = new Label("Campos obligatorios");
        noteText.getStyleClass().add("field-hint");
        requiredNote.getChildren().addAll(starNote, noteText);

        // ── Label de error ───────────────────────────────────────────────
        Label errorLabel = new Label();
        errorLabel.setStyle(
            "-fx-text-fill:#ba1a1a; -fx-font-size:12px;" +
            "-fx-padding:6 12; -fx-background-color:#ffdad6;" +
            "-fx-background-radius:8; -fx-wrap-text:true;"
        );
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setMaxWidth(Double.MAX_VALUE);

        formBody.getChildren().addAll(
            sec1, nombreApellidoRow, edadBox,
            div1,
            sec2, contactRow,
            div2,
            sec3, datosMedicosRow, notaMedica,
            requiredNote, errorLabel
        );

        scrollPane.setContent(formBody);

        // ─── FOOTER ────────────────────────────────────────────────────
        HBox footerBox = new HBox(12);
        footerBox.setAlignment(Pos.CENTER_RIGHT);
        footerBox.setStyle(
            "-fx-padding:20 28 24 28;" +
            "-fx-border-color:rgba(195,198,207,0.4) transparent transparent transparent;" +
            "-fx-border-width:1 0 0 0;" +
            "-fx-background-color:#f8f9ff; -fx-background-radius:0 0 20 20;"
        );

        Button cancelBtn = new Button("Cancelar");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setPrefWidth(120);
        cancelBtn.setOnAction(e -> modalStage.close());

        Button registrarBtn = new Button("✚  Registrar Paciente");
        registrarBtn.getStyleClass().add("btn-primary");
        registrarBtn.setPrefWidth(190);
        footerBox.getChildren().addAll(cancelBtn, registrarBtn);

        // ── Limpiar errores al escribir ──────────────────────────────────
        nombreField.textProperty().addListener((obs,o,n)  -> { if(!n.trim().isEmpty()) nombreField.getStyleClass().remove("form-field-error"); });
        apellidoField.textProperty().addListener((obs,o,n)-> { if(!n.trim().isEmpty()) apellidoField.getStyleClass().remove("form-field-error"); });
        edadField.textProperty().addListener((obs,o,n)    -> { if(!n.trim().isEmpty()) edadField.getStyleClass().remove("form-field-error"); });

        // ── Acción Registrar ─────────────────────────────────────────────
        registrarBtn.setOnAction(ev -> {
            String nombre   = nombreField.getText().trim();
            String apellido = apellidoField.getText().trim();
            String edadStr  = edadField.getText().trim();
            String telefono = telField.getText().trim();
            String correo   = correoField.getText().trim();
            String sangre   = sangreCombo.getValue() != null ? sangreCombo.getValue().trim() : "";
            String alergias = alergiasField.getText().trim();

            nombreField.getStyleClass().remove("form-field-error");
            apellidoField.getStyleClass().remove("form-field-error");
            edadField.getStyleClass().remove("form-field-error");
            telField.getStyleClass().remove("form-field-error");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);

            boolean valido = true;
            StringBuilder msg = new StringBuilder("⚠  ");

            if (nombre.isEmpty())   { nombreField.getStyleClass().add("form-field-error");   valido = false; }
            if (apellido.isEmpty()) { apellidoField.getStyleClass().add("form-field-error"); valido = false; }

            int edad = 0;
            if (edadStr.isEmpty()) {
                edadField.getStyleClass().add("form-field-error"); valido = false;
            } else {
                try {
                    edad = Integer.parseInt(edadStr);
                    if (edad <= 0 || edad > 130) {
                        edadField.getStyleClass().add("form-field-error");
                        msg.append("La edad debe estar entre 1 y 130. ");
                        valido = false;
                    }
                } catch (NumberFormatException ex) {
                    edadField.getStyleClass().add("form-field-error");
                    msg.append("La edad debe ser un número válido. ");
                    valido = false;
                }
            }

            if (!telefono.isEmpty() && telefono.length() < 7) {
                telField.getStyleClass().add("form-field-error");
                msg.append("El teléfono debe tener entre 7 y 10 dígitos. ");
                valido = false;
            }

            if (!valido) {
                String msgStr = msg.toString().equals("⚠  ")
                        ? "⚠  Complete todos los campos obligatorios marcados con *"
                        : msg.toString().trim();
                errorLabel.setText(msgStr);
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            try {
                String nombreCompleto = nombre + " " + apellido;
                pacienteService.registrarPaciente(nombreCompleto, edad, telefono, sangre, alergias);
                modalStage.close();
                cargarDatos();
            } catch (IllegalArgumentException e) {
                errorLabel.setText("⚠  " + e.getMessage());
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });

        modalCard.getChildren().addAll(headerBox, scrollPane, footerBox);
        StackPane.setAlignment(modalCard, Pos.CENTER);
        overlay.getChildren().add(modalCard);
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) modalStage.close(); });

        Scene modalScene = new Scene(overlay,
                ownerStage.getScene().getWidth(),
                ownerStage.getScene().getHeight());
        modalScene.setFill(Color.TRANSPARENT);
        modalScene.getStylesheets().add(
            getClass().getResource("/com/mediconsult/beta/view/fxml/styles.css").toExternalForm());

        modalStage.setScene(modalScene);
        Platform.runLater(nombreField::requestFocus);
        modalStage.showAndWait();
    }

    /* ── Navegación ─────────────────────────────────────────── */

    @FXML
    public void onVerHistorialCompleto(ActionEvent event) {
        if (pacienteEnDetalle == null) return;
        HistorialClinicoController.setPacienteIdExterno(pacienteEnDetalle.getId());
        HistorialClinicoController.setRetornarA("/com/mediconsult/beta/view/fxml/pacientes.fxml");
        navegarA("/com/mediconsult/beta/view/fxml/historial_clinico.fxml", "CLINEX — Historial Clínico");
    }

    @FXML public void onNavPacientes(ActionEvent e)   { /* ya estamos aquí */ }
    @FXML public void onNavMedicos(ActionEvent e)     { navegarA("/com/mediconsult/beta/view/fxml/medicos.fxml",     "CLINEX — Médicos"); }
    @FXML public void onNavCitas(ActionEvent e)       { navegarA("/com/mediconsult/beta/view/fxml/citas.fxml",       "CLINEX — Citas"); }
    @FXML public void onNavDiagnostico(ActionEvent e) { navegarA("/com/mediconsult/beta/view/fxml/diagnostico.fxml", "CLINEX — Diagnóstico IA"); }
    @FXML public void onNavRemisiones(ActionEvent e)   { navegarA("/com/mediconsult/beta/view/fxml/gestion_remisiones.fxml", "CLINEX — Remisiones"); }
    @FXML public void onNavListaEspera(ActionEvent e)  { navegarA("/com/mediconsult/beta/view/fxml/lista_espera.fxml",        "CLINEX — Lista de Espera"); }
    @FXML public void onNotificaciones(ActionEvent e) { navegarA("/com/mediconsult/beta/view/fxml/notificaciones_admin.fxml", "CLINEX — Notificaciones"); }

    /* ── Utilidades ─────────────────────────────────────────── */

    private void navegarA(String fxmlPath, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(titulo);
        } catch (IOException ex) {
            mostrarError("No se pudo cargar la pantalla: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void mostrarError(String msg) {
        Stage st = (Stage) searchField.getScene().getWindow();
        NotificacionUtil.alerta(st, NotificacionUtil.Tipo.ERROR, "Error — CLINEX", msg);
    }
}