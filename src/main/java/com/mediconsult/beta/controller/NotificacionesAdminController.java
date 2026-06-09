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
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NotificacionesAdminController {

    /* ── Constantes de severidad ────────────────────────────── */

    private static final String CRITICA     = "CRÍTICA";
    private static final String ADVERTENCIA = "ADVERTENCIA";
    private static final String INFO        = "INFO";

    /* ── Inyección FXML ─────────────────────────────────────── */

    @FXML private Label timestampLabel;
    @FXML private Label totalBadgeLabel;
    @FXML private Label statCriticasLabel;
    @FXML private Label statAdvertenciasLabel;
    @FXML private Label statInfoLabel;
    @FXML private VBox  alertasView;

    /* ── Servicios ──────────────────────────────────────────── */

    private final CitaService     citaService     = new CitaService();
    private final MedicoService   medicoService   = new MedicoService();
    private final PacienteService pacienteService = new PacienteService();
    private final RemisionService remisionService = new RemisionService();
    private final AltaMedicaService altaService   = new AltaMedicaService();

    /* ── Contadores ─────────────────────────────────────────── */

    private int cntCriticas;
    private int cntAdvertencias;
    private int cntInfo;

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        timestampLabel.setText("Generado: "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        generarAlertas();
    }

    /* ── Generación automática de alertas ───────────────────── */

    private void generarAlertas() {
        alertasView.getChildren().clear();
        cntCriticas = 0;
        cntAdvertencias = 0;
        cntInfo = 0;

        List<VBox> tarjetas = new ArrayList<>();

        tarjetas.addAll(alertasCitasVencidas());
        tarjetas.addAll(alertasRemisionesSinAsignar());
        tarjetas.addAll(alertasCitasHoy());
        tarjetas.addAll(alertasMedicosNoDisponibles());
        tarjetas.addAll(alertasRemisionesEnAtencion());
        tarjetas.addAll(alertasAltasRecientes());

        if (tarjetas.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setStyle("-fx-padding:48 0;");
            Label icon = new Label("✅");
            icon.setStyle("-fx-font-size:48px;");
            Label txt = new Label("Sin alertas. El sistema está operando con normalidad.");
            txt.setStyle("-fx-font-size:15px; -fx-text-fill:#73777f; -fx-font-style:italic;");
            empty.getChildren().addAll(icon, txt);
            alertasView.getChildren().add(empty);
        } else {
            alertasView.getChildren().addAll(tarjetas);
        }

        int total = cntCriticas + cntAdvertencias + cntInfo;
        statCriticasLabel.setText(String.valueOf(cntCriticas));
        statAdvertenciasLabel.setText(String.valueOf(cntAdvertencias));
        statInfoLabel.setText(String.valueOf(cntInfo));
        totalBadgeLabel.setText(total == 0 ? "Sin alertas"
                : total + (total == 1 ? " alerta" : " alertas"));
    }

    /* ── Citas vencidas (PENDIENTE, fecha < hoy) ────────────── */

    private List<VBox> alertasCitasVencidas() {
        List<VBox> lista = new ArrayList<>();
        try {
            LocalDate hoy = LocalDate.now();
            List<Cita> vencidas = citaService.listarTodas().stream()
                    .filter(c -> Cita.ESTADO_PENDIENTE.equals(c.getEstado())
                              && c.getFecha() != null
                              && c.getFecha().isBefore(hoy))
                    .toList();

            if (!vencidas.isEmpty()) {
                String desc = vencidas.size() + " cita" + (vencidas.size() > 1 ? "s" : "")
                        + " pendiente" + (vencidas.size() > 1 ? "s" : "")
                        + " con fecha pasada sin atender. Revisar y marcar como completadas o cancelarlas.";
                lista.add(buildCard("🚨  Citas vencidas sin atender",
                        desc, CRITICA, "#ba1a1a",
                        "Requiere acción inmediata"));
                cntCriticas++;
            }
        } catch (Exception e) {
            lista.add(buildError("Error en alertas de citas: " + e.getMessage()));
        }
        return lista;
    }

    /* ── Remisiones PENDIENTE sin asignar > 3 días ──────────── */

    private List<VBox> alertasRemisionesSinAsignar() {
        List<VBox> lista = new ArrayList<>();
        try {
            LocalDateTime limite = LocalDateTime.now().minusDays(3);
            List<Remision> demoradas = remisionService.listarTodas().stream()
                    .filter(r -> Remision.ESTADO_PENDIENTE.equals(r.getEstado())
                              && r.getFechaRegistro() != null
                              && r.getFechaRegistro().isBefore(limite))
                    .toList();

            List<Remision> recientes = remisionService.listarTodas().stream()
                    .filter(r -> Remision.ESTADO_PENDIENTE.equals(r.getEstado())
                              && (r.getFechaRegistro() == null
                                  || !r.getFechaRegistro().isBefore(limite)))
                    .toList();

            if (!demoradas.isEmpty()) {
                String desc = demoradas.size() + " remisión" + (demoradas.size() > 1 ? "es" : "")
                        + " lleva" + (demoradas.size() > 1 ? "n" : "")
                        + " más de 3 días sin especialista asignado. Acceder a Gestión de Remisiones para asignar.";
                lista.add(buildCard("⚠  Remisiones demoradas sin asignar",
                        desc, CRITICA, "#ba1a1a",
                        "Hace más de 3 días"));
                cntCriticas++;
            }

            if (!recientes.isEmpty()) {
                String desc = recientes.size() + " remisión" + (recientes.size() > 1 ? "es" : "")
                        + " pendiente" + (recientes.size() > 1 ? "s" : "")
                        + " de asignación de especialista.";
                lista.add(buildCard("🔗  Remisiones pendientes de asignación",
                        desc, ADVERTENCIA, "#F59E0B",
                        "Pendiente de gestión"));
                cntAdvertencias++;
            }
        } catch (Exception e) {
            lista.add(buildError("Error en alertas de remisiones: " + e.getMessage()));
        }
        return lista;
    }

    /* ── Citas de hoy PENDIENTE ─────────────────────────────── */

    private List<VBox> alertasCitasHoy() {
        List<VBox> lista = new ArrayList<>();
        try {
            LocalDate hoy = LocalDate.now();
            long hoyCount = citaService.listarTodas().stream()
                    .filter(c -> Cita.ESTADO_PENDIENTE.equals(c.getEstado())
                              && hoy.equals(c.getFecha()))
                    .count();

            if (hoyCount > 0) {
                String desc = hoyCount + " paciente" + (hoyCount > 1 ? "s" : "")
                        + " con cita para hoy. Ver Lista de Espera para la cola en tiempo real.";
                lista.add(buildCard("📅  Citas pendientes hoy",
                        desc, INFO, "#2E86DE",
                        "Hoy · " + hoyCount + (hoyCount > 1 ? " pacientes" : " paciente")));
                cntInfo++;
            }
        } catch (Exception ignored) {}
        return lista;
    }

    /* ── Médicos con disponible=false ───────────────────────── */

    private List<VBox> alertasMedicosNoDisponibles() {
        List<VBox> lista = new ArrayList<>();
        try {
            List<Medico> noDisp = medicoService.listarTodos().stream()
                    .filter(m -> !m.isDisponible())
                    .toList();

            if (!noDisp.isEmpty()) {
                StringBuilder nombres = new StringBuilder();
                noDisp.stream().limit(3).forEach(m ->
                        nombres.append("Dr(a). ").append(m.getNombreCompleto())
                               .append(" (").append(m.getEspecialidadDisplay()).append("), "));
                String desc = noDisp.size() + " médico" + (noDisp.size() > 1 ? "s" : "")
                        + " marcado" + (noDisp.size() > 1 ? "s" : "")
                        + " como no disponible: "
                        + nombres.toString().replaceAll(", $", "")
                        + (noDisp.size() > 3 ? " y " + (noDisp.size() - 3) + " más." : ".");
                lista.add(buildCard("⚕  Médicos no disponibles",
                        desc, ADVERTENCIA, "#F59E0B",
                        noDisp.size() + (noDisp.size() > 1 ? " médicos" : " médico")));
                cntAdvertencias++;
            }
        } catch (Exception ignored) {}
        return lista;
    }

    /* ── Remisiones EN_ATENCION activas ─────────────────────── */

    private List<VBox> alertasRemisionesEnAtencion() {
        List<VBox> lista = new ArrayList<>();
        try {
            long enAtencion = remisionService.listarTodas().stream()
                    .filter(r -> Remision.ESTADO_EN_ATENCION.equals(r.getEstado()))
                    .count();

            if (enAtencion > 0) {
                lista.add(buildCard("⚕  Remisiones en atención activa",
                        enAtencion + " remisión" + (enAtencion > 1 ? "es" : "")
                        + " actualmente siendo atendida" + (enAtencion > 1 ? "s" : "")
                        + " por especialistas.",
                        INFO, "#2E86DE",
                        enAtencion + (enAtencion > 1 ? " activas" : " activa")));
                cntInfo++;
            }
        } catch (Exception ignored) {}
        return lista;
    }

    /* ── Altas emitidas en los últimos 7 días ───────────────── */

    private List<VBox> alertasAltasRecientes() {
        List<VBox> lista = new ArrayList<>();
        try {
            LocalDate hace7 = LocalDate.now().minusDays(7);
            long recientes = altaService.listarPorPaciente(0) == null ? 0
                    : remisionService.listarTodas().stream()
                            .filter(r -> Remision.ESTADO_COMPLETADA.equals(r.getEstado())
                                      && r.getFechaRegistro() != null
                                      && !r.getFechaRegistro().toLocalDate().isBefore(hace7))
                            .count();

            if (recientes > 0) {
                lista.add(buildCard("✔  Remisiones completadas recientemente",
                        recientes + " remisión" + (recientes > 1 ? "es" : "")
                        + " completada" + (recientes > 1 ? "s" : "")
                        + " en los últimos 7 días. Alta médica emitida correctamente.",
                        INFO, "#065F46",
                        "Últimos 7 días"));
                cntInfo++;
            }
        } catch (Exception ignored) {}
        return lista;
    }

    /* ── Construcción de tarjetas ───────────────────────────── */

    private VBox buildCard(String titulo, String descripcion,
                           String nivel, String accentColor, String subtitulo) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:12; " +
                      "-fx-border-color:rgba(46,134,222,0.15); -fx-border-radius:12; " +
                      "-fx-border-width:1; " +
                      "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.06),8,0,0,2);");

        HBox content = new HBox(0);

        Region colorBar = new Region();
        colorBar.setStyle("-fx-background-color:" + accentColor + "; " +
                          "-fx-background-radius:12 0 0 12; -fx-min-width:6; -fx-max-width:6;");

        VBox body = new VBox(6);
        body.setStyle("-fx-padding:16 18;");
        HBox.setHgrow(body, Priority.ALWAYS);

        /* Cabecera */
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label tituloL = new Label(titulo);
        tituloL.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label nivelBadge = new Label(nivel);
        String badgeBg = switch (nivel) {
            case CRITICA     -> "#FEE2E2";
            case ADVERTENCIA -> "#FEF3C7";
            default          -> "#DBEAFE";
        };
        String badgeFg = switch (nivel) {
            case CRITICA     -> "#991B1B";
            case ADVERTENCIA -> "#92400E";
            default          -> "#1E40AF";
        };
        nivelBadge.setStyle("-fx-background-color:" + badgeBg + "; -fx-background-radius:8; " +
                            "-fx-padding:2 8; -fx-font-size:10px; " +
                            "-fx-font-weight:bold; -fx-text-fill:" + badgeFg + ";");

        Label subL = new Label(subtitulo);
        subL.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f;");

        header.getChildren().addAll(tituloL, spacer, nivelBadge, subL);

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
        l.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a; -fx-padding:8;");
        VBox box = new VBox(l);
        return box;
    }

    /* ── Navegación ─────────────────────────────────────────── */

    @FXML
    public void onVolver(ActionEvent event) {
        navegarA("/com/mediconsult/beta/view/fxml/pacientes.fxml", "CLINEX — Pacientes");
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
            Stage stage = (Stage) alertasView.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(titulo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
