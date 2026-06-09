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

public class HistorialClinicoController {

    /* ── Estado estático para acceso desde admin ────────────── */

    private static int    pacienteIdExterno = 0;
    private static String retornarAFxml     = null;

    public static void setPacienteIdExterno(int id)    { pacienteIdExterno = id; }
    public static void setRetornarA(String fxmlPath)   { retornarAFxml = fxmlPath; }

    /* ── Inyección FXML — Panel izquierdo ───────────────────── */

    @FXML private Label nombrePacienteLabel;
    @FXML private Label edadSangreLabel;
    @FXML private Label alergiasLabel;
    @FXML private Label statGeneralesLabel;
    @FXML private Label statEspecializadasLabel;
    @FXML private Label statAutodiagLabel;
    @FXML private Label statOrdenesLabel;
    @FXML private Label statRemisionesLabel;

    /* ── Inyección FXML — Panel derecho ─────────────────────── */

    @FXML private VBox historiasView;
    @FXML private VBox consultasEspView;
    @FXML private VBox autodiagnosticosView;
    @FXML private VBox ordenesMedicasView;
    @FXML private VBox remisionesView;

    /* ── Servicios ──────────────────────────────────────────── */

    private final PacienteService              pacienteService  = new PacienteService();
    private final MedicoService                medicoService    = new MedicoService();
    private final HistoriaClinicaService       historiaService  = new HistoriaClinicaService();
    private final ConsultaEspecializadaService consultaService  = new ConsultaEspecializadaService();
    private final AutodiagnosticoService       autodiagService  = new AutodiagnosticoService();
    private final OrdenMedicaService           ordenService     = new OrdenMedicaService();
    private final RemisionService              remisionService  = new RemisionService();

    private int pacienteId;

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        // Prioridad: ID externo (admin ve historial de un paciente seleccionado)
        if (pacienteIdExterno > 0) {
            pacienteId = pacienteIdExterno;
            pacienteIdExterno = 0; // reset para futuros usos
        } else {
            pacienteId = Sesion.getUsuario() != null ? Sesion.getUsuario().getPacienteId() : 0;
        }
        cargarDatosPaciente();
        cargarHistoriasGenerales();
        cargarConsultasEspecializadas();
        cargarAutodiagnosticos();
        cargarOrdenesMedicas();
        cargarRemisiones();
    }

    private void cargarDatosPaciente() {
        if (pacienteId <= 0) {
            nombrePacienteLabel.setText("Paciente no identificado");
            return;
        }
        try {
            Optional<Paciente> opt = pacienteService.obtenerPorId(pacienteId);
            opt.ifPresent(p -> {
                nombrePacienteLabel.setText(p.getNombreCompleto());
                String ts = p.getTipoSangre() != null && !p.getTipoSangre().isBlank()
                        ? p.getTipoSangre() : "—";
                edadSangreLabel.setText(p.getEdad() + " años  ·  Sangre: " + ts);
                alergiasLabel.setText(p.getAlergias() != null && !p.getAlergias().isBlank()
                        ? p.getAlergias() : "Ninguna registrada");
            });
        } catch (Exception e) {
            nombrePacienteLabel.setText("Error al cargar datos");
        }
    }

    /* ── Consultas Médicas Generales ────────────────────────── */

    private void cargarHistoriasGenerales() {
        historiasView.getChildren().clear();
        if (pacienteId <= 0) { agregarMensaje(historiasView, "No hay datos disponibles.", false); return; }
        try {
            List<HistoriaClinica> lista = historiaService.listarPorPaciente(pacienteId);
            statGeneralesLabel.setText(String.valueOf(lista.size()));
            if (lista.isEmpty()) { agregarMensaje(historiasView, "Sin consultas médicas registradas.", true); return; }
            for (HistoriaClinica h : lista) historiasView.getChildren().add(crearTarjetaGeneral(h));
        } catch (Exception e) {
            agregarMensaje(historiasView, "Error al cargar historial: " + e.getMessage(), false);
        }
    }

    private VBox crearTarjetaGeneral(HistoriaClinica h) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:14; " +
                      "-fx-border-color:rgba(46,134,222,0.2); -fx-border-radius:14; " +
                      "-fx-border-width:1; -fx-padding:16 20; " +
                      "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.05),6,0,0,1);");
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label tipoBadge = new Label("📋  Consulta General");
        tipoBadge.setStyle("-fx-background-color:#DBEAFE; -fx-background-radius:12; " +
                           "-fx-padding:3 12; -fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1E40AF;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        String fechaStr = h.getFechaRegistro() != null
                ? h.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
        Label fechaL = new Label(fechaStr);
        fechaL.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f; -fx-font-weight:bold;");
        header.getChildren().addAll(tipoBadge, spacer, fechaL);

        String nombreMedico = "Médico ID " + h.getMedicoId();
        try {
            Optional<Medico> m = medicoService.obtenerPorId(h.getMedicoId());
            if (m.isPresent()) nombreMedico = "Dr(a). " + m.get().getNombreCompleto();
        } catch (Exception ignored) {}
        Label medicoL = new Label(nombreMedico);
        medicoL.setStyle("-fx-font-size:12px; -fx-text-fill:#43474e;");
        card.getChildren().addAll(header, medicoL);

        Region sep = new Region();
        sep.setStyle("-fx-background-color:rgba(195,198,207,0.4); -fx-min-height:1; -fx-max-height:1;");
        card.getChildren().add(sep);

        if (!h.getDiagnostico().isBlank()) card.getChildren().add(campoClinico("Diagnóstico", h.getDiagnostico(), true));
        if (!h.getTratamiento().isBlank()) {
            String tx = h.getTratamiento().length() > 120 ? h.getTratamiento().substring(0, 120) + "…" : h.getTratamiento();
            card.getChildren().add(campoClinico("Tratamiento", tx, false));
        }
        if (!h.getObservaciones().isBlank()) {
            String obs = h.getObservaciones().length() > 100 ? h.getObservaciones().substring(0, 100) + "…" : h.getObservaciones();
            card.getChildren().add(campoClinico("Observaciones", obs, false));
        }
        if (h.getDiagnostico().isBlank() && h.getTratamiento().isBlank()) {
            Label pendL = new Label("Consulta sin diagnóstico registrado");
            pendL.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f; -fx-font-style:italic;");
            card.getChildren().add(pendL);
        }
        return card;
    }

    /* ── Consultas Especializadas ────────────────────────────── */

    private void cargarConsultasEspecializadas() {
        consultasEspView.getChildren().clear();
        if (pacienteId <= 0) { agregarMensaje(consultasEspView, "No hay datos disponibles.", false); return; }
        try {
            List<ConsultaEspecializada> lista = consultaService.listarPorPaciente(pacienteId);
            statEspecializadasLabel.setText(String.valueOf(lista.size()));
            if (lista.isEmpty()) { agregarMensaje(consultasEspView, "Sin consultas especializadas registradas.", true); return; }
            for (ConsultaEspecializada c : lista) consultasEspView.getChildren().add(crearTarjetaEspecializada(c));
        } catch (Exception e) {
            agregarMensaje(consultasEspView, "Error al cargar consultas: " + e.getMessage(), false);
        }
    }

    private VBox crearTarjetaEspecializada(ConsultaEspecializada c) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:14; " +
                      "-fx-border-color:rgba(26,58,92,0.2); -fx-border-radius:14; " +
                      "-fx-border-width:1; -fx-padding:16 20; " +
                      "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.05),6,0,0,1);");
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label tipoBadge = new Label("🩺  Consulta Especializada");
        tipoBadge.setStyle("-fx-background-color:#EEF2F8; -fx-background-radius:12; " +
                           "-fx-padding:3 12; -fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;");
        Label estadoBadge = new Label(getEstadoLabel(c.getEstado()));
        estadoBadge.setStyle(getEstadoStyle(c.getEstado()));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        String fechaStr = c.getFechaRegistro() != null
                ? c.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
        Label fechaL = new Label(fechaStr);
        fechaL.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f; -fx-font-weight:bold;");
        header.getChildren().addAll(tipoBadge, estadoBadge, spacer, fechaL);

        String nombreMedico = "Especialista ID " + c.getMedicoEspecialistaId();
        try {
            Optional<Medico> m = medicoService.obtenerPorId(c.getMedicoEspecialistaId());
            if (m.isPresent()) nombreMedico = "Dr(a). " + m.get().getNombreCompleto()
                    + (m.get().isEspecialista() ? " — " + m.get().getEspecialidad() : "");
        } catch (Exception ignored) {}
        Label medicoL = new Label(nombreMedico);
        medicoL.setStyle("-fx-font-size:12px; -fx-text-fill:#43474e;");
        card.getChildren().addAll(header, medicoL);

        Region sep = new Region();
        sep.setStyle("-fx-background-color:rgba(195,198,207,0.4); -fx-min-height:1; -fx-max-height:1;");
        card.getChildren().add(sep);

        if (!c.getDiagnostico().isBlank()) card.getChildren().add(campoClinico("Diagnóstico", c.getDiagnostico(), true));
        if (!c.getTratamiento().isBlank()) {
            String tx = c.getTratamiento().length() > 120 ? c.getTratamiento().substring(0, 120) + "…" : c.getTratamiento();
            card.getChildren().add(campoClinico("Tratamiento", tx, false));
        }
        if (!c.getEvolucion().isBlank()) {
            String ev = c.getEvolucion().length() > 100 ? c.getEvolucion().substring(0, 100) + "…" : c.getEvolucion();
            card.getChildren().add(campoClinico("Evolución", ev, false));
        }
        if (c.getDiagnostico().isBlank() && c.getTratamiento().isBlank()) {
            Label pendL = new Label("Consulta en proceso");
            pendL.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f; -fx-font-style:italic;");
            card.getChildren().add(pendL);
        }
        return card;
    }

    /* ── Diagnósticos IA ─────────────────────────────────────── */

    private void cargarAutodiagnosticos() {
        autodiagnosticosView.getChildren().clear();
        if (pacienteId <= 0) { agregarMensaje(autodiagnosticosView, "No hay datos disponibles.", false); return; }
        try {
            List<Autodiagnostico> lista = autodiagService.listarPorPaciente(pacienteId);
            statAutodiagLabel.setText(String.valueOf(lista.size()));
            if (lista.isEmpty()) { agregarMensaje(autodiagnosticosView, "Sin diagnósticos IA registrados.", true); return; }
            for (Autodiagnostico ad : lista) autodiagnosticosView.getChildren().add(crearTarjetaAutodiag(ad));
        } catch (Exception e) {
            agregarMensaje(autodiagnosticosView, "Error al cargar autodiagnósticos: " + e.getMessage(), false);
        }
    }

    private VBox crearTarjetaAutodiag(Autodiagnostico ad) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:14; " +
                      "-fx-border-color:rgba(99,102,241,0.2); -fx-border-radius:14; " +
                      "-fx-border-width:1; -fx-padding:14 18; " +
                      "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.05),6,0,0,1);");
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label("🤖  Diagnóstico IA");
        badge.setStyle("-fx-background-color:#EEF2FF; -fx-background-radius:12; " +
                       "-fx-padding:3 12; -fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#4338CA;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        String fechaStr = ad.getFechaRegistro() != null
                ? ad.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";
        Label fechaL = new Label(fechaStr);
        fechaL.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f;");
        header.getChildren().addAll(badge, spacer, fechaL);
        card.getChildren().add(header);

        if (ad.getSintomas() != null && !ad.getSintomas().isBlank()) {
            String sint = ad.getSintomas().length() > 120 ? ad.getSintomas().substring(0, 120) + "…" : ad.getSintomas();
            card.getChildren().add(campoClinico("Síntomas", sint, false));
        }
        if (ad.getResultadoIa() != null && ad.getResultadoIa().contains("GRAVEDAD:")) {
            String grav = parsearSeccion(ad.getResultadoIa(), "GRAVEDAD");
            if (!grav.isEmpty()) card.getChildren().add(campoClinico("Gravedad IA", grav.toUpperCase(), true));
        }
        return card;
    }

    /* ── Órdenes Médicas ─────────────────────────────────────── */

    private void cargarOrdenesMedicas() {
        ordenesMedicasView.getChildren().clear();
        if (pacienteId <= 0) { agregarMensaje(ordenesMedicasView, "No hay datos disponibles.", false); return; }
        try {
            List<OrdenMedica> lista = ordenService.listarPorPaciente(pacienteId);
            statOrdenesLabel.setText(String.valueOf(lista.size()));
            if (lista.isEmpty()) { agregarMensaje(ordenesMedicasView, "Sin órdenes médicas registradas.", true); return; }
            for (OrdenMedica o : lista) ordenesMedicasView.getChildren().add(crearTarjetaOrden(o));
        } catch (Exception e) {
            agregarMensaje(ordenesMedicasView, "Error al cargar órdenes: " + e.getMessage(), false);
        }
    }

    private VBox crearTarjetaOrden(OrdenMedica o) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:14; " +
                      "-fx-border-color:rgba(46,134,222,0.15); -fx-border-radius:14; " +
                      "-fx-border-width:1; -fx-padding:14 18; " +
                      "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.05),6,0,0,1);");
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        String tipoIcon = switch (o.getTipo() != null ? o.getTipo() : "") {
            case OrdenMedica.TIPO_MEDICAMENTO   -> "💊  Medicamento";
            case OrdenMedica.TIPO_EXAMEN        -> "🔬  Examen";
            case OrdenMedica.TIPO_PROCEDIMIENTO -> "🩺  Procedimiento";
            default -> "📋  Orden";
        };
        Label badge = new Label(tipoIcon);
        badge.setStyle("-fx-background-color:#DBEAFE; -fx-background-radius:12; " +
                       "-fx-padding:3 12; -fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1E40AF;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        String fechaStr = o.getFechaRegistro() != null
                ? o.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
        Label fechaL = new Label(fechaStr);
        fechaL.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f;");
        header.getChildren().addAll(badge, spacer, fechaL);
        card.getChildren().add(header);

        if (o.getDescripcion() != null && !o.getDescripcion().isBlank()) {
            String desc = o.getDescripcion().length() > 120 ? o.getDescripcion().substring(0, 120) + "…" : o.getDescripcion();
            card.getChildren().add(campoClinico("Descripción", desc, false));
        }
        if (o.getObservaciones() != null && !o.getObservaciones().isBlank()) {
            card.getChildren().add(campoClinico("Observaciones", o.getObservaciones(), false));
        }
        return card;
    }

    /* ── Remisiones ─────────────────────────────────────────── */

    private void cargarRemisiones() {
        remisionesView.getChildren().clear();
        if (pacienteId <= 0) { agregarMensaje(remisionesView, "No hay datos disponibles.", false); return; }
        try {
            List<Remision> lista = remisionService.listarPorPaciente(pacienteId);
            statRemisionesLabel.setText(String.valueOf(lista.size()));
            if (lista.isEmpty()) { agregarMensaje(remisionesView, "Sin remisiones registradas.", true); return; }
            for (Remision r : lista) remisionesView.getChildren().add(crearTarjetaRemision(r));
        } catch (Exception e) {
            agregarMensaje(remisionesView, "Error al cargar remisiones: " + e.getMessage(), false);
        }
    }

    private VBox crearTarjetaRemision(Remision r) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:14; " +
                      "-fx-border-color:rgba(26,58,92,0.15); -fx-border-radius:14; " +
                      "-fx-border-width:1; -fx-padding:14 18; " +
                      "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.05),6,0,0,1);");
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label("🔗  " + (r.getEspecialidadDestino() != null ? r.getEspecialidadDestino() : "Remisión"));
        badge.setStyle("-fx-background-color:#EFF6FF; -fx-background-radius:12; " +
                       "-fx-padding:3 12; -fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1E3A5F;");
        Label estadoL = new Label(r.getEstado() != null ? r.getEstado() : "PENDIENTE");
        estadoL.setStyle("-fx-background-color:#FEF3C7; -fx-background-radius:10; -fx-padding:2 8; " +
                         "-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#92400E;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        String fechaStr = r.getFechaRegistro() != null
                ? r.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
        Label fechaL = new Label(fechaStr);
        fechaL.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f;");
        header.getChildren().addAll(badge, estadoL, spacer, fechaL);
        card.getChildren().add(header);

        if (r.getMotivoRemision() != null && !r.getMotivoRemision().isBlank()) {
            String mot = r.getMotivoRemision().length() > 120 ? r.getMotivoRemision().substring(0, 120) + "…" : r.getMotivoRemision();
            card.getChildren().add(campoClinico("Motivo", mot, false));
        }
        return card;
    }

    /* ── Helpers de UI ──────────────────────────────────────── */

    private HBox campoClinico(String etiqueta, String valor, boolean bold) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.TOP_LEFT);
        Label etiq = new Label(etiqueta + ":");
        etiq.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#73777f; -fx-min-width:100;");
        Label val = new Label(valor);
        val.setStyle("-fx-font-size:13px; -fx-text-fill:#0b1c30; -fx-wrap-text:true;"
                + (bold ? " -fx-font-weight:bold;" : ""));
        val.setWrapText(true);
        HBox.setHgrow(val, Priority.ALWAYS);
        row.getChildren().addAll(etiq, val);
        return row;
    }

    private void agregarMensaje(VBox container, String msg, boolean italic) {
        Label l = new Label(msg);
        l.setStyle("-fx-font-size:13px; -fx-text-fill:#73777f; -fx-padding:8 0;"
                + (italic ? " -fx-font-style:italic;" : ""));
        container.getChildren().add(l);
    }

    private String getEstadoLabel(String estado) {
        if (estado == null) return "";
        return switch (estado) {
            case ConsultaEspecializada.ESTADO_ACTIVA     -> "En proceso";
            case ConsultaEspecializada.ESTADO_COMPLETADA -> "✔ Completada";
            default -> estado;
        };
    }

    private String getEstadoStyle(String estado) {
        if (ConsultaEspecializada.ESTADO_COMPLETADA.equals(estado)) {
            return "-fx-background-color:#D1FAE5; -fx-background-radius:10; -fx-padding:2 8; " +
                   "-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#065F46;";
        }
        return "-fx-background-color:#FEF3C7; -fx-background-radius:10; -fx-padding:2 8; " +
               "-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#92400E;";
    }

    private String parsearSeccion(String texto, String seccion) {
        for (String linea : texto.split("\n")) {
            if (linea.trim().startsWith(seccion + ":"))
                return linea.trim().substring(seccion.length() + 1).trim();
        }
        return "";
    }

    /* ── Navegación ─────────────────────────────────────────── */

    @FXML
    public void onVolver(ActionEvent event) {
        String destino = retornarAFxml;
        retornarAFxml = null; // reset
        if (destino != null) {
            navegarA(destino, "CLINEX — Pacientes");
        } else {
            navegarA("/com/mediconsult/beta/view/fxml/portal_paciente.fxml",
                     "CLINEX — Portal del Paciente");
        }
    }

    @FXML
    public void onCerrarSesion(ActionEvent event) {
        retornarAFxml = null;
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
