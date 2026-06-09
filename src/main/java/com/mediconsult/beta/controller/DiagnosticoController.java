package com.mediconsult.beta.controller;

import com.mediconsult.beta.model.Cita;
import com.mediconsult.beta.model.Diagnostico;
import com.mediconsult.beta.model.Paciente;
import com.mediconsult.beta.service.CitaService;
import com.mediconsult.beta.service.DiagnosticoService;
import com.mediconsult.beta.service.PacienteService;
import com.mediconsult.beta.util.TopBarUtil;
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
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller de diagnostico.fxml — CLINEX
 *
 * <p>Gestiona el flujo clínico completo asistido por IA: selección de paciente,
 * captura de síntomas, generación del diagnóstico estructurado y renderizado
 * de todas las secciones clínicas (causas, gravedad, recomendaciones, exámenes,
 * tratamiento, seguimiento, revisión e historial del paciente).</p>
 *
 * @author MediConsult Team
 */
public class DiagnosticoController {

    /* ── Inyección FXML ─────────────────────────────────────── */

    @FXML private HBox  topBar;

    @FXML private ComboBox<String> pacienteCombo;
    @FXML private TextArea         symptomsArea;
    @FXML private Button           chipFiebreBtn;
    @FXML private Button           chipDolorBtn;
    @FXML private Button           chipFatigaBtn;
    @FXML private Button           generateBtn;

    @FXML private VBox    aiOutputPane;
    @FXML private Label   aiStatusLabel;
    @FXML private VBox    aiResultArea;

    @FXML private HBox warningBanner;

    @FXML private Button navPacientesBtn;
    @FXML private Button navMedicosBtn;
    @FXML private Button navCitasBtn;
    @FXML private Button navDiagnosticoBtn;

    /* ── Servicios ──────────────────────────────────────────── */

    private final DiagnosticoService diagnosticoService = new DiagnosticoService();
    private final CitaService        citaService        = new CitaService();
    private final PacienteService    pacienteService    = new PacienteService();

