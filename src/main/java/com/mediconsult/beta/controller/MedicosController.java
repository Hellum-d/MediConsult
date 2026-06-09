package com.mediconsult.beta.controller;

import com.mediconsult.beta.model.Medico;
import com.mediconsult.beta.model.Usuario;
import com.mediconsult.beta.service.MedicoService;
import com.mediconsult.beta.service.UsuarioService;
import com.mediconsult.beta.util.NotificacionUtil;
import com.mediconsult.beta.util.Sesion;
import com.mediconsult.beta.util.TopBarUtil;
import javafx.application.Platform;
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
import javafx.geometry.Bounds;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.List;

/**
 * Controller de medicos.fxml — CLINEX
 *
 * <p>Gestiona el directorio médico: listado, búsqueda por especialidad,
 * cambio de disponibilidad y registro/eliminación (solo admin).</p>
 *
 * @author MediConsult Team
 */
public class MedicosController {

    /* ── Inyección FXML ─────────────────────────────────────── */

    @FXML private HBox  topBar;
    @FXML private TextField searchMedicoField;

    // Estadísticas
    @FXML private Label statTotalLabel;
    @FXML private Label statGuardiaLabel;
    @FXML private Label statConsultaLabel;
    @FXML private Label statUrgenciasLabel;

    // Lista de médicos
    @FXML private VBox medicosListView;

    // FAB y navegación
    @FXML private Button addMedicoBtn;
    @FXML private Button navPacientesBtn;
    @FXML private Button navMedicosBtn;
    @FXML private Button navCitasBtn;
    @FXML private Button navDiagnosticoBtn;

    /* ── Servicios ──────────────────────────────────────────── */

    private final MedicoService  medicoService  = new MedicoService();
    private final UsuarioService usuarioService = new UsuarioService();
    private List<Medico> todosMedicos;

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        TopBarUtil.configurar(topBar);
        cargarDatos();

        // Filtrado en tiempo real por nombre o especialidad
        searchMedicoField.textProperty().addListener((obs, o, n) ->
                filtrarMedicos(n.trim()));

