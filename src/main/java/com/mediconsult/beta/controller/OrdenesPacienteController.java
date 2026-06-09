package com.mediconsult.beta.controller;

import com.mediconsult.beta.model.*;
import com.mediconsult.beta.service.*;
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

public class OrdenesPacienteController {

    /* ── Inyección FXML — Panel izquierdo ───────────────────── */

    @FXML private Label nombrePacienteLabel;
    @FXML private Label statMedicamentosLabel;
    @FXML private Label statProcedimientosLabel;
    @FXML private Label statExamenesLabel;

    /* ── Inyección FXML — Panel derecho ─────────────────────── */

    @FXML private VBox ordenesGeneralesView;
    @FXML private VBox ordenesEspView;

    /* ── Servicios ──────────────────────────────────────────── */

    private final PacienteService           pacienteService  = new PacienteService();
    private final OrdenMedicaService        ordenGenService  = new OrdenMedicaService();
    private final OrdenEspecializadaService ordenEspService  = new OrdenEspecializadaService();

    private int pacienteId;

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        pacienteId = Sesion.getUsuario() != null ? Sesion.getUsuario().getPacienteId() : 0;
        cargarNombrePaciente();
        cargarOrdenesGenerales();
        cargarOrdenesEspecialista();
    }

    private void cargarNombrePaciente() {
        if (pacienteId <= 0) return;
        try {
            Optional<Paciente> opt = pacienteService.obtenerPorId(pacienteId);
            opt.ifPresent(p -> nombrePacienteLabel.setText(p.getNombreCompleto()));
        } catch (Exception ignored) {}
    }

    /* ── Órdenes del Médico General ─────────────────────────── */

    private void cargarOrdenesGenerales() {
        ordenesGeneralesView.getChildren().clear();
        if (pacienteId <= 0) {
            agregarMensaje(ordenesGeneralesView, "No hay datos disponibles.", false);
            return;
        }
        try {
            List<OrdenMedica> lista = ordenGenService.listarPorPaciente(pacienteId);

            if (lista.isEmpty()) {
                agregarMensaje(ordenesGeneralesView, "Sin órdenes del médico general.", true);
                actualizarStats(List.of(), List.of());
                return;
            }
            for (OrdenMedica o : lista) {
                ordenesGeneralesView.getChildren().add(crearTarjetaGeneral(o));
            }
            actualizarStats(lista, List.of());

        } catch (Exception e) {
            agregarMensaje(ordenesGeneralesView, "Error al cargar órdenes: " + e.getMessage(), false);
        }
    }

    private VBox crearTarjetaGeneral(OrdenMedica o) {
        return buildCard(
                o.getTipo(),
                o.getDescripcion(),
                o.getObservaciones(),
                o.getEstado(),
                o.getFechaRegistro() != null
                        ? o.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—",
                "#2E86DE"
        );
    }

    /* ── Órdenes del Especialista ────────────────────────────── */

    private void cargarOrdenesEspecialista() {
        ordenesEspView.getChildren().clear();
        if (pacienteId <= 0) {
            agregarMensaje(ordenesEspView, "No hay datos disponibles.", false);
            return;
        }
        try {
            List<OrdenEspecializada> lista = ordenEspService.listarPorPaciente(pacienteId);

            if (lista.isEmpty()) {
                agregarMensaje(ordenesEspView, "Sin órdenes del especialista.", true);
                return;
            }
            for (OrdenEspecializada o : lista) {
                ordenesEspView.getChildren().add(crearTarjetaEspecializada(o));
            }

            List<OrdenMedica> generales;
            try {
                generales = ordenGenService.listarPorPaciente(pacienteId);
            } catch (Exception ex) {
                generales = List.of();
            }
            actualizarStats(generales, lista);

        } catch (Exception e) {
            agregarMensaje(ordenesEspView, "Error al cargar órdenes: " + e.getMessage(), false);
        }
    }

    private VBox crearTarjetaEspecializada(OrdenEspecializada o) {
        return buildCard(
                o.getTipo(),
                o.getDescripcion(),
                o.getObservaciones(),
                o.getEstado(),
                o.getFechaRegistro() != null
                        ? o.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—",
                "#1a3a5c"
        );
    }

    /* ── Construcción de tarjeta genérica ───────────────────── */

    private VBox buildCard(String tipo, String descripcion, String observaciones,
                           String estado, String fecha, String accentColor) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:12; " +
                      "-fx-border-color:rgba(46,134,222,0.15); -fx-border-radius:12; " +
                      "-fx-border-width:1; -fx-padding:14 18; " +
                      "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.05),6,0,0,1);");

        /* ── Cabecera: tipo badge + fecha + estado ── */
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label tipoBadge = new Label(getTipoLabel(tipo));
        tipoBadge.setStyle(getTipoStyle(tipo));

        Label estadoBadge = new Label(getEstadoLabel(estado));
        estadoBadge.setStyle(getEstadoStyle(estado));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label fechaL = new Label(fecha);
        fechaL.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f; -fx-font-weight:bold;");

        header.getChildren().addAll(tipoBadge, estadoBadge, spacer, fechaL);
        card.getChildren().add(header);

        /* ── Separador ── */
        Region sep = new Region();
        sep.setStyle("-fx-background-color:rgba(195,198,207,0.4); -fx-min-height:1; -fx-max-height:1;");
        card.getChildren().add(sep);

        /* ── Descripción ── */
        Label descL = new Label(descripcion != null ? descripcion : "");
        descL.setStyle("-fx-font-size:13px; -fx-text-fill:#0b1c30; -fx-wrap-text:true;");
        descL.setWrapText(true);
        card.getChildren().add(descL);

        /* ── Observaciones ── */
        if (observaciones != null && !observaciones.isBlank()) {
            Label obsL = new Label("Obs: " + observaciones);
            obsL.setStyle("-fx-font-size:12px; -fx-text-fill:#43474e; " +
                          "-fx-font-style:italic; -fx-wrap-text:true;");
            obsL.setWrapText(true);
            card.getChildren().add(obsL);
        }

        return card;
    }

    /* ── Estadísticas ───────────────────────────────────────── */

    private void actualizarStats(List<OrdenMedica> generales,
                                  List<OrdenEspecializada> especializadas) {
        long meds  = contar(generales, OrdenMedica.TIPO_MEDICAMENTO)
                   + contarEsp(especializadas, OrdenEspecializada.TIPO_MEDICAMENTO);
        long procs = contar(generales, OrdenMedica.TIPO_PROCEDIMIENTO)
                   + contarEsp(especializadas, OrdenEspecializada.TIPO_PROCEDIMIENTO);
        long exams = contar(generales, OrdenMedica.TIPO_EXAMEN)
                   + contarEsp(especializadas, OrdenEspecializada.TIPO_EXAMEN);
        statMedicamentosLabel.setText(String.valueOf(meds));
        statProcedimientosLabel.setText(String.valueOf(procs));
        statExamenesLabel.setText(String.valueOf(exams));
    }

    private long contar(List<OrdenMedica> lista, String tipo) {
        return lista.stream().filter(o -> tipo.equals(o.getTipo())).count();
    }

    private long contarEsp(List<OrdenEspecializada> lista, String tipo) {
        return lista.stream().filter(o -> tipo.equals(o.getTipo())).count();
    }

    /* ── Helpers de visualización ───────────────────────────── */

    private String getTipoLabel(String tipo) {
        if (tipo == null) return "—";
        return switch (tipo) {
            case "MEDICAMENTO"   -> "💊  Medicamento";
            case "PROCEDIMIENTO" -> "🩺  Procedimiento";
            case "EXAMEN"        -> "🔬  Examen";
            default -> tipo;
        };
    }

    private String getTipoStyle(String tipo) {
        if (tipo == null) return "";
        return switch (tipo) {
            case "MEDICAMENTO" ->
                "-fx-background-color:#DBEAFE; -fx-background-radius:12; -fx-padding:3 10; " +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1E40AF;";
            case "PROCEDIMIENTO" ->
                "-fx-background-color:#EDE9FE; -fx-background-radius:12; -fx-padding:3 10; " +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#5B21B6;";
            case "EXAMEN" ->
                "-fx-background-color:#D1FAE5; -fx-background-radius:12; -fx-padding:3 10; " +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#065F46;";
            default ->
                "-fx-background-color:#e0e0e0; -fx-background-radius:12; -fx-padding:3 10; -fx-font-size:11px;";
        };
    }

    private String getEstadoLabel(String estado) {
        if (estado == null) return "";
        return switch (estado) {
            case "ACTIVA"    -> "Activa";
            case "APLICADA"  -> "✔ Aplicada";
            case "CANCELADA" -> "✗ Cancelada";
            default -> estado;
        };
    }

    private String getEstadoStyle(String estado) {
        if (estado == null) return "";
        return switch (estado) {
            case "ACTIVA" ->
                "-fx-background-color:#FEF3C7; -fx-background-radius:10; -fx-padding:2 8; " +
                "-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#92400E;";
            case "APLICADA" ->
                "-fx-background-color:#D1FAE5; -fx-background-radius:10; -fx-padding:2 8; " +
                "-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#065F46;";
            case "CANCELADA" ->
                "-fx-background-color:#FEE2E2; -fx-background-radius:10; -fx-padding:2 8; " +
                "-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#991B1B;";
            default ->
                "-fx-background-color:#e0e0e0; -fx-background-radius:10; -fx-padding:2 8; -fx-font-size:10px;";
        };
    }

    private void agregarMensaje(VBox container, String msg, boolean italic) {
        Label l = new Label(msg);
        l.setStyle("-fx-font-size:13px; -fx-text-fill:#73777f; -fx-padding:8 0;"
                + (italic ? " -fx-font-style:italic;" : ""));
        container.getChildren().add(l);
    }

    /* ── Navegación ─────────────────────────────────────────── */

    @FXML
    public void onVolver(ActionEvent event) {
        navegarA("/com/mediconsult/beta/view/fxml/portal_paciente.fxml",
                 "CLINEX — Portal del Paciente");
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
            Stage stage = (Stage) nombrePacienteLabel.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(titulo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