    private List<Cita>     citasDisponibles;
    private List<Paciente> pacientesDisponibles;

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        TopBarUtil.configurar(topBar);
        cargarCombosPacientes();
        aiStatusLabel.setText("Esperando input...");
    }

    /* ── Carga de datos ─────────────────────────────────────── */

    private void cargarCombosPacientes() {
        pacienteCombo.getItems().clear();
        try {
            citasDisponibles = citaService.listarTodas().stream()
                    .filter(c -> Cita.ESTADO_PENDIENTE.equals(c.getEstado())
                              || Cita.ESTADO_SOLICITUD.equals(c.getEstado()))
                    .toList();
            pacientesDisponibles = pacienteService.listarTodos();

            for (Cita cita : citasDisponibles) {
                String nombrePaciente = pacientesDisponibles.stream()
                        .filter(p -> p.getId() == cita.getPacienteId())
                        .map(Paciente::getNombreCompleto)
                        .findFirst()
                        .orElse("Paciente ID " + cita.getPacienteId());

                String fechaStr = cita.getFecha() != null ? " — " + cita.getFecha() : " (sin fecha)";
                pacienteCombo.getItems().add(
                        cita.getId() + " | " + nombrePaciente + fechaStr);
            }

            if (pacienteCombo.getItems().isEmpty()) {
                pacienteCombo.setPromptText("No hay citas pendientes ni solicitudes");
            }

        } catch (Exception e) {
            pacienteCombo.setPromptText("Error al cargar citas: " + e.getMessage());
        }
    }

    /* ── Chips de síntomas sugeridos ────────────────────────── */

    @FXML public void onAddSymptomFiebre(ActionEvent e)  { agregarSintoma("Fiebre"); }
    @FXML public void onAddSymptomDolor(ActionEvent e)   { agregarSintoma("Dolor de cabeza"); }
    @FXML public void onAddSymptomFatiga(ActionEvent e)  { agregarSintoma("Fatiga"); }
    @FXML public void onAddSymptomNauseas(ActionEvent e) { agregarSintoma("Náuseas"); }
    @FXML public void onAddSymptomTos(ActionEvent e)     { agregarSintoma("Tos"); }
    @FXML public void onAddSymptomDisnea(ActionEvent e)  { agregarSintoma("Dificultad para respirar (disnea)"); }

    private void agregarSintoma(String sintoma) {
        String actual = symptomsArea.getText().trim();
        symptomsArea.setText(actual.isEmpty() ? sintoma : actual + ", " + sintoma);
        symptomsArea.requestFocus();
        symptomsArea.positionCaret(symptomsArea.getText().length());
    }

    /* ── Generar Diagnóstico ────────────────────────────────── */

    @FXML
    public void onGenerateDiagnosis(ActionEvent event) {
        String selPaciente = pacienteCombo.getValue();
        String sintomas    = symptomsArea.getText().trim();

        if (selPaciente == null) {
            mostrarError("Selecciona un paciente / cita antes de generar el diagnóstico.");
            return;
        }
        if (sintomas.isEmpty()) {
            mostrarError("Describe los síntomas en el campo de texto antes de continuar.");
            return;
        }

        int citaId;
        int pacienteId;
        try {
            citaId = Integer.parseInt(selPaciente.split("\\s\\|")[0].trim());
            Optional<Cita> citaOpt = citasDisponibles.stream()
                    .filter(c -> c.getId() == citaId).findFirst();
            if (citaOpt.isEmpty()) throw new NumberFormatException();
            pacienteId = citaOpt.get().getPacienteId();
        } catch (NumberFormatException e) {
            mostrarError("Selección de cita inválida. Por favor vuelve a elegir.");
            return;
        }

        generateBtn.setDisable(true);
        generateBtn.setText("⏳  Generando...");
        aiStatusLabel.setText("Analizando con IA...");
        limpiarResultadoIA();
        mostrarEstadoCarga();

        final int    finalCitaId     = citaId;
        final int    finalPacienteId = pacienteId;
        final String finalSintomas   = sintomas;

        Task<Diagnostico> task = new Task<>() {
            @Override
            protected Diagnostico call() throws Exception {
                return diagnosticoService.generarDiagnostico(
                        finalCitaId, finalPacienteId, finalSintomas);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            Diagnostico dx = task.getValue();
            mostrarResultadoIA(dx, finalPacienteId);
            generateBtn.setDisable(false);
            generateBtn.setText("⚡  Generar Diagnóstico");
            aiStatusLabel.setText("Análisis completo ✓");

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "¿Marcar la cita como COMPLETADA?", ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Diagnóstico generado");
            confirm.setHeaderText("El diagnóstico se guardó con ID: " + dx.getId());
            confirm.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.YES) {
                    try {
                        citaService.completarCita(finalCitaId);
                        cargarCombosPacientes();
                    } catch (Exception ex) {
                        mostrarError("No se pudo completar la cita: " + ex.getMessage());
                    }
                }
            });
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            generateBtn.setDisable(false);
            generateBtn.setText("⚡  Generar Diagnóstico");
            aiStatusLabel.setText("Error en la generación");
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "Error desconocido";
            mostrarError("No se pudo generar el diagnóstico:\n" + msg);
            mostrarErrorIA(msg);
        }));

        new Thread(task, "dx-ia-thread").start();
    }

    /* ── Estado de carga ────────────────────────────────────── */

    private void mostrarEstadoCarga() {
        aiResultArea.getChildren().clear();
        aiResultArea.setAlignment(Pos.CENTER);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(60, 60);
        spinner.setStyle("-fx-accent:#2E86DE;");

        Label espera = new Label("Consultando inteligencia artificial...");
        espera.getStyleClass().add("ai-output-empty");
        Label sub = new Label("Generando análisis clínico completo...");
        sub.setStyle("-fx-font-size:12px; -fx-text-fill:rgba(67,71,78,0.5);");

        aiResultArea.getChildren().addAll(spinner, espera, sub);
    }

    private void limpiarResultadoIA() {
        aiResultArea.getChildren().clear();
    }

    /* ── Renderizado del resultado IA ───────────────────────── */

    /**
     * Muestra el diagnóstico completo con todas las secciones clínicas.
     * Si el resultado está en formato estructurado (contiene "CAUSAS:") lo renderiza
     * con secciones visuales diferenciadas. De lo contrario usa modo legado (texto libre).
     */
    private void mostrarResultadoIA(Diagnostico dx, int pacienteId) {
        aiResultArea.getChildren().clear();
        aiResultArea.setAlignment(Pos.TOP_LEFT);

        VBox contenido = new VBox(16);
        contenido.setPadding(new Insets(4, 4, 16, 4));

        String raw = dx.getResultadoIa() != null ? dx.getResultadoIa() : "";
        String texto = raw.replace("\r\n", "\n").replace("\r", "\n");
        boolean esEstructurado = texto.contains("CAUSAS:") || texto.contains("GRAVEDAD:");

        // ── Encabezado ──────────────────────────────────────────────────────
        HBox encabezado = new HBox(8);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        Label iconoOk = new Label("✅");
        iconoOk.setStyle("-fx-font-size:18px;");
        Label tituloRes = new Label("Diagnóstico ID " + dx.getId());
        tituloRes.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#002444;");
        encabezado.getChildren().addAll(iconoOk, tituloRes);
        contenido.getChildren().add(encabezado);

        // ── Síntomas reportados ──────────────────────────────────────────────
        contenido.getChildren().add(crearSeccionTexto("SÍNTOMAS REPORTADOS", dx.getSintomas()));

        if (esEstructurado) {
            renderizarFormatoEstructurado(contenido, texto);
        } else {
            renderizarFormatoLegado(contenido, texto);
        }

        // ── Separador ────────────────────────────────────────────────────────
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:rgba(46,134,222,0.25); -fx-padding:2 0;");
        contenido.getChildren().add(sep);

        // ── Historial de diagnósticos del paciente ────────────────────────────
        contenido.getChildren().add(crearHistorialPaciente(pacienteId, dx.getId()));

        aiResultArea.getChildren().add(contenido);
    }

    /** Renderiza todas las secciones clínicas del formato estructurado. */
    private void renderizarFormatoEstructurado(VBox contenido, String texto) {
        // Posibles causas
        List<String> causas = parsearLista(texto, "CAUSAS");
        if (!causas.isEmpty()) {
            contenido.getChildren().add(crearSeccionCausas(causas));
        }

        // Nivel de gravedad
        String gravedad      = parsearSeccion(texto, "GRAVEDAD");
        String justificacion = parsearSeccion(texto, "JUSTIFICACION");
        if (!gravedad.isEmpty()) {
            contenido.getChildren().add(crearSeccionGravedad(gravedad, justificacion));
        }

        // Recomendaciones médicas
        List<String> recomendaciones = parsearLista(texto, "RECOMENDACIONES");
        if (!recomendaciones.isEmpty()) {
            contenido.getChildren().add(
                crearSeccionChips("RECOMENDACIONES MÉDICAS", recomendaciones,
                                  "#DBEAFE", "#0060aa"));
        }

        // Exámenes sugeridos
        List<String> examenes = parsearLista(texto, "EXAMENES");
        if (!examenes.isEmpty()) {
            contenido.getChildren().add(
                crearSeccionChips("EXÁMENES SUGERIDOS", examenes,
                                  "#E0F2FE", "#0284C7"));
        }

        // Tratamiento inicial
        String tratamiento = parsearSeccion(texto, "TRATAMIENTO");
        if (!tratamiento.isEmpty()) {
            contenido.getChildren().add(
                crearSeccionTexto("TRATAMIENTO INICIAL SUGERIDO", tratamiento));
        }

        // Seguimiento del paciente
        String estadoActual  = parsearSeccion(texto, "ESTADO_ACTUAL");
        String observaciones = parsearSeccion(texto, "OBSERVACIONES");
        String evolucion     = parsearSeccion(texto, "EVOLUCION");
        if (!estadoActual.isEmpty() || !observaciones.isEmpty() || !evolucion.isEmpty()) {
            contenido.getChildren().add(
                crearSeccionSeguimiento(estadoActual, observaciones, evolucion));
        }

        // Próxima revisión
        String revision = parsearSeccion(texto, "REVISION");
        if (!revision.isEmpty()) {
            contenido.getChildren().add(crearSeccionRevision(revision));
        }
    }

    /** Renderiza la respuesta en formato de texto libre (versiones anteriores o errores). */
    private void renderizarFormatoLegado(VBox contenido, String texto) {
        VBox legacyBox = new VBox(4);
        legacyBox.getStyleClass().add("clinical-section");

        Label titulo = new Label("ANÁLISIS DE LA IA");
        titulo.getStyleClass().add("detail-section-label");
        legacyBox.getChildren().add(titulo);

        for (String parrafo : texto.split("\n")) {
            if (parrafo.trim().isEmpty()) continue;
            Label p = new Label(parrafo.trim());
            p.setWrapText(true);
            p.getStyleClass().add("label-body-md");
            p.setStyle("-fx-padding:2 0 2 0;");
            legacyBox.getChildren().add(p);
        }
        contenido.getChildren().add(legacyBox);
    }

    /* ── Parseo de secciones ────────────────────────────────── */

    /** Extrae el valor de una sección con formato "SECCION: valor" (una sola línea). */
    private String parsearSeccion(String texto, String seccion) {
        for (String linea : texto.split("\n")) {
            String trimmed = linea.trim();
            if (trimmed.startsWith(seccion + ":")) {
                return trimmed.substring(seccion.length() + 1).trim();
            }
        }
        return "";
    }

    /** Extrae una lista de elementos de una sección con valores separados por "|". */
    private List<String> parsearLista(String texto, String seccion) {
        String valor = parsearSeccion(texto, seccion);
        if (valor.isEmpty()) return List.of();
        return Arrays.stream(valor.split("\\|"))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .collect(Collectors.toList());
    }

    /* ── Constructores de secciones visuales ────────────────── */

    private VBox crearSeccionTexto(String titulo, String cuerpo) {
        VBox box = new VBox(6);
        box.getStyleClass().add("clinical-section");

        Label tituloLabel = new Label(titulo);
        tituloLabel.getStyleClass().add("detail-section-label");

        Label cuerpoLabel = new Label(cuerpo != null ? cuerpo : "");
        cuerpoLabel.setWrapText(true);
        cuerpoLabel.getStyleClass().add("label-body-md");

        box.getChildren().addAll(tituloLabel, cuerpoLabel);
        return box;
    }

    private VBox crearSeccionCausas(List<String> causas) {
        VBox box = new VBox(8);
        box.getStyleClass().add("clinical-section");

        Label titulo = new Label("POSIBLES CAUSAS");
        titulo.getStyleClass().add("detail-section-label");
        box.getChildren().add(titulo);

        int num = 1;
        for (String causa : causas) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label numLabel = new Label(num + ".");
            numLabel.setStyle("-fx-font-size:13px; -fx-font-weight:bold; "
                + "-fx-text-fill:#2E86DE; -fx-min-width:20;");

            Label causaLabel = new Label(causa);
            causaLabel.setWrapText(true);
            causaLabel.getStyleClass().add("label-body-md");
            HBox.setHgrow(causaLabel, Priority.ALWAYS);

            row.getChildren().addAll(numLabel, causaLabel);
            box.getChildren().add(row);
            num++;
        }
        return box;
    }

    private VBox crearSeccionGravedad(String nivel, String justificacion) {
        VBox box = new VBox(8);
        box.getStyleClass().add("clinical-section");

        Label titulo = new Label("NIVEL DE GRAVEDAD");
        titulo.getStyleClass().add("detail-section-label");
        box.getChildren().add(titulo);

        HBox nivelRow = new HBox(12);
        nivelRow.setAlignment(Pos.CENTER_LEFT);

        String icono = switch (nivel.toLowerCase()) {
            case "leve"       -> "🟢";
            case "moderado"   -> "🟡";
            case "grave"      -> "🟠";
            case "emergencia" -> "🔴";
            default           -> "⚪";
        };
        Label iconoLabel = new Label(icono);
        iconoLabel.setStyle("-fx-font-size:20px;");

        Label nivelLabel = new Label(nivel.toUpperCase());
        nivelLabel.getStyleClass().add("severity-" + nivel.toLowerCase());

        nivelRow.getChildren().addAll(iconoLabel, nivelLabel);
        box.getChildren().add(nivelRow);

        if (!justificacion.isEmpty()) {
            Label justLabel = new Label(justificacion);
            justLabel.setWrapText(true);
            justLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#43474e;");
            box.getChildren().add(justLabel);
        }
        return box;
    }

    private VBox crearSeccionChips(String titulo, List<String> items,
                                    String bgColor, String textColor) {
        VBox box = new VBox(8);
        box.getStyleClass().add("clinical-section");

        Label tituloLabel = new Label(titulo);
        tituloLabel.getStyleClass().add("detail-section-label");
        box.getChildren().add(tituloLabel);

        FlowPane chipPane = new FlowPane();
        chipPane.setHgap(8);
        chipPane.setVgap(8);

        for (String item : items) {
            Label chip = new Label(item);
            chip.setStyle("-fx-background-color:" + bgColor + "; -fx-background-radius:20; "
                + "-fx-border-color:rgba(0,0,0,0.08); -fx-border-radius:20; -fx-border-width:1; "
                + "-fx-padding:4 12 4 12; -fx-font-size:12px; -fx-font-weight:bold; "
                + "-fx-text-fill:" + textColor + ";");
            chipPane.getChildren().add(chip);
        }

        box.getChildren().add(chipPane);
        return box;
    }

    private VBox crearSeccionSeguimiento(String estadoActual,
                                          String observaciones,
                                          String evolucion) {
        VBox box = new VBox(10);
        box.getStyleClass().add("clinical-section");

        Label titulo = new Label("SEGUIMIENTO DEL PACIENTE");
        titulo.getStyleClass().add("detail-section-label");
        box.getChildren().add(titulo);

        if (!estadoActual.isEmpty()) {
            box.getChildren().add(crearSubseccion("Estado actual:", estadoActual));
        }
        if (!observaciones.isEmpty()) {
            box.getChildren().add(crearSubseccion("Observaciones:", observaciones));
        }
        if (!evolucion.isEmpty()) {
            box.getChildren().add(crearSubseccion("Evolución esperada:", evolucion));
        }
        return box;
    }

    private HBox crearSubseccion(String etiqueta, String valor) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.TOP_LEFT);

        Label etqLabel = new Label(etiqueta);
        etqLabel.setStyle("-fx-font-size:12px; -fx-font-weight:bold; "
            + "-fx-text-fill:#43474e; -fx-min-width:120;");

        Label valLabel = new Label(valor);
        valLabel.setWrapText(true);
        valLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#0b1c30;");
        HBox.setHgrow(valLabel, Priority.ALWAYS);

        row.getChildren().addAll(etqLabel, valLabel);
        return row;
    }

    private VBox crearSeccionRevision(String revision) {
        VBox box = new VBox(6);
        box.setStyle("-fx-background-color:#EFF4FF; -fx-background-radius:10; "
            + "-fx-border-color:#2E86DE; -fx-border-radius:10; -fx-border-width:1; "
            + "-fx-padding:12 16 12 16;");

        Label titulo = new Label("PRÓXIMA REVISIÓN");
        titulo.getStyleClass().add("detail-section-label");

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("📅");
        icon.setStyle("-fx-font-size:16px;");
        Label texto = new Label(revision);
        texto.setWrapText(true);
        texto.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#002444;");
        row.getChildren().addAll(icon, texto);

        box.getChildren().addAll(titulo, row);
        return box;
    }

    /* ── Historial de diagnósticos ──────────────────────────── */

    private VBox crearHistorialPaciente(int pacienteId, int currentDxId) {
        VBox hBox = new VBox(10);

        Label titulo = new Label("HISTORIAL DE DIAGNÓSTICOS");
        titulo.getStyleClass().add("detail-section-label");
        hBox.getChildren().add(titulo);

        try {
            List<Diagnostico> historial = diagnosticoService.listarPorPaciente(pacienteId)
                    .stream()
                    .filter(d -> d.getId() != currentDxId)
                    .limit(5)
                    .collect(Collectors.toList());

            if (historial.isEmpty()) {
                Label noHist = new Label(
                    "Sin diagnósticos previos registrados para este paciente.");
                noHist.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f; "
                    + "-fx-font-style:italic;");
                hBox.getChildren().add(noHist);
            } else {
                for (Diagnostico d : historial) {
                    hBox.getChildren().add(crearHistItemCard(d));
                }
            }
        } catch (Exception e) {
            Label errLabel = new Label("No se pudo cargar el historial.");
            errLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");
            hBox.getChildren().add(errLabel);
        }
        return hBox;
    }

    private HBox crearHistItemCard(Diagnostico d) {
        HBox row = new HBox(10);
        row.getStyleClass().add("hist-item-card");
        row.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("#" + d.getId());
        idLabel.setStyle("-fx-font-size:11px; -fx-font-weight:bold; "
            + "-fx-text-fill:#2E86DE; -fx-min-width:28;");

        String sint = d.getSintomas() != null ? d.getSintomas() : "";
        if (sint.length() > 55) sint = sint.substring(0, 55) + "…";
        Label sintomasLabel = new Label(sint);
        sintomasLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#43474e;");
        HBox.setHgrow(sintomasLabel, Priority.ALWAYS);

        row.getChildren().addAll(idLabel, sintomasLabel);

        // Mostrar badge de gravedad si el diagnóstico tiene formato estructurado
        String rawRes = d.getResultadoIa();
        if (rawRes != null && rawRes.contains("GRAVEDAD:")) {
            String grav = parsearSeccion(
                rawRes.replace("\r\n", "\n").replace("\r", "\n"), "GRAVEDAD");
            if (!grav.isEmpty()) {
                row.getChildren().add(crearGravBadgeSmall(grav));
            }
        }
        return row;
    }

    /** Crea un badge pequeño de gravedad con estilos inline (para uso en historial). */
    private Label crearGravBadgeSmall(String nivel) {
        String[] colors = switch (nivel.toLowerCase()) {
            case "leve"       -> new String[]{"#D1FAE5", "#065F46"};
            case "moderado"   -> new String[]{"#FEF3C7", "#92400E"};
            case "grave"      -> new String[]{"#FFEDD5", "#9A3412"};
            case "emergencia" -> new String[]{"#ba1a1a", "white"};
            default           -> new String[]{"#e5e7eb", "#374151"};
        };
        Label badge = new Label(nivel);
        badge.setStyle("-fx-background-color:" + colors[0] + "; -fx-text-fill:" + colors[1]
            + "; -fx-background-radius:12; -fx-padding:2 8 2 8; "
            + "-fx-font-size:10px; -fx-font-weight:bold;");
        return badge;
    }

    /* ── Error en el panel IA ───────────────────────────────── */

    private void mostrarErrorIA(String msg) {
        aiResultArea.getChildren().clear();
        aiResultArea.setAlignment(Pos.CENTER);

        Label iconoErr = new Label("⚠");
        iconoErr.setStyle("-fx-font-size:40px; -fx-text-fill:#F59E0B; -fx-opacity:0.7;");
        Label errLabel = new Label("No se pudo obtener el diagnóstico:\n" + msg);
        errLabel.getStyleClass().add("ai-output-empty");
        errLabel.setWrapText(true);
        errLabel.setMaxWidth(280);
        errLabel.setStyle("-fx-text-fill:#92400E;");

        aiResultArea.getChildren().addAll(iconoErr, errLabel);
    }

    /* ── Navegación ─────────────────────────────────────────── */

    @FXML public void onNavPacientes(ActionEvent e)   {
        navegarA("/com/mediconsult/beta/view/fxml/pacientes.fxml", "CLINEX — Pacientes");
    }
    @FXML public void onNavMedicos(ActionEvent e)     {
        navegarA("/com/mediconsult/beta/view/fxml/medicos.fxml", "CLINEX — Médicos");
    }
    @FXML public void onNavCitas(ActionEvent e)       {
        navegarA("/com/mediconsult/beta/view/fxml/citas.fxml", "CLINEX — Citas");
    }
    @FXML public void onNavDiagnostico(ActionEvent e) { /* ya estamos aquí */ }
    @FXML public void onNavRemisiones(ActionEvent e)   { navegarA("/com/mediconsult/beta/view/fxml/gestion_remisiones.fxml", "CLINEX — Remisiones"); }
    @FXML public void onNavListaEspera(ActionEvent e)  { navegarA("/com/mediconsult/beta/view/fxml/lista_espera.fxml",        "CLINEX — Lista de Espera"); }
    @FXML public void onNotificaciones(ActionEvent e) { navegarA("/com/mediconsult/beta/view/fxml/notificaciones_admin.fxml", "CLINEX — Notificaciones"); }

    /* ── Utilidades ─────────────────────────────────────────── */

    private void navegarA(String fxmlPath, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) generateBtn.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(),
                    stage.getScene().getHeight()));
            stage.setTitle(titulo);
        } catch (IOException ex) {
            mostrarError("No se pudo cargar la pantalla: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void mostrarError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setTitle("Error — CLINEX");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