        // Ocultar FAB si no es admin
        boolean esAdmin = Sesion.getUsuario() != null
                && Sesion.getUsuario().getRol().equals(Usuario.ROL_ADMIN);
        addMedicoBtn.setVisible(esAdmin);
        addMedicoBtn.setManaged(esAdmin);
    }

    /* ── Carga de datos ─────────────────────────────────────── */

    private void cargarDatos() {
        try {
            todosMedicos = medicoService.listarTodos();
        } catch (Exception e) {
            todosMedicos = List.of();
            mostrarError("No se pudo conectar a la base de datos: " + e.getMessage());
        }

        long disponibles  = todosMedicos.stream()
                .filter(m -> Medico.ESTADO_DISPONIBLE.equals(m.getEstado())).count();
        long enConsulta   = todosMedicos.stream()
                .filter(m -> Medico.ESTADO_EN_CONSULTA.equals(m.getEstado())).count();
        long emergencias  = todosMedicos.stream()
                .filter(m -> Medico.ESTADO_EMERGENCIA.equals(m.getEstado())).count();

        statTotalLabel.setText(String.valueOf(todosMedicos.size()));
        statGuardiaLabel.setText(String.valueOf(disponibles));
        statConsultaLabel.setText(String.valueOf(enConsulta));
        statUrgenciasLabel.setText(String.valueOf(emergencias));

        renderizarLista(todosMedicos);

        // Solo el admin ve los médicos inactivos
        boolean esAdmin = Sesion.getUsuario() != null
                && Sesion.getUsuario().getRol().equals(Usuario.ROL_ADMIN);
        if (esAdmin) {
            try {
                List<Medico> inactivos = medicoService.listarInactivos();
                if (!inactivos.isEmpty()) {
                    agregarSeccionInactivos(inactivos);
                }
            } catch (Exception e) {
                // Si falla (ej. columna aún no migrada), ignorar silenciosamente
            }
        }
    }

    private void agregarSeccionInactivos(List<Medico> inactivos) {
        Label header = new Label("MÉDICOS INACTIVOS  (" + inactivos.size() + ")");
        header.setStyle(
            "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#73777f;" +
            "-fx-padding:6 12; -fx-background-color:#f2f3f7; -fx-background-radius:8;"
        );
        medicosListView.getChildren().add(header);

        for (Medico m : inactivos) {
            HBox card = new HBox(16);
            card.getStyleClass().add("item-card");
            card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            card.setStyle("-fx-opacity:0.6;");

            StackPane avatarPane = new StackPane();
            avatarPane.setStyle(
                "-fx-background-color:#c3c6cf; -fx-background-radius:28; -fx-alignment:center;" +
                "-fx-min-width:56; -fx-min-height:56; -fx-max-width:56; -fx-max-height:56;"
            );
            String inicial = m.getNombre() != null && !m.getNombre().isEmpty()
                    ? String.valueOf(m.getNombre().charAt(0)).toUpperCase() : "M";
            Label avatarLabel = new Label(inicial);
            avatarLabel.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:white;");
            avatarPane.getChildren().add(avatarLabel);

            VBox datosCol = new VBox(4);
            HBox.setHgrow(datosCol, javafx.scene.layout.Priority.ALWAYS);
            Label nombreLabel = new Label("Dr(a). " + m.getNombreCompleto());
            nombreLabel.setStyle("-fx-font-size:17px; -fx-text-fill:#73777f;");
            Label subLabel = new Label("Inactivo · " + m.getEspecialidadDisplay());
            subLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#9ca3af;");
            datosCol.getChildren().addAll(nombreLabel, subLabel);

            Button reactivarBtn = new Button("✓ Reactivar");
            reactivarBtn.setStyle(
                "-fx-background-color:#22c55e; -fx-text-fill:white; -fx-font-weight:bold;" +
                "-fx-background-radius:10; -fx-cursor:hand; -fx-padding:7 14;"
            );
            reactivarBtn.setOnAction(e -> {
                try {
                    medicoService.reactivarMedico(m.getId());
                    cargarDatos();
                } catch (Exception ex) {
                    mostrarError("No se pudo reactivar: " + ex.getMessage());
                }
            });

            card.getChildren().addAll(avatarPane, datosCol, reactivarBtn);
            medicosListView.getChildren().add(card);
        }
    }

    private void filtrarMedicos(String query) {
        if (query.isEmpty()) {
            renderizarLista(todosMedicos);
            return;
        }
        String q = query.toLowerCase();
        List<Medico> filtrados = todosMedicos.stream()
                .filter(m -> m.getNombreCompleto().toLowerCase().contains(q)
                          || m.getEspecialidadDisplay().toLowerCase().contains(q))
                .toList();
        renderizarLista(filtrados);
    }

    /* ── Renderizado de tarjetas ────────────────────────────── */

    private void renderizarLista(List<Medico> lista) {
        medicosListView.getChildren().clear();

        if (lista.isEmpty()) {
            Label vacio = new Label("No se encontraron médicos.");
            vacio.getStyleClass().add("label-body-lg");
            medicosListView.getChildren().add(vacio);
            return;
        }

        for (Medico m : lista) {
            medicosListView.getChildren().add(crearTarjetaMedico(m));
        }
    }

    /** Construye una tarjeta de médico al estilo Stitch con borde de estado. */
    private HBox crearTarjetaMedico(Medico m) {
        HBox card = new HBox(16);
        card.getStyleClass().add("item-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // ── Avatar inicial ───────────────────────────────────
        StackPane avatarPane = new StackPane();
        String inicial = m.getNombre() != null && !m.getNombre().isEmpty()
                ? String.valueOf(m.getNombre().charAt(0)).toUpperCase() : "M";

        String estado = m.getEstado() != null ? m.getEstado() : Medico.ESTADO_DISPONIBLE;
        String avatarBg = switch (estado) {
            case Medico.ESTADO_DISPONIBLE   -> "#22c55e";
            case Medico.ESTADO_EN_CONSULTA  -> "#1a3a5c";
            case Medico.ESTADO_DESCANSANDO  -> "#54a3fd";
            case Medico.ESTADO_FUERA_TURNO  -> "#c3c6cf";
            case Medico.ESTADO_EMERGENCIA   -> "#ba1a1a";
            default                         -> "#c3c6cf";
        };
        avatarPane.setStyle("-fx-background-color:" + avatarBg
                + "; -fx-background-radius:28; -fx-alignment:center;"
                + " -fx-min-width:56; -fx-min-height:56; -fx-max-width:56; -fx-max-height:56;");
        Label avatarLabel = new Label(inicial);
        avatarLabel.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:white;");
        avatarPane.getChildren().add(avatarLabel);

        // ── Datos del médico ─────────────────────────────────
        VBox datosCol = new VBox(6);
        HBox.setHgrow(datosCol, Priority.ALWAYS);

        Label nombreLabel = new Label("Dr(a). " + m.getNombreCompleto());
        nombreLabel.getStyleClass().add("item-card-name");
        nombreLabel.setStyle("-fx-font-size:17px;");

        HBox subRow = new HBox(12);
        subRow.setAlignment(Pos.CENTER_LEFT);

        Label espChip = new Label(m.getEspecialidadDisplay().toUpperCase());
        if (m.isEspecialista()) {
            espChip.getStyleClass().add("chip-neutral");
        } else {
            espChip.setStyle("-fx-background-color:#EEF2FF; -fx-background-radius:10; " +
                             "-fx-padding:3 10; -fx-font-size:11px; -fx-font-weight:bold; " +
                             "-fx-text-fill:#4338CA;");
        }

        String ubicStr = switch (estado) {
            case Medico.ESTADO_DISPONIBLE   -> "📍 Disponible para citas";
            case Medico.ESTADO_EN_CONSULTA  -> "🏥 En consulta";
            case Medico.ESTADO_DESCANSANDO  -> "☕ Descansando";
            case Medico.ESTADO_FUERA_TURNO  -> "🚪 Fuera de turno";
            case Medico.ESTADO_EMERGENCIA   -> "🚨 Atendiendo emergencia";
            default                         -> "📍 Estado desconocido";
        };
        Label ubicLabel = new Label(ubicStr);
        ubicLabel.getStyleClass().add("item-card-sub");
        subRow.getChildren().addAll(espChip, ubicLabel);

        datosCol.getChildren().addAll(nombreLabel, subRow);

        // ── Estado + menú de acciones ────────────────────────
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox accionesBox = new HBox(16);
        accionesBox.setAlignment(Pos.CENTER_RIGHT);

        // ── Badge de estado (5 variantes) ────────────────────────────────
        Label statusLabel = new Label();
        switch (estado) {
            case Medico.ESTADO_DISPONIBLE   -> { statusLabel.setText("✓ Disponible");    statusLabel.getStyleClass().add("status-disponible"); }
            case Medico.ESTADO_EN_CONSULTA  -> { statusLabel.setText("En Consulta");     statusLabel.getStyleClass().add("status-consulta"); }
            case Medico.ESTADO_DESCANSANDO  -> { statusLabel.setText("☕ Descansando");  statusLabel.getStyleClass().add("status-descanso"); }
            case Medico.ESTADO_FUERA_TURNO  -> { statusLabel.setText("Fuera de turno"); statusLabel.getStyleClass().add("status-fuera-turno"); }
            case Medico.ESTADO_EMERGENCIA   -> { statusLabel.setText("🚨 Emergencia");  statusLabel.getStyleClass().add("status-urgencias"); }
            default                         -> { statusLabel.setText(estado);            statusLabel.getStyleClass().add("status-fuera-turno"); }
        }

        boolean esAdmin = Sesion.getUsuario() != null
                && Sesion.getUsuario().getRol().equals(Usuario.ROL_ADMIN);

        Button menuBtn = new Button("⋮");
        menuBtn.setStyle("-fx-background-color:transparent; -fx-font-size:18px;"
                + " -fx-text-fill:#73777f; -fx-cursor:hand; -fx-padding:4 8;");
        menuBtn.setVisible(esAdmin);
        menuBtn.setManaged(esAdmin);

        // Popup de cambio de estado (reemplaza al ContextMenu binario)
        menuBtn.setOnAction(ev -> mostrarPopupEstado(m, menuBtn));

        accionesBox.getChildren().addAll(statusLabel, menuBtn);

        String borderColor = switch (estado) {
            case Medico.ESTADO_DISPONIBLE   -> "#22c55e";
            case Medico.ESTADO_EN_CONSULTA  -> "#0060aa";
            case Medico.ESTADO_DESCANSANDO  -> "#54a3fd";
            case Medico.ESTADO_FUERA_TURNO  -> "#c3c6cf";
            case Medico.ESTADO_EMERGENCIA   -> "#ba1a1a";
            default                         -> "#c3c6cf";
        };
        card.setStyle(card.getStyle()
                + " -fx-border-width:0 0 0 4;"
                + " -fx-border-color:" + borderColor + " transparent transparent " + borderColor + ";");

        card.getChildren().addAll(avatarPane, datosCol, spacer, accionesBox);
        return card;
    }

    /* ══════════════════════════════════════════════════════════
       POPUP: Cambio de estado del médico (5 opciones)
       ══════════════════════════════════════════════════════════ */

    /**
     * Muestra un Popup con los 5 estados válidos del médico.
     * Al seleccionar uno, persiste el cambio y refresca la lista.
     */
    private void mostrarPopupEstado(Medico m, Button anchor) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setAutoFix(true);

        VBox panel = new VBox(0);
        panel.setStyle(
            "-fx-background-color:#ffffff; -fx-background-radius:14;" +
            "-fx-border-color:rgba(46,134,222,0.18); -fx-border-radius:14; -fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.2),16,0,6,4);" +
            "-fx-min-width:220;"
        );

        // Header con nombre del médico
        VBox header = new VBox(2);
        header.setStyle(
            "-fx-padding:12 16 10 16;" +
            "-fx-border-color:transparent transparent rgba(195,198,207,0.4) transparent;" +
            "-fx-border-width:0 0 1 0;"
        );
        Label nombreHdr = new Label("Dr(a). " + m.getNombreCompleto());
        nombreHdr.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#002444;");
        Label subHdr = new Label("Cambiar estado");
        subHdr.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f;");
        header.getChildren().addAll(nombreHdr, subHdr);

        // Las 5 opciones
        String estadoActual = m.getEstado() != null ? m.getEstado() : Medico.ESTADO_DISPONIBLE;

        String[][] opciones = {
            { Medico.ESTADO_DISPONIBLE,  "✓ Disponible",    "#22c55e", "#14532d" },
            { Medico.ESTADO_EN_CONSULTA, "🏥 En consulta",  "#2E86DE", "#002444" },
            { Medico.ESTADO_DESCANSANDO, "☕ Descansando",  "#54a3fd", "#0f1c2e" },
            { Medico.ESTADO_FUERA_TURNO, "🚪 Fuera de turno","#c3c6cf","#43474e" },
            { Medico.ESTADO_EMERGENCIA,  "🚨 Emergencia",   "#ba1a1a", "#ffffff" }
        };

        VBox lista = new VBox(0);
        for (int i = 0; i < opciones.length; i++) {
            final String nuevoEstado = opciones[i][0];
            String texto   = opciones[i][1];
            String badgeBg = opciones[i][2];
            String badgeFg = opciones[i][3];
            boolean esActual = nuevoEstado.equals(estadoActual);

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            boolean isLast = (i == opciones.length - 1);
            String border = isLast ? ""
                    : "-fx-border-color:transparent transparent rgba(195,198,207,0.3) transparent; -fx-border-width:0 0 1 0;";
            String baseStyle = "-fx-padding:10 16 10 16; " + border
                    + (esActual ? " -fx-background-color:#f0f4f8;" : "");
            row.setStyle(baseStyle);
            if (!esActual) {
                row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#f8faff; -fx-padding:10 16 10 16; " + border));
                row.setOnMouseExited(e  -> row.setStyle(baseStyle));
            }

            Label textoLabel = new Label(texto);
            textoLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#0b1c30;"
                    + (esActual ? " -fx-font-weight:bold;" : ""));
            HBox.setHgrow(textoLabel, Priority.ALWAYS);

            Label badge = new Label(nuevoEstado);
            badge.setStyle(
                "-fx-background-color:" + badgeBg + "; -fx-text-fill:" + badgeFg + ";" +
                "-fx-background-radius:10; -fx-padding:2 8 2 8; -fx-font-size:10px; -fx-font-weight:bold;"
            );

            if (esActual) {
                Label checkMark = new Label("●");
                checkMark.setStyle("-fx-font-size:8px; -fx-text-fill:#2E86DE; -fx-min-width:10;");
                row.getChildren().addAll(checkMark, textoLabel, badge);
            } else {
                Label spacer = new Label("  ");
                spacer.setStyle("-fx-min-width:10;");
                row.getChildren().addAll(spacer, textoLabel, badge);
            }

            if (!esActual) {
                row.setOnMouseClicked(e -> {
                    popup.hide();
                    try {
                        medicoService.cambiarEstado(m.getId(), nuevoEstado);
                        cargarDatos();
                    } catch (Exception ex) {
                        mostrarError("Error al cambiar estado: " + ex.getMessage());
                    }
                });
                row.setStyle(row.getStyle() + " -fx-cursor:hand;");
            }

            lista.getChildren().add(row);
        }

        // Separador + inactivar (solo admin)
        boolean esAdmin = Sesion.getUsuario() != null
                && Sesion.getUsuario().getRol().equals(Usuario.ROL_ADMIN);
        if (esAdmin) {
            Region sep = new Region();
            sep.setStyle("-fx-background-color:rgba(195,198,207,0.5); -fx-min-height:1; -fx-max-height:1;");
            HBox elimRow = new HBox();
            elimRow.setAlignment(Pos.CENTER_LEFT);
            elimRow.setStyle("-fx-padding:10 16 10 16; -fx-cursor:hand;");
            Label elimLabel = new Label("⊘  Inactivar médico");
            elimLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");
            elimRow.getChildren().add(elimLabel);
            elimRow.setOnMouseEntered(e -> elimRow.setStyle("-fx-background-color:#fff0f0; -fx-padding:10 16 10 16; -fx-cursor:hand;"));
            elimRow.setOnMouseExited(e  -> elimRow.setStyle("-fx-padding:10 16 10 16; -fx-cursor:hand;"));
            elimRow.setOnMouseClicked(e -> {
                popup.hide();
                confirmarEliminar(m);
            });
            panel.getChildren().addAll(header, lista, sep, elimRow);
        } else {
            panel.getChildren().addAll(header, lista);
        }

        popup.getContent().add(panel);

        Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
        popup.show(anchor, b.getMaxX() - 220, b.getMaxY() + 4);
    }

    /** Confirmación de inactivación. */
    private void confirmarEliminar(Medico m) {
        Stage st = (Stage) searchMedicoField.getScene().getWindow();
        boolean ok = NotificacionUtil.confirmar(st,
                "Inactivar médico",
                "El médico Dr(a). " + m.getNombreCompleto() + " quedará inactivo.\n" +
                "Sus citas e historial se conservan. Solo el admin puede reactivarlo.",
                "Sí, inactivar", "Cancelar", true);
        if (ok) {
            try {
                medicoService.eliminarMedico(m.getId());
                cargarDatos();
                NotificacionUtil.toast(st, NotificacionUtil.Tipo.ADVERTENCIA,
                        "Dr(a). " + m.getNombreCompleto() + " inactivado del sistema.");
            } catch (Exception ex) {
                mostrarError("No se pudo inactivar: " + ex.getMessage());
            }
        }
    }

    /* ══════════════════════════════════════════════════════════
       FAB: Nuevo médico — Modal personalizado CLINEX
       ══════════════════════════════════════════════════════════ */

    @FXML
    public void onAddMedico(ActionEvent event) {
        // ── Obtener la ventana principal ────────────────────────────────
        Stage ownerStage = (Stage) searchMedicoField.getScene().getWindow();

        // ── Crear Stage del modal ───────────────────────────────────────
        Stage modalStage = new Stage();
        modalStage.initOwner(ownerStage);
        modalStage.initModality(Modality.WINDOW_MODAL);
        modalStage.initStyle(StageStyle.TRANSPARENT);
        modalStage.setResizable(false);

        // ── Overlay oscuro de fondo ─────────────────────────────────────
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,36,68,0.55);");

        // ── Tarjeta modal (contenedor principal) ────────────────────────
        VBox modalCard = new VBox(0);
        modalCard.setMaxWidth(520);
        modalCard.setMinWidth(520);
        modalCard.getStyleClass().add("modal-card");
        modalCard.setStyle(
            "-fx-background-color:#ffffff;" +
            "-fx-background-radius:20;" +
            "-fx-border-color:rgba(46,134,222,0.2);" +
            "-fx-border-radius:20;" +
            "-fx-border-width:1;" +
            "-fx-padding:0;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.25),32,0,0,8);"
        );

        // ─── HEADER del modal ───────────────────────────────────────────
        HBox headerBox = new HBox(16);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setStyle(
            "-fx-background-color:#f8f9ff;" +
            "-fx-background-radius:20 20 0 0;" +
            "-fx-padding:24 28 20 28;" +
            "-fx-border-color:transparent transparent rgba(195,198,207,0.4) transparent;" +
            "-fx-border-width:0 0 1 0;"
        );

        // Ícono médico decorativo
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(48, 48);
        iconBox.setMaxSize(48, 48);
        iconBox.setStyle(
            "-fx-background-color:#eff4ff;" +
            "-fx-background-radius:14;"
        );
        Label iconLabel = new Label("⚕");
        iconLabel.setStyle("-fx-font-size:22px;");
        iconBox.getChildren().add(iconLabel);

        VBox headerTexts = new VBox(4);
        HBox.setHgrow(headerTexts, Priority.ALWAYS);
        Label modalTitle = new Label("Nuevo Médico");
        modalTitle.setStyle(
            "-fx-font-size:20px;" +
            "-fx-font-weight:bold;" +
            "-fx-text-fill:#002444;"
        );
        Label modalSubtitle = new Label("Complete los datos del profesional médico");
        modalSubtitle.getStyleClass().add("modal-subtitle");
        headerTexts.getChildren().addAll(modalTitle, modalSubtitle);

        // Botón cerrar (X)
        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
            "-fx-background-color:transparent;" +
            "-fx-text-fill:#73777f;" +
            "-fx-font-size:16px;" +
            "-fx-cursor:hand;" +
            "-fx-padding:4 8;" +
            "-fx-background-radius:8;"
        );
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
            "-fx-background-color:#f2f3f7;" +
            "-fx-text-fill:#0b1c30;" +
            "-fx-font-size:16px;" +
            "-fx-cursor:hand;" +
            "-fx-padding:4 8;" +
            "-fx-background-radius:8;"
        ));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
            "-fx-background-color:transparent;" +
            "-fx-text-fill:#73777f;" +
            "-fx-font-size:16px;" +
            "-fx-cursor:hand;" +
            "-fx-padding:4 8;" +
            "-fx-background-radius:8;"
        ));
        closeBtn.setOnAction(e -> modalStage.close());

        headerBox.getChildren().addAll(iconBox, headerTexts, closeBtn);

        // ─── CUERPO del formulario ──────────────────────────────────────
        VBox formBody = new VBox(20);
        formBody.setStyle("-fx-padding:28 28 0 28;");

        // ── Sección 1: Información personal ──────────────────────────
        Label seccion1Label = new Label("INFORMACIÓN PERSONAL");
        seccion1Label.getStyleClass().add("form-section-title");

        // Fila Nombre / Apellido
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
        nombreField.setPromptText("Ej: Carlos Andrés");
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
        apellidoField.setPromptText("Ej: Gómez Ruiz");
        apellidoField.getStyleClass().add("form-field");
        apellidoField.setMaxWidth(Double.MAX_VALUE);
        apellidoBox.getChildren().addAll(apellidoLabelRow, apellidoField);

        nombreApellidoRow.getChildren().addAll(nombreBox, apellidoBox);

        // Campo Especialidad — visible y obligatorio solo para Médico Especialista
        VBox especialidadBox = new VBox(6);
        HBox espLabelRow = new HBox(2);
        Label lblEsp = new Label("Especialidad");
        lblEsp.getStyleClass().add("form-label");
        Label espStar = new Label("*");
        espStar.getStyleClass().add("required-star");
        espLabelRow.getChildren().addAll(lblEsp, espStar);
        TextField especialidadField = new TextField();
        especialidadField.setPromptText("Ej: Cardiología, Pediatría, Neurología...");
        especialidadField.getStyleClass().add("form-field");
        especialidadField.setMaxWidth(Double.MAX_VALUE);
        Label espHint = new Label("Requerida solo para Médico Especialista.");
        espHint.getStyleClass().add("field-hint");
        especialidadBox.getChildren().addAll(espLabelRow, especialidadField, espHint);
        // Oculto por defecto (Médico General está seleccionado por defecto)
        especialidadBox.setVisible(false);
        especialidadBox.setManaged(false);
        especialidadField.setText(Medico.ESPECIALIDAD_GENERAL);

        // ── Divisor ──────────────────────────────────────────────────
        Region div1 = new Region();
        div1.getStyleClass().add("form-divider");

        // ── Sección 2: Contacto ───────────────────────────────────────
        Label seccion2Label = new Label("INFORMACIÓN DE CONTACTO  (opcional)");
        seccion2Label.getStyleClass().add("form-section-title");

        // Fila Teléfono / Correo
        HBox contactRow = new HBox(16);
        VBox telefonoBox = new VBox(6);
        HBox.setHgrow(telefonoBox, Priority.ALWAYS);
        Label lblTel = new Label("Teléfono");
        lblTel.getStyleClass().add("form-label");
        TextField telefonoField = new TextField();
        telefonoField.setPromptText("Ej: 3001234567");
        telefonoField.getStyleClass().add("form-field");
        telefonoField.setMaxWidth(Double.MAX_VALUE);
        // Solo dígitos, máx 10 caracteres
        telefonoField.textProperty().addListener((obs, oldVal, newVal) -> {
            String soloDigitos = newVal.replaceAll("[^0-9]", "");
            if (soloDigitos.length() > 10) soloDigitos = soloDigitos.substring(0, 10);
            if (!newVal.equals(soloDigitos)) telefonoField.setText(soloDigitos);
        });
        telefonoBox.getChildren().addAll(lblTel, telefonoField);

        VBox correoBox = new VBox(6);
        HBox.setHgrow(correoBox, Priority.ALWAYS);
        Label lblCorreo = new Label("Correo electrónico");
        lblCorreo.getStyleClass().add("form-label");
        TextField correoField = new TextField();
        correoField.setPromptText("medico@clinex.com");
        correoField.getStyleClass().add("form-field");
        correoField.setMaxWidth(Double.MAX_VALUE);
        correoBox.getChildren().addAll(lblCorreo, correoField);

        contactRow.getChildren().addAll(telefonoBox, correoBox);

        // ── Divisor 2 ─────────────────────────────────────────────────
        Region div2 = new Region();
        div2.getStyleClass().add("form-divider");

        // ── Sección 3: Tipo de médico ─────────────────────────────────
        Label seccion3Label = new Label("TIPO DE MÉDICO");
        seccion3Label.getStyleClass().add("form-section-title");

        VBox rolBox = new VBox(6);
        HBox rolLabelRow = new HBox(2);
        Label lblRol   = new Label("Categoría del médico");
        lblRol.getStyleClass().add("form-label");
        Label rolStar  = new Label("*");
        rolStar.getStyleClass().add("required-star");
        rolLabelRow.getChildren().addAll(lblRol, rolStar);
        ComboBox<String> rolCombo = new ComboBox<>();
        rolCombo.getItems().addAll("Médico General", "Médico Especialista");
        rolCombo.setValue("Médico General");
        rolCombo.getStyleClass().add("combo-box-clinex");
        rolCombo.setMaxWidth(Double.MAX_VALUE);
        Label rolHint = new Label("Define los permisos y accesos dentro del sistema.");
        rolHint.getStyleClass().add("field-hint");
        rolBox.getChildren().addAll(rolLabelRow, rolCombo, rolHint);

        // Listener: muestra/oculta especialidad según el tipo seleccionado
        rolCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean esEspecialista = "Médico Especialista".equals(newVal);
            especialidadBox.setVisible(esEspecialista);
            especialidadBox.setManaged(esEspecialista);
            if (!esEspecialista) {
                especialidadField.getStyleClass().remove("form-field-error");
                especialidadField.setText(Medico.ESPECIALIDAD_GENERAL);
            } else if (Medico.ESPECIALIDAD_GENERAL.equals(especialidadField.getText())) {
                especialidadField.clear();
            }
        });

        // ── Nota de campos obligatorios ───────────────────────────────
        HBox requiredNote = new HBox(4);
        requiredNote.setAlignment(Pos.CENTER_LEFT);
        Label starNote = new Label("*");
        starNote.getStyleClass().add("required-star");
        Label noteText = new Label("Campos obligatorios");
        noteText.getStyleClass().add("field-hint");
        requiredNote.getChildren().addAll(starNote, noteText);

        // ── Label de error general ────────────────────────────────────
        Label errorLabel = new Label();
        errorLabel.setStyle(
            "-fx-text-fill:#ba1a1a;" +
            "-fx-font-size:12px;" +
            "-fx-padding:6 12 6 12;" +
            "-fx-background-color:#ffdad6;" +
            "-fx-background-radius:8;" +
            "-fx-wrap-text:true;"
        );
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setMaxWidth(Double.MAX_VALUE);

        formBody.getChildren().addAll(
            seccion1Label,
            nombreApellidoRow,
            especialidadBox,
            div1,
            seccion2Label,
            contactRow,
            div2,
            seccion3Label,
            rolBox,
            requiredNote,
            errorLabel
        );

        // ─── FOOTER con botones ─────────────────────────────────────────
        HBox footerBox = new HBox(12);
        footerBox.setAlignment(Pos.CENTER_RIGHT);
        footerBox.setStyle(
            "-fx-padding:20 28 24 28;" +
            "-fx-border-color:rgba(195,198,207,0.4) transparent transparent transparent;" +
            "-fx-border-width:1 0 0 0;" +
            "-fx-background-color:#f8f9ff;" +
            "-fx-background-radius:0 0 20 20;"
        );

        Button cancelBtn = new Button("Cancelar");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setPrefWidth(120);
        cancelBtn.setOnAction(e -> modalStage.close());

        Button registrarBtn = new Button("✚  Registrar Médico");
        registrarBtn.getStyleClass().add("btn-primary");
        registrarBtn.setPrefWidth(180);

        footerBox.getChildren().addAll(cancelBtn, registrarBtn);

        // ── Validación en tiempo real (resaltar campos vacíos) ──────────
        nombreField.textProperty().addListener((obs, o, n) -> {
            if (!n.trim().isEmpty()) nombreField.getStyleClass().remove("form-field-error");
        });
        apellidoField.textProperty().addListener((obs, o, n) -> {
            if (!n.trim().isEmpty()) apellidoField.getStyleClass().remove("form-field-error");
        });
        especialidadField.textProperty().addListener((obs, o, n) -> {
            if (!n.trim().isEmpty() && especialidadBox.isVisible())
                especialidadField.getStyleClass().remove("form-field-error");
        });

        // ── Acción del botón Registrar ──────────────────────────────────
        registrarBtn.setOnAction(ev -> {
            String nombre      = nombreField.getText().trim();
            String apellido    = apellidoField.getText().trim();
            String especialidad = especialidadField.getText().trim();
            String telefono    = telefonoField.getText().trim();
            String correo      = correoField.getText().trim();

            // Limpiar errores previos
            nombreField.getStyleClass().remove("form-field-error");
            apellidoField.getStyleClass().remove("form-field-error");
            especialidadField.getStyleClass().remove("form-field-error");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);

            boolean valido = true;
            boolean esEspecialista = "Médico Especialista".equals(rolCombo.getValue());

            if (nombre.isEmpty()) {
                nombreField.getStyleClass().add("form-field-error");
                valido = false;
            }
            if (apellido.isEmpty()) {
                apellidoField.getStyleClass().add("form-field-error");
                valido = false;
            }
            // Especialidad solo obligatoria para Médico Especialista
            if (esEspecialista && especialidad.isEmpty()) {
                especialidadField.getStyleClass().add("form-field-error");
                valido = false;
            }

            if (!valido) {
                errorLabel.setText("⚠  Por favor complete todos los campos obligatorios marcados con *");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            // Para Médico General, usar valor centinela si el campo quedó vacío
            if (!esEspecialista && (especialidad.isEmpty() || Medico.ESPECIALIDAD_GENERAL.equals(especialidad))) {
                // especialidad ya es "Médico General" por el listener; garantizamos el valor
            }

            try {
                // Registrar el médico con campos individuales
                Medico medico = medicoService.registrarMedico(nombre, apellido, especialidad,
                        telefono, correo);

                // Crear usuario automáticamente
                String rolMedico = "Médico Especialista".equals(rolCombo.getValue())
                        ? Usuario.ROL_MEDICO_ESPECIALISTA
                        : Usuario.ROL_MEDICO_GENERAL;
                String userBase = "dr." + apellido.toLowerCase()
                        .replaceAll("[áàäãâ]", "a").replaceAll("[éèëê]", "e")
                        .replaceAll("[íìïî]", "i").replaceAll("[óòöôõ]", "o")
                        .replaceAll("[úùüû]", "u").replaceAll("[ñ]", "n")
                        .replaceAll("[^a-z0-9]", "");
                if (userBase.length() < 5) userBase = "medico";
                String tempPassword = "Clinex@" + (1000 + new java.util.Random().nextInt(9000));
                Usuario usuarioCreado = usuarioService.registrarUsuarioMedico(
                        userBase, tempPassword, rolMedico, medico.getId());

                modalStage.close();
                cargarDatos();

                // Mostrar credenciales al administrador
                Alert credAlert = new Alert(Alert.AlertType.INFORMATION);
                credAlert.setTitle("Usuario médico creado — CLINEX");
                credAlert.setHeaderText("Credenciales de acceso generadas");
                credAlert.setContentText(
                    "El médico Dr(a). " + nombre + " " + apellido
                    + " fue registrado exitosamente.\n\n"
                    + "Usuario:             " + usuarioCreado.getUsername() + "\n"
                    + "Contraseña temporal: " + tempPassword + "\n"
                    + "Rol:                 " + rolCombo.getValue() + "\n\n"
                    + "⚠  Guarda estos datos en un lugar seguro.\n"
                    + "Se recomienda cambiar la contraseña en el primer inicio de sesión."
                );
                credAlert.showAndWait();

            } catch (IllegalArgumentException ex) {
                errorLabel.setText("⚠  " + ex.getMessage());
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });

        // ── Ensamblar modal card ────────────────────────────────────────
        modalCard.getChildren().addAll(headerBox, formBody, footerBox);

        // ── Ensamblar overlay ───────────────────────────────────────────
        StackPane.setAlignment(modalCard, Pos.CENTER);
        overlay.getChildren().add(modalCard);

        // Cerrar al clic en el overlay (fuera de la tarjeta)
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) modalStage.close();
        });

        // ── Escena transparente ─────────────────────────────────────────
        Scene modalScene = new Scene(overlay,
                ownerStage.getScene().getWidth(),
                ownerStage.getScene().getHeight());
        modalScene.setFill(Color.TRANSPARENT);
        modalScene.getStylesheets().add(
            getClass().getResource("/com/mediconsult/beta/view/fxml/styles.css").toExternalForm()
        );

        modalStage.setScene(modalScene);

        // ── Foco en nombre al abrir ─────────────────────────────────────
        Platform.runLater(nombreField::requestFocus);

        modalStage.showAndWait();
    }

    /* ── Navegación ─────────────────────────────────────────── */

    @FXML public void onNavPacientes(ActionEvent e)   { navegarA("/com/mediconsult/beta/view/fxml/pacientes.fxml",  "CLINEX — Pacientes"); }
    @FXML public void onNavMedicos(ActionEvent e)     { /* ya estamos aquí */ }
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
            Stage stage = (Stage) searchMedicoField.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(titulo);
        } catch (IOException ex) {
            mostrarError("No se pudo cargar la pantalla: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void mostrarError(String msg) {
        Stage st = (Stage) searchMedicoField.getScene().getWindow();
        NotificacionUtil.alerta(st, NotificacionUtil.Tipo.ERROR, "Error — CLINEX", msg);
    }
}