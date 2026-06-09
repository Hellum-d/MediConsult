package com.mediconsult.beta.controller;

import com.mediconsult.beta.model.*;
import com.mediconsult.beta.service.*;
import com.mediconsult.beta.util.NotificacionUtil;
import com.mediconsult.beta.util.Sesion;
import com.mediconsult.beta.util.TopBarUtil;
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
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class GestionRemisionesController {

    /* ── Inyección FXML ─────────────────────────────────────── */

    @FXML private HBox       topBar;
    @FXML private BorderPane rootPane;
    @FXML private Label      statTotalLabel;
    @FXML private Label      statPendientesLabel;
    @FXML private Label      statEnAtencionLabel;
    @FXML private Label      statCompletadasLabel;
    @FXML private Label      filtroActivoLabel;
    @FXML private VBox       remisionesView;

    /* ── Servicios ──────────────────────────────────────────── */

    private final RemisionService  remisionService  = new RemisionService();
    private final PacienteService  pacienteService  = new PacienteService();
    private final MedicoService    medicoService    = new MedicoService();

    /* ── Estado ─────────────────────────────────────────────── */

    private String filtroActual = "TODOS";
    private List<Remision> todasLasRemisiones;

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        TopBarUtil.configurar(topBar);
        cargarYMostrar("TODOS");
    }

    /* ── Filtros ────────────────────────────────────────────── */

    @FXML public void onFiltroTodos(ActionEvent e)      { cargarYMostrar("TODOS"); }
    @FXML public void onFiltroPendiente(ActionEvent e)  { cargarYMostrar(Remision.ESTADO_PENDIENTE); }
    @FXML public void onFiltroAceptada(ActionEvent e)   { cargarYMostrar(Remision.ESTADO_ACEPTADA); }
    @FXML public void onFiltroEnAtencion(ActionEvent e) { cargarYMostrar(Remision.ESTADO_EN_ATENCION); }
    @FXML public void onFiltroCompletada(ActionEvent e) { cargarYMostrar(Remision.ESTADO_COMPLETADA); }
    @FXML public void onFiltroCancelada(ActionEvent e)  { cargarYMostrar(Remision.ESTADO_CANCELADA); }

    private void cargarYMostrar(String filtro) {
        filtroActual = filtro;
        remisionesView.getChildren().clear();

        try {
            todasLasRemisiones = remisionService.listarTodas();

            /* Stats (siempre del total) */
            long total       = todasLasRemisiones.size();
            long pendientes  = contar(Remision.ESTADO_PENDIENTE);
            long enAtencion  = contar(Remision.ESTADO_ACEPTADA) + contar(Remision.ESTADO_EN_ATENCION);
            long completadas = contar(Remision.ESTADO_COMPLETADA);
            statTotalLabel.setText(String.valueOf(total));
            statPendientesLabel.setText(String.valueOf(pendientes));
            statEnAtencionLabel.setText(String.valueOf(enAtencion));
            statCompletadasLabel.setText(String.valueOf(completadas));

            /* Lista filtrada */
            List<Remision> lista = "TODOS".equals(filtro)
                    ? todasLasRemisiones
                    : todasLasRemisiones.stream()
                            .filter(r -> filtro.equals(r.getEstado())).toList();

            filtroActivoLabel.setText("Mostrando: " + ("TODOS".equals(filtro) ? "Todos"
                    : filtro.replace("_", " ")) + " (" + lista.size() + ")");

            if (lista.isEmpty()) {
                Label empty = new Label("No hay remisiones con este filtro.");
                empty.setStyle("-fx-font-size:14px; -fx-text-fill:#73777f; -fx-font-style:italic; -fx-padding:16 0;");
                remisionesView.getChildren().add(empty);
                return;
            }
            for (Remision r : lista) {
                remisionesView.getChildren().add(crearTarjeta(r));
            }
        } catch (Exception e) {
            Label err = new Label("Error al cargar remisiones: " + e.getMessage());
            err.setStyle("-fx-font-size:13px; -fx-text-fill:#ba1a1a;");
            remisionesView.getChildren().add(err);
        }
    }

    private long contar(String estado) {
        if (todasLasRemisiones == null) return 0;
        return todasLasRemisiones.stream().filter(r -> estado.equals(r.getEstado())).count();
    }

    /* ── Tarjeta de remisión ────────────────────────────────── */

    private VBox crearTarjeta(Remision r) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:14; " +
                      "-fx-border-color:rgba(46,134,222,0.18); -fx-border-radius:14; " +
                      "-fx-border-width:1; -fx-padding:16 20; " +
                      "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.06),8,0,0,2);");

        /* ── Cabecera: especialidad + estado + fecha ── */
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label espBadge = new Label("🔗  " + r.getEspecialidadDestino());
        espBadge.setStyle("-fx-background-color:#EEF2F8; -fx-background-radius:12; " +
                          "-fx-padding:3 12; -fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;");

        Label estadoBadge = new Label(getEstadoLabel(r.getEstado()));
        estadoBadge.setStyle(getEstadoStyle(r.getEstado()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String fecha = r.getFechaRegistro() != null
                ? r.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
        Label fechaL = new Label("📅  " + fecha);
        fechaL.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f;");

        Label idL = new Label("#" + r.getId());
        idL.setStyle("-fx-font-size:11px; -fx-text-fill:#adb5bd;");

        header.getChildren().addAll(espBadge, estadoBadge, spacer, fechaL, idL);

        /* ── Info paciente + médico remitente ── */
        HBox info = new HBox(32);
        info.setAlignment(Pos.CENTER_LEFT);

        String nombrePaciente = "Paciente ID " + r.getPacienteId();
        try {
            Optional<Paciente> p = pacienteService.obtenerPorId(r.getPacienteId());
            if (p.isPresent()) nombrePaciente = p.get().getNombreCompleto();
        } catch (Exception ignored) {}

        String nombreMedico = "Médico ID " + r.getMedicoRemitenteId();
        try {
            Optional<Medico> m = medicoService.obtenerPorId(r.getMedicoRemitenteId());
            if (m.isPresent()) nombreMedico = "Dr(a). " + m.get().getNombreCompleto();
        } catch (Exception ignored) {}

        VBox pacBox = new VBox(2);
        Label pacTitL = new Label("PACIENTE");
        pacTitL.setStyle("-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#73777f;");
        Label pacNomL = new Label(nombrePaciente);
        pacNomL.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");
        pacBox.getChildren().addAll(pacTitL, pacNomL);

        VBox medBox = new VBox(2);
        Label medTitL = new Label("MÉDICO REMITENTE");
        medTitL.setStyle("-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#73777f;");
        Label medNomL = new Label(nombreMedico);
        medNomL.setStyle("-fx-font-size:13px; -fx-text-fill:#43474e;");
        medBox.getChildren().addAll(medTitL, medNomL);

        info.getChildren().addAll(pacBox, medBox);

        /* ── Motivo ── */
        if (r.getMotivoRemision() != null && !r.getMotivoRemision().isBlank()) {
            String motivo = r.getMotivoRemision().length() > 120
                    ? r.getMotivoRemision().substring(0, 120) + "…" : r.getMotivoRemision();
            Label motivoL = new Label(motivo);
            motivoL.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f; -fx-wrap-text:true; -fx-font-style:italic;");
            motivoL.setWrapText(true);
            card.getChildren().addAll(header, info, motivoL);
        } else {
            card.getChildren().addAll(header, info);
        }

        /* ── Separador ── */
        Region sep = new Region();
        sep.setStyle("-fx-background-color:rgba(195,198,207,0.4); -fx-min-height:1; -fx-max-height:1;");
        card.getChildren().add(sep);

        /* ── Acciones ── */
        HBox acciones = new HBox(10);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        if (Remision.ESTADO_PENDIENTE.equals(r.getEstado())) {
            Button asignarBtn = new Button("👨‍⚕️  Asignar Especialista");
            asignarBtn.setStyle("-fx-background-color:#002444; -fx-text-fill:white; " +
                                "-fx-font-weight:bold; -fx-background-radius:8; " +
                                "-fx-padding:8 16; -fx-cursor:hand; -fx-font-size:12px;");
            asignarBtn.setOnAction(e -> abrirModalAsignacion(r));
            acciones.getChildren().add(asignarBtn);
        }

        boolean cancelable = !Remision.ESTADO_COMPLETADA.equals(r.getEstado())
                          && !Remision.ESTADO_CANCELADA.equals(r.getEstado());
        if (cancelable) {
            Button cancelarBtn = new Button("✗  Cancelar");
            cancelarBtn.setStyle("-fx-background-color:transparent; " +
                                 "-fx-border-color:#ba1a1a; -fx-border-radius:8; " +
                                 "-fx-background-radius:8; -fx-text-fill:#ba1a1a; " +
                                 "-fx-font-weight:bold; -fx-padding:8 16; " +
                                 "-fx-cursor:hand; -fx-font-size:12px;");
            cancelarBtn.setOnAction(e -> cancelarRemision(r));
            acciones.getChildren().add(cancelarBtn);
        }

        if (!acciones.getChildren().isEmpty()) {
            card.getChildren().add(acciones);
        }

        return card;
    }

    /* ── Modal: Asignar Especialista ────────────────────────── */

    private void abrirModalAsignacion(Remision remision) {
        Stage owner = (Stage) rootPane.getScene().getWindow();
        Stage modal = new Stage();
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.initOwner(owner);
        modal.initModality(Modality.APPLICATION_MODAL);

        /* ComboBox de especialistas coincidentes.
           Se normalizan ambas cadenas (sin acentos, minúsculas) para que
           "Cardiología" == "CARDIOLOGIA" == "cardiologia" */
        List<Medico> especialistas;
        try {
            String espRemision = normalizar(remision.getEspecialidadDestino());
            especialistas = medicoService.listarTodos().stream()
                    .filter(m -> m.isEspecialista()
                            && espRemision.equals(normalizar(m.getEspecialidad())))
                    .toList();
        } catch (Exception ex) {
            especialistas = List.of();
        }

        ComboBox<String> espCombo = new ComboBox<>();
        for (Medico m : especialistas) {
            espCombo.getItems().add(m.getId() + " — Dr(a). " + m.getNombreCompleto());
        }
        if (espCombo.getItems().isEmpty()) {
            espCombo.getItems().add("Sin especialistas disponibles para esta especialidad");
        }
        espCombo.setPromptText("Seleccionar especialista...");
        espCombo.setMaxWidth(Double.MAX_VALUE);
        espCombo.setStyle("-fx-background-radius:8; -fx-border-radius:8; " +
                          "-fx-border-color:rgba(46,134,222,0.3); -fx-font-size:13px;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");

        final List<Medico> finalEspecialistas = especialistas;

        Button confirmarBtn = new Button("Confirmar Asignación");
        confirmarBtn.setStyle("-fx-background-color:#002444; -fx-text-fill:white; " +
                              "-fx-font-weight:bold; -fx-background-radius:10; " +
                              "-fx-padding:10 24; -fx-cursor:hand; -fx-font-size:13px;");

        Button cancelarBtn = new Button("Cancelar");
        cancelarBtn.setStyle("-fx-background-color:transparent; " +
                             "-fx-border-color:rgba(46,134,222,0.35); -fx-border-radius:10; " +
                             "-fx-background-radius:10; -fx-text-fill:#002444; " +
                             "-fx-font-weight:bold; -fx-padding:10 24; -fx-cursor:hand; -fx-font-size:13px;");
        cancelarBtn.setOnAction(e -> modal.close());

        confirmarBtn.setOnAction(e -> {
            if (espCombo.getValue() == null || finalEspecialistas.isEmpty()) {
                errorLabel.setText("Selecciona un especialista.");
                return;
            }
            int idx = espCombo.getItems().indexOf(espCombo.getValue());
            if (idx < 0 || idx >= finalEspecialistas.size()) {
                errorLabel.setText("Selección inválida.");
                return;
            }
            try {
                Medico medico = finalEspecialistas.get(idx);
                remision.setMedicoEspecialistaId(medico.getId());
                remision.setEstado(Remision.ESTADO_ACEPTADA);
                remisionService.actualizar(remision);
                modal.close();
                cargarYMostrar(filtroActual);
                NotificacionUtil.toast((Stage) rootPane.getScene().getWindow(),
                        NotificacionUtil.Tipo.EXITO,
                        "Remisión #" + remision.getId() + " asignada a Dr(a). "
                        + medico.getNombreCompleto() + ".");
            } catch (Exception ex) {
                errorLabel.setText("Error: " + ex.getMessage());
            }
        });

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.getChildren().addAll(cancelarBtn, confirmarBtn);

        Label titleL = new Label("Asignar Especialista");
        titleL.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");

        Label subL = new Label("Remisión #" + remision.getId() + " · " + remision.getEspecialidadDestino());
        subL.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f;");

        Label lblEsp = new Label("ESPECIALISTA DISPONIBLE");
        lblEsp.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#2E86DE;");

        VBox card = new VBox(12);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:16; " +
                      "-fx-padding:28; -fx-max-width:480; " +
                      "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.22),24,0,0,6);");
        card.getChildren().addAll(titleL, subL, lblEsp, espCombo, errorLabel, btnRow);
        VBox.setMargin(btnRow, new Insets(4, 0, 0, 0));

        StackPane overlay = new StackPane(card);
        overlay.setStyle("-fx-background-color:rgba(0,36,68,0.55);");
        StackPane.setAlignment(card, Pos.CENTER);

        Scene scene = new Scene(overlay, owner.getWidth(), owner.getHeight());
        scene.setFill(Color.TRANSPARENT);
        modal.setScene(scene);
        modal.show();
    }

    /* ── Cancelar remisión ──────────────────────────────────── */

    private void cancelarRemision(Remision remision) {
        Stage st = (Stage) rootPane.getScene().getWindow();
        boolean ok = NotificacionUtil.confirmar(st,
                "Cancelar remisión",
                "¿Cancelar la remisión #" + remision.getId() + " a "
                + remision.getEspecialidadDestino() + "?\nEsta acción no puede deshacerse.",
                "Sí, cancelar", "No", true);
        if (ok) {
            try {
                remision.setEstado(Remision.ESTADO_CANCELADA);
                remisionService.actualizar(remision);
                cargarYMostrar(filtroActual);
                NotificacionUtil.toast(st, NotificacionUtil.Tipo.ADVERTENCIA,
                        "Remisión #" + remision.getId() + " cancelada.");
            } catch (Exception ex) {
                NotificacionUtil.alerta(st, NotificacionUtil.Tipo.ERROR,
                        "Error", "No se pudo cancelar: " + ex.getMessage());
            }
        }
    }

    /* ── Helpers de visualización ───────────────────────────── */

    private String getEstadoLabel(String estado) {
        if (estado == null) return "";
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
                "-fx-background-color:#FEF3C7; -fx-background-radius:10; -fx-padding:3 10; " +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#92400E;";
            case Remision.ESTADO_ACEPTADA ->
                "-fx-background-color:#D1FAE5; -fx-background-radius:10; -fx-padding:3 10; " +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#065F46;";
            case Remision.ESTADO_EN_ATENCION ->
                "-fx-background-color:#DBEAFE; -fx-background-radius:10; -fx-padding:3 10; " +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1E40AF;";
            case Remision.ESTADO_COMPLETADA ->
                "-fx-background-color:#E0E7FF; -fx-background-radius:10; -fx-padding:3 10; " +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#3730A3;";
            case Remision.ESTADO_CANCELADA ->
                "-fx-background-color:#FEE2E2; -fx-background-radius:10; -fx-padding:3 10; " +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#991B1B;";
            default ->
                "-fx-background-color:#e0e0e0; -fx-background-radius:10; -fx-padding:3 10; -fx-font-size:11px;";
        };
    }

    /* ── Navegación ─────────────────────────────────────────── */

    @FXML public void onNavPacientes(ActionEvent e)   { navegarA("/com/mediconsult/beta/view/fxml/pacientes.fxml",   "CLINEX — Pacientes"); }
    @FXML public void onNavMedicos(ActionEvent e)     { navegarA("/com/mediconsult/beta/view/fxml/medicos.fxml",     "CLINEX — Médicos"); }
    @FXML public void onNavCitas(ActionEvent e)       { navegarA("/com/mediconsult/beta/view/fxml/citas.fxml",       "CLINEX — Citas"); }
    @FXML public void onNavDiagnostico(ActionEvent e) { navegarA("/com/mediconsult/beta/view/fxml/diagnostico.fxml", "CLINEX — Diagnóstico IA"); }
    @FXML public void onNavRemisiones(ActionEvent e)   { /* ya estamos aquí */ }
    @FXML public void onNavListaEspera(ActionEvent e)  { navegarA("/com/mediconsult/beta/view/fxml/lista_espera.fxml",        "CLINEX — Lista de Espera"); }
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

    /**
     * Normaliza una especialidad para comparación: elimina acentos,
     * convierte a minúsculas y recorta espacios.
     * "Cardiología" == "CARDIOLOGIA" == "cardiologia"
     */
    private String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "")
                .toLowerCase()
                .trim();
    }
}
