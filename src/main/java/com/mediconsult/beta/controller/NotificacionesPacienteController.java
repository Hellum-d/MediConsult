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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NotificacionesPacienteController {

    /* ── Inyección FXML — Panel izquierdo ───────────────────── */

    @FXML private Label nombrePacienteLabel;
    @FXML private Label totalBadgeLabel;
    @FXML private Label statUrgentesLabel;
    @FXML private Label statPendientesLabel;
    @FXML private Label statInfoLabel;

    /* ── Inyección FXML — Panel derecho ─────────────────────── */

    @FXML private VBox notificacionesView;

    /* ── Servicios ──────────────────────────────────────────── */

    private final PacienteService           pacienteService  = new PacienteService();
    private final CitaService               citaService      = new CitaService();
    private final OrdenMedicaService        ordenGenService  = new OrdenMedicaService();
    private final OrdenEspecializadaService ordenEspService  = new OrdenEspecializadaService();
    private final RemisionService           remisionService  = new RemisionService();
    private final AltaMedicaService         altaService      = new AltaMedicaService();
    private final MedicoService             medicoService    = new MedicoService();

    private int pacienteId;

    /* ── Contadores ─────────────────────────────────────────── */

    private int countUrgentes;
    private int countPendientes;
    private int countInfo;

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        pacienteId = Sesion.getUsuario() != null ? Sesion.getUsuario().getPacienteId() : 0;
        cargarNombrePaciente();
        generarNotificaciones();
    }

    private void cargarNombrePaciente() {
        if (pacienteId <= 0) return;
        try {
            Optional<Paciente> opt = pacienteService.obtenerPorId(pacienteId);
            opt.ifPresent(p -> nombrePacienteLabel.setText(p.getNombreCompleto()));
        } catch (Exception ignored) {}
    }

    /* ── Generación de notificaciones ───────────────────────── */

    private void generarNotificaciones() {
        notificacionesView.getChildren().clear();
        countUrgentes = 0;
        countPendientes = 0;
        countInfo = 0;

        if (pacienteId <= 0) {
            agregarVacio("Sesión no válida.");
            actualizarContadores();
            return;
        }

        List<VBox> tarjetas = new ArrayList<>();

        tarjetas.addAll(notifCitas());
        tarjetas.addAll(notifOrdenes());
        tarjetas.addAll(notifRemisiones());
        tarjetas.addAll(notifAltas());

        if (tarjetas.isEmpty()) {
            agregarVacio("No tienes notificaciones pendientes. ¡Todo está al día!");
        } else {
            notificacionesView.getChildren().addAll(tarjetas);
        }

        actualizarContadores();
    }

    /* ── Citas ──────────────────────────────────────────────── */

    private List<VBox> notifCitas() {
        List<VBox> lista = new ArrayList<>();
        try {
            LocalDate hoy = LocalDate.now();
            List<Cita> citas = citaService.listarPorPaciente(pacienteId)
                    .stream()
                    .filter(c -> Cita.ESTADO_PENDIENTE.equals(c.getEstado())
                              && c.getFecha() != null
                              && !c.getFecha().isBefore(hoy))
                    .sorted((a, b) -> a.getFecha().compareTo(b.getFecha()))
                    .toList();

            for (Cita c : citas) {
                String fechaStr = c.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                String hora = c.getHora() != null ? " a las " + c.getHora() : "";

                String nombreMedico = "Médico ID " + c.getMedicoId();
                try {
                    Optional<Medico> m = medicoService.obtenerPorId(c.getMedicoId());
                    if (m.isPresent()) nombreMedico = "Dr(a). " + m.get().getNombreCompleto();
                } catch (Exception ignored) {}

                boolean esHoy = hoy.equals(c.getFecha());
                boolean proxima = !esHoy && c.getFecha().isBefore(hoy.plusDays(8));

                String titulo, descripcion, nivel, color;
                if (esHoy) {
                    titulo       = "📅  Cita médica HOY";
                    descripcion  = "Tienes una cita con " + nombreMedico + hora + ".";
                    nivel        = "URGENTE";
                    color        = "#ba1a1a";
                    countUrgentes++;
                } else if (proxima) {
                    titulo       = "📅  Cita próxima";
                    descripcion  = "Cita con " + nombreMedico + " el " + fechaStr + hora + ".";
                    nivel        = "PRÓXIMA";
                    color        = "#F59E0B";
                    countPendientes++;
                } else {
                    titulo       = "📅  Cita agendada";
                    descripcion  = "Cita con " + nombreMedico + " el " + fechaStr + hora + ".";
                    nivel        = "AGENDADA";
                    color        = "#2E86DE";
                    countInfo++;
                }
                lista.add(buildCard(titulo, descripcion, nivel, color, fechaStr));
            }
        } catch (Exception e) {
            lista.add(buildError("Error al cargar citas: " + e.getMessage()));
        }
        return lista;
    }

    /* ── Órdenes Médicas ────────────────────────────────────── */

    private List<VBox> notifOrdenes() {
        List<VBox> lista = new ArrayList<>();
        try {
            List<OrdenMedica> generales = ordenGenService.listarPorPaciente(pacienteId)
                    .stream().filter(o -> "ACTIVA".equals(o.getEstado())).toList();

            List<OrdenEspecializada> especializadas = ordenEspService.listarPorPaciente(pacienteId)
                    .stream().filter(o -> "ACTIVA".equals(o.getEstado())).toList();

            for (OrdenMedica o : generales) {
                String icon = tipoIcon(o.getTipo());
                String desc = "Orden de " + o.getTipo().toLowerCase()
                        + " prescrita por tu médico general. "
                        + (o.getDescripcion().length() > 60
                                ? o.getDescripcion().substring(0, 60) + "…"
                                : o.getDescripcion());
                String fecha = o.getFechaRegistro() != null
                        ? o.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
                lista.add(buildCard(icon + "  Orden activa — " + o.getTipo(),
                        desc, "ACTIVA", "#2E86DE", fecha));
                countInfo++;
            }

            for (OrdenEspecializada o : especializadas) {
                String icon = tipoIcon(o.getTipo());
                String desc = "Orden de " + o.getTipo().toLowerCase()
                        + " prescrita por tu especialista. "
                        + (o.getDescripcion().length() > 60
                                ? o.getDescripcion().substring(0, 60) + "…"
                                : o.getDescripcion());
                String fecha = o.getFechaRegistro() != null
                        ? o.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
                lista.add(buildCard(icon + "  Orden especializada activa",
                        desc, "ACTIVA", "#1a3a5c", fecha));
                countInfo++;
            }
        } catch (Exception e) {
            lista.add(buildError("Error al cargar órdenes: " + e.getMessage()));
        }
        return lista;
    }

    /* ── Remisiones ─────────────────────────────────────────── */

    private List<VBox> notifRemisiones() {
        List<VBox> lista = new ArrayList<>();
        try {
            List<Remision> remisiones = remisionService.listarPorPaciente(pacienteId)
                    .stream()
                    .filter(r -> !Remision.ESTADO_COMPLETADA.equals(r.getEstado())
                              && !Remision.ESTADO_CANCELADA.equals(r.getEstado()))
                    .toList();

            for (Remision r : remisiones) {
                String fecha = r.getFechaRegistro() != null
                        ? r.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";

                String titulo, descripcion, nivel, color;
                switch (r.getEstado()) {
                    case Remision.ESTADO_PENDIENTE -> {
                        titulo      = "🔗  Remisión pendiente — " + r.getEspecialidadDestino();
                        descripcion = "Tu remisión a " + r.getEspecialidadDestino()
                                + " está en espera de asignación por administración.";
                        nivel       = "PENDIENTE";
                        color       = "#F59E0B";
                        countPendientes++;
                    }
                    case Remision.ESTADO_ACEPTADA -> {
                        titulo      = "✓  Remisión aceptada — " + r.getEspecialidadDestino();
                        descripcion = "Tu remisión a " + r.getEspecialidadDestino()
                                + " ha sido aceptada. En breve serás atendido.";
                        nivel       = "ACEPTADA";
                        color       = "#059669";
                        countPendientes++;
                    }
                    default -> {
                        titulo      = "⚕  Remisión en atención — " + r.getEspecialidadDestino();
                        descripcion = "Tu remisión a " + r.getEspecialidadDestino()
                                + " está siendo gestionada por el especialista.";
                        nivel       = "EN ATENCIÓN";
                        color       = "#2E86DE";
                        countInfo++;
                    }
                }
                lista.add(buildCard(titulo, descripcion, nivel, color, fecha));
            }
        } catch (Exception e) {
            lista.add(buildError("Error al cargar remisiones: " + e.getMessage()));
        }
        return lista;
    }

    /* ── Altas médicas ──────────────────────────────────────── */

    private List<VBox> notifAltas() {
        List<VBox> lista = new ArrayList<>();
        try {
            List<AltaMedica> altas = altaService.listarPorPaciente(pacienteId);
            for (AltaMedica a : altas) {
                String fechaAlta = a.getFechaAlta() != null
                        ? a.getFechaAlta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
                String tipo = a.getTipoAlta() != null ? a.getTipoAlta().replace("_", " ") : "—";
                String descripcion = "Alta emitida el " + fechaAlta + " · Tipo: " + tipo + ".";
                if (!a.getIndicacionesPaciente().isBlank()) {
                    String ind = a.getIndicacionesPaciente().length() > 80
                            ? a.getIndicacionesPaciente().substring(0, 80) + "…"
                            : a.getIndicacionesPaciente();
                    descripcion += "\nIndicaciones: " + ind;
                }
                lista.add(buildCard("🏥  Alta médica emitida", descripcion,
                        "COMPLETADO", "#065F46", fechaAlta));
                countInfo++;
            }
        } catch (Exception e) {
            lista.add(buildError("Error al cargar altas: " + e.getMessage()));
        }
        return lista;
    }

    /* ── Construcción de tarjetas ───────────────────────────── */

    private VBox buildCard(String titulo, String descripcion,
                           String nivel, String accentColor, String fecha) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:12; " +
                      "-fx-border-color:rgba(46,134,222,0.15); -fx-border-radius:12; " +
                      "-fx-border-width:1; " +
                      "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.06),8,0,0,2);");

        /* Barra izquierda de color */
        HBox content = new HBox(0);

        Region colorBar = new Region();
        colorBar.setStyle("-fx-background-color:" + accentColor + "; " +
                          "-fx-background-radius:12 0 0 12; -fx-min-width:5; -fx-max-width:5;");
        colorBar.setPrefHeight(80);

        VBox body = new VBox(6);
        body.setStyle("-fx-padding:14 16;");
        HBox.setHgrow(body, Priority.ALWAYS);

        /* Cabecera: título + nivel badge + fecha */
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label tituloL = new Label(titulo);
        tituloL.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label nivelBadge = new Label(nivel);
        nivelBadge.setStyle("-fx-background-color:" + accentColor + "22; " +
                            "-fx-background-radius:8; -fx-padding:2 8; " +
                            "-fx-font-size:10px; -fx-font-weight:bold; " +
                            "-fx-text-fill:" + accentColor + ";");

        Label fechaL = new Label(fecha);
        fechaL.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f;");

        header.getChildren().addAll(tituloL, spacer, nivelBadge, fechaL);

        /* Descripción */
        Label descL = new Label(descripcion);
        descL.setStyle("-fx-font-size:12px; -fx-text-fill:#43474e; -fx-wrap-text:true;");
        descL.setWrapText(true);

        body.getChildren().addAll(header, descL);
        content.getChildren().addAll(colorBar, body);
        card.getChildren().add(content);
        return card;
    }

    private VBox buildError(String msg) {
        Label l = new Label("⚠  " + msg);
        l.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");
        VBox box = new VBox(l);
        box.setStyle("-fx-padding:8;");
        return box;
    }

    private void agregarVacio(String msg) {
        VBox empty = new VBox(16);
        empty.setAlignment(Pos.CENTER);
        empty.setStyle("-fx-padding:40 0;");
        Label icon = new Label("🔔");
        icon.setStyle("-fx-font-size:48px; -fx-text-fill:#d0d5dd;");
        Label txt = new Label(msg);
        txt.setStyle("-fx-font-size:15px; -fx-text-fill:#73777f; -fx-font-style:italic;");
        empty.getChildren().addAll(icon, txt);
        notificacionesView.getChildren().add(empty);
    }

    private void actualizarContadores() {
        int total = countUrgentes + countPendientes + countInfo;
        statUrgentesLabel.setText(String.valueOf(countUrgentes));
        statPendientesLabel.setText(String.valueOf(countPendientes));
        statInfoLabel.setText(String.valueOf(countInfo));
        totalBadgeLabel.setText(total == 0 ? "Sin notificaciones"
                : total + (total == 1 ? " notificación" : " notificaciones"));
    }

    private String tipoIcon(String tipo) {
        if (tipo == null) return "📋";
        return switch (tipo) {
            case "MEDICAMENTO"   -> "💊";
            case "PROCEDIMIENTO" -> "🩺";
            case "EXAMEN"        -> "🔬";
            default -> "📋";
        };
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
