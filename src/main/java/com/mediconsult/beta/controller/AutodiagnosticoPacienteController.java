package com.mediconsult.beta.controller;

import com.mediconsult.beta.model.Autodiagnostico;
import com.mediconsult.beta.service.AutodiagnosticoService;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller de autodiagnostico_paciente.fxml — CLINEX
 *
 * <p>Permite al paciente describir sus síntomas y obtener un análisis
 * preliminar de IA (GPT-4o-mini). El resultado queda guardado en la tabla
 * {@code autodiagnostico} y en el historial clínico del paciente.</p>
 */
public class AutodiagnosticoPacienteController {

    /* ── Inyección FXML ─────────────────────────────────────── */

    @FXML private Label    nombrePacienteLabel;
    @FXML private TextArea symptomsArea;
    @FXML private Button   generateBtn;
    @FXML private Label    aiStatusLabel;
    @FXML private VBox     aiResultArea;
    @FXML private VBox     historialArea;

    /* ── Servicios ──────────────────────────────────────────── */

    private final AutodiagnosticoService service = new AutodiagnosticoService();
    private int  pacienteId;
    private File imagenSeleccionada = null;  // null = solo texto, != null = análisis con imagen

    // Panel de imagen (construido dinámicamente)
    private VBox    imagenPanel;
    private Label   imagenNombreLabel;
    private ImageView imagenPreview;

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        if (Sesion.getUsuario() != null) {
            pacienteId = Sesion.getUsuario().getPacienteId();
            nombrePacienteLabel.setText("Paciente · " + Sesion.getUsuario().getUsername());
        }
        cargarHistorial();
        aiStatusLabel.setText("Describe tus síntomas y presiona «Analizar».");
        construirPanelImagen();
    }

    /**
     * Crea dinámicamente el panel de carga de imagen debajo del TextArea
     * de síntomas (sin modificar el FXML).
     */
    private void construirPanelImagen() {
        // Usamos el parent del symptomsArea para insertarlo debajo
        Pane parent = (Pane) symptomsArea.getParent();
        int indexSintomas = parent.getChildren().indexOf(symptomsArea);

        imagenPanel = new VBox(8);
        imagenPanel.setStyle(
            "-fx-background-color:#f0f9ff; -fx-background-radius:12;" +
            "-fx-border-color:#bae6fd; -fx-border-radius:12; -fx-border-width:1.5;" +
            "-fx-padding:12 16;"
        );

        // Fila superior: icono + título + botón cargar + botón quitar
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label ico = new Label("🩻");
        ico.setStyle("-fx-font-size:18px;");

        VBox textos = new VBox(2);
        HBox.setHgrow(textos, Priority.ALWAYS);
        Label titulo = new Label("Adjuntar imagen médica");
        titulo.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0369a1;");
        Label subtitulo = new Label("Radiografía · Ecografía · Examen · Foto clínica  (JPEG, PNG, máx. 20 MB)");
        subtitulo.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b;");
        textos.getChildren().addAll(titulo, subtitulo);

        Button cargarBtn = new Button("📂 Cargar");
        cargarBtn.setStyle(
            "-fx-background-color:#0284c7; -fx-text-fill:white; -fx-font-weight:bold;" +
            "-fx-background-radius:8; -fx-cursor:hand; -fx-padding:6 14; -fx-font-size:12px;"
        );
        cargarBtn.setOnAction(e -> onCargarImagen());

        Button quitarBtn = new Button("✕");
        quitarBtn.setStyle(
            "-fx-background-color:#fee2e2; -fx-text-fill:#dc2626; -fx-font-weight:bold;" +
            "-fx-background-radius:8; -fx-cursor:hand; -fx-padding:6 10; -fx-font-size:12px;"
        );
        quitarBtn.setVisible(false);
        quitarBtn.setManaged(false);

        topRow.getChildren().addAll(ico, textos, cargarBtn, quitarBtn);

        // Preview de imagen (oculto hasta que se cargue una)
        imagenPreview = new ImageView();
        imagenPreview.setFitWidth(260);
        imagenPreview.setFitHeight(160);
        imagenPreview.setPreserveRatio(true);
        imagenPreview.setStyle("-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),8,0,0,2);");
        imagenPreview.setVisible(false);
        imagenPreview.setManaged(false);

        imagenNombreLabel = new Label();
        imagenNombreLabel.setStyle(
            "-fx-font-size:11px; -fx-text-fill:#0369a1; -fx-font-style:italic;"
        );
        imagenNombreLabel.setVisible(false);
        imagenNombreLabel.setManaged(false);

        // Acción quitar imagen
        quitarBtn.setOnAction(e -> {
            imagenSeleccionada = null;
            imagenPreview.setImage(null);
            imagenPreview.setVisible(false);
            imagenPreview.setManaged(false);
            imagenNombreLabel.setVisible(false);
            imagenNombreLabel.setManaged(false);
            quitarBtn.setVisible(false);
            quitarBtn.setManaged(false);
            imagenPanel.setStyle(
                "-fx-background-color:#f0f9ff; -fx-background-radius:12;" +
                "-fx-border-color:#bae6fd; -fx-border-radius:12; -fx-border-width:1.5;" +
                "-fx-padding:12 16;"
            );
        });

        imagenPanel.getChildren().addAll(topRow, imagenPreview, imagenNombreLabel);

        // Guardar referencia al quitarBtn para activarlo al cargar
        imagenPanel.setUserData(quitarBtn);

        // Insertar el panel justo después del symptomsArea
        if (indexSintomas >= 0 && indexSintomas < parent.getChildren().size() - 1) {
            parent.getChildren().add(indexSintomas + 1, imagenPanel);
        } else {
            parent.getChildren().add(imagenPanel);
        }
    }

    /* ── Chips de síntomas ──────────────────────────────────── */

    @FXML public void onChipFiebre(ActionEvent e)   { agregarSintoma("Fiebre"); }
    @FXML public void onChipDolor(ActionEvent e)    { agregarSintoma("Dolor de cabeza"); }
    @FXML public void onChipFatiga(ActionEvent e)   { agregarSintoma("Fatiga"); }
    @FXML public void onChipNauseas(ActionEvent e)  { agregarSintoma("Náuseas"); }
    @FXML public void onChipTos(ActionEvent e)      { agregarSintoma("Tos"); }
    @FXML public void onChipDisnea(ActionEvent e)   { agregarSintoma("Dificultad para respirar"); }
    @FXML public void onChipDorsal(ActionEvent e)   { agregarSintoma("Dolor dorsal"); }
    @FXML public void onChipGarganta(ActionEvent e) { agregarSintoma("Dolor de garganta"); }

    private void agregarSintoma(String s) {
        String actual = symptomsArea.getText().trim();
        symptomsArea.setText(actual.isEmpty() ? s : actual + ", " + s);
        symptomsArea.positionCaret(symptomsArea.getText().length());
    }

    /* ── Carga de imagen ────────────────────────────────────── */

    private void onCargarImagen() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar imagen médica — CLINEX");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Imágenes médicas",
                "*.jpg", "*.jpeg", "*.png", "*.webp", "*.gif"),
            new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );

        Stage stage = (Stage) generateBtn.getScene().getWindow();
        File archivo = chooser.showOpenDialog(stage);
        if (archivo == null) return;

        // Validar tamaño (20 MB máx)
        if (archivo.length() > 20 * 1024 * 1024L) {
            mostrarAlerta("La imagen no puede superar 20 MB. Selecciona un archivo más pequeño.");
            return;
        }

        imagenSeleccionada = archivo;

        // Mostrar preview
        try {
            Image img = new Image(archivo.toURI().toString(),
                    260, 160, true, true, true);
            imagenPreview.setImage(img);
            imagenPreview.setVisible(true);
            imagenPreview.setManaged(true);
        } catch (Exception e) {
            // Si no se puede previsualizar (ej. formato raro), igual se adjunta
        }

        imagenNombreLabel.setText("📎 " + archivo.getName()
                + "  (" + (archivo.length() / 1024) + " KB)");
        imagenNombreLabel.setVisible(true);
        imagenNombreLabel.setManaged(true);

        // Activar botón quitar
        Button quitarBtn = (Button) imagenPanel.getUserData();
        quitarBtn.setVisible(true);
        quitarBtn.setManaged(true);

        // Cambiar borde a verde para indicar que hay imagen
        imagenPanel.setStyle(
            "-fx-background-color:#f0fdf4; -fx-background-radius:12;" +
            "-fx-border-color:#86efac; -fx-border-radius:12; -fx-border-width:2;" +
            "-fx-padding:12 16;"
        );

        aiStatusLabel.setText("Imagen cargada ✓ — Presiona «Analizar» para el análisis visual.");
    }

    /* ── Generar autodiagnóstico ────────────────────────────── */

    @FXML
    public void onGenerar(ActionEvent event) {
        String sintomas = symptomsArea.getText().trim();
        boolean tieneImagen = imagenSeleccionada != null;

        if (sintomas.isEmpty() && !tieneImagen) {
            mostrarAlerta("Describe tus síntomas o adjunta una imagen médica antes de continuar.");
            return;
        }
        if (pacienteId <= 0) {
            mostrarAlerta("No se encontró la cuenta de paciente asociada a esta sesión.");
            return;
        }

        generateBtn.setDisable(true);
        generateBtn.setText("⏳  Analizando...");
        aiStatusLabel.setText(tieneImagen
                ? "Analizando imagen médica con IA..."
                : "Consultando inteligencia artificial...");
        mostrarCarga();

        // Capturar referencias para el hilo
        final File imagenParaAnalisis = imagenSeleccionada;

        Task<Autodiagnostico> task = new Task<>() {
            @Override
            protected Autodiagnostico call() throws Exception {
                if (imagenParaAnalisis != null) {
                    return service.generarAutodiagnosticoConImagen(
                            pacienteId, sintomas, imagenParaAnalisis);
                } else {
                    return service.generarAutodiagnostico(pacienteId, sintomas);
                }
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            Autodiagnostico ad = task.getValue();
            renderizarResultado(ad);
            generateBtn.setDisable(false);
            generateBtn.setText("🔍  Analizar Síntomas");
            aiStatusLabel.setText("Análisis completado ✓  —  Guardado en tu historial");
            cargarHistorial();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            generateBtn.setDisable(false);
            generateBtn.setText("🔍  Analizar Síntomas");
            String msg = task.getException() != null
                    ? task.getException().getMessage() : "Error desconocido";
            aiStatusLabel.setText("Error al generar el análisis");
            mostrarErrorIA("No se pudo obtener el análisis:\n" + msg);
        }));

        new Thread(task, "autodiag-ia-thread").start();
    }

    /* ── Estado de carga ────────────────────────────────────── */

    private void mostrarCarga() {
        aiResultArea.getChildren().clear();
        aiResultArea.setAlignment(Pos.CENTER);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(56, 56);
        spinner.setStyle("-fx-accent:#2E86DE;");

        Label msg1 = new Label("Consultando inteligencia artificial...");
        msg1.getStyleClass().add("ai-output-empty");
        Label msg2 = new Label("Generando análisis clínico de tus síntomas.");
        msg2.setStyle("-fx-font-size:12px; -fx-text-fill:rgba(67,71,78,0.5);");

        aiResultArea.getChildren().addAll(spinner, msg1, msg2);
    }

    /* ── Renderizado del resultado ──────────────────────────── */

    private void renderizarResultado(Autodiagnostico ad) {
        aiResultArea.getChildren().clear();
        aiResultArea.setAlignment(Pos.TOP_LEFT);

        VBox contenido = new VBox(14);
        contenido.setPadding(new Insets(4, 4, 16, 4));

        String raw   = ad.getResultadoIa() != null ? ad.getResultadoIa() : "";
        String texto = raw.replace("\r\n", "\n").replace("\r", "\n");
        boolean esEstructurado = texto.contains("CAUSAS:") || texto.contains("GRAVEDAD:");

        // Encabezado
        HBox enc = new HBox(10);
        enc.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label("🤖");
        ico.setStyle("-fx-font-size:20px;");
        Label tit = new Label("Análisis Preliminar IA");
        tit.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#002444;");
        enc.getChildren().addAll(ico, tit);
        contenido.getChildren().add(enc);

        // Banner aviso médico
        VBox aviso = new VBox(4);
        aviso.setStyle("-fx-background-color:#FEF3C7; -fx-background-radius:8; " +
                       "-fx-border-color:#FCD34D; -fx-border-radius:8; -fx-border-width:1; " +
                       "-fx-padding:10 14;");
        Label avisotit = new Label("⚠  Aviso importante");
        avisotit.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#92400E;");
        Label aviso2 = new Label("Este análisis es orientativo. No reemplaza la consulta médica. "
                + "Si tienes síntomas graves, busca atención médica de inmediato.");
        aviso2.setStyle("-fx-font-size:11px; -fx-text-fill:#92400E; -fx-wrap-text:true;");
        aviso2.setWrapText(true);
        aviso.getChildren().addAll(avisotit, aviso2);
        contenido.getChildren().add(aviso);

        // Síntomas reportados
        contenido.getChildren().add(crearSeccion("SÍNTOMAS REPORTADOS", ad.getSintomas()));

        if (esEstructurado) {
            // Posibles causas
            List<String> causas = parsearLista(texto, "CAUSAS");
            if (!causas.isEmpty()) contenido.getChildren().add(crearSeccionCausas(causas));

            // Nivel de gravedad
            String gravedad = parsearSeccion(texto, "GRAVEDAD");
            String justif   = parsearSeccion(texto, "JUSTIFICACION");
            if (!gravedad.isEmpty()) contenido.getChildren().add(crearSeccionGravedad(gravedad, justif));

            // Recomendaciones
            List<String> recs = parsearLista(texto, "RECOMENDACIONES");
            if (!recs.isEmpty())
                contenido.getChildren().add(crearSeccionChips("RECOMENDACIONES", recs, "#DBEAFE", "#0060aa"));

            // Exámenes sugeridos
            List<String> examenes = parsearLista(texto, "EXAMENES");
            if (!examenes.isEmpty())
                contenido.getChildren().add(crearSeccionChips("EXÁMENES SUGERIDOS", examenes, "#E0F2FE", "#0284C7"));

            // Tratamiento inicial
            String trat = parsearSeccion(texto, "TRATAMIENTO");
            if (!trat.isEmpty()) contenido.getChildren().add(crearSeccion("TRATAMIENTO INICIAL", trat));

            // Próxima revisión
            String rev = parsearSeccion(texto, "REVISION");
            if (!rev.isEmpty()) contenido.getChildren().add(crearSeccionRevision(rev));

        } else {
            // Texto libre (fallback)
            contenido.getChildren().add(crearSeccion("ANÁLISIS IA", texto));
        }

        aiResultArea.getChildren().add(contenido);
    }

    private void mostrarErrorIA(String msg) {
        aiResultArea.getChildren().clear();
        aiResultArea.setAlignment(Pos.CENTER);
        Label ico = new Label("⚠");
        ico.setStyle("-fx-font-size:36px; -fx-text-fill:#F59E0B;");
        Label err = new Label(msg);
        err.getStyleClass().add("ai-output-empty");
        err.setWrapText(true);
        err.setMaxWidth(300);
        err.setStyle("-fx-text-fill:#92400E;");
        aiResultArea.getChildren().addAll(ico, err);
    }

    /* ── Historial de autodiagnósticos ──────────────────────── */

    private void cargarHistorial() {
        historialArea.getChildren().clear();

        if (pacienteId <= 0) return;

        try {
            List<Autodiagnostico> historial = service.listarPorPaciente(pacienteId);
            if (historial.isEmpty()) {
                Label empty = new Label("Aún no tienes autodiagnósticos registrados.");
                empty.setStyle("-fx-font-size:13px; -fx-text-fill:#73777f; -fx-font-style:italic;");
                historialArea.getChildren().add(empty);
                return;
            }
            historial.stream().limit(8).forEach(ad -> historialArea.getChildren().add(crearItemHistorial(ad)));
        } catch (Exception e) {
            Label err = new Label("No se pudo cargar el historial.");
            err.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");
            historialArea.getChildren().add(err);
        }
    }

    private HBox crearItemHistorial(Autodiagnostico ad) {
        HBox row = new HBox(10);
        row.getStyleClass().add("hist-item-card");
        row.setAlignment(Pos.CENTER_LEFT);

        Label fechaLabel = new Label(ad.getFechaRegistro() != null
                ? ad.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "—");
        fechaLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#2E86DE; -fx-min-width:110;");

        String sint = ad.getSintomas() != null ? ad.getSintomas() : "";
        if (sint.length() > 60) sint = sint.substring(0, 60) + "…";
        Label sintomasLabel = new Label(sint);
        sintomasLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#43474e;");
        HBox.setHgrow(sintomasLabel, Priority.ALWAYS);

        row.getChildren().addAll(fechaLabel, sintomasLabel);

        // Badge "🩻 Imagen" si tiene imagen adjunta
        if (ad.tieneImagen()) {
            Label imgBadge = new Label("🩻 Imagen");
            imgBadge.setStyle(
                "-fx-background-color:#e0f2fe; -fx-text-fill:#0369a1;" +
                "-fx-background-radius:10; -fx-padding:2 7;" +
                "-fx-font-size:10px; -fx-font-weight:bold;"
            );
            row.getChildren().add(imgBadge);
        }

        // Badge de gravedad si disponible
        String raw = ad.getResultadoIa();
        if (raw != null && raw.contains("GRAVEDAD:")) {
            String grav = parsearSeccion(raw.replace("\r\n", "\n"), "GRAVEDAD");
            if (!grav.isEmpty()) row.getChildren().add(crearGravBadge(grav));
        }
        return row;
    }

    /* ── Parseo de secciones ────────────────────────────────── */

    private String parsearSeccion(String texto, String seccion) {
        for (String linea : texto.split("\n")) {
            if (linea.trim().startsWith(seccion + ":"))
                return linea.trim().substring(seccion.length() + 1).trim();
        }
        return "";
    }

    private List<String> parsearLista(String texto, String seccion) {
        String valor = parsearSeccion(texto, seccion);
        if (valor.isEmpty()) return List.of();
        return Arrays.stream(valor.split("\\|"))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .collect(Collectors.toList());
    }

    /* ── Constructores de secciones visuales ────────────────── */

    private VBox crearSeccion(String titulo, String cuerpo) {
        VBox box = new VBox(6);
        box.getStyleClass().add("clinical-section");
        Label tit = new Label(titulo);
        tit.getStyleClass().add("detail-section-label");
        Label txt = new Label(cuerpo != null ? cuerpo : "");
        txt.setWrapText(true);
        txt.getStyleClass().add("label-body-md");
        box.getChildren().addAll(tit, txt);
        return box;
    }

    private VBox crearSeccionCausas(List<String> causas) {
        VBox box = new VBox(8);
        box.getStyleClass().add("clinical-section");
        Label tit = new Label("POSIBLES CAUSAS");
        tit.getStyleClass().add("detail-section-label");
        box.getChildren().add(tit);
        int num = 1;
        for (String causa : causas) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            Label numL = new Label(num + ".");
            numL.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#2E86DE; -fx-min-width:20;");
            Label causaL = new Label(causa);
            causaL.setWrapText(true);
            causaL.getStyleClass().add("label-body-md");
            HBox.setHgrow(causaL, Priority.ALWAYS);
            row.getChildren().addAll(numL, causaL);
            box.getChildren().add(row);
            num++;
        }
        return box;
    }

    private VBox crearSeccionGravedad(String nivel, String justificacion) {
        VBox box = new VBox(8);
        box.getStyleClass().add("clinical-section");
        Label tit = new Label("NIVEL DE GRAVEDAD");
        tit.getStyleClass().add("detail-section-label");
        box.getChildren().add(tit);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        String icono = switch (nivel.toLowerCase()) {
            case "leve"       -> "🟢";
            case "moderado"   -> "🟡";
            case "grave"      -> "🟠";
            case "emergencia" -> "🔴";
            default           -> "⚪";
        };
        Label icoL = new Label(icono);
        icoL.setStyle("-fx-font-size:20px;");
        Label nivL = new Label(nivel.toUpperCase());
        nivL.getStyleClass().add("severity-" + nivel.toLowerCase());
        row.getChildren().addAll(icoL, nivL);
        box.getChildren().add(row);

        if (!justificacion.isEmpty()) {
            Label justL = new Label(justificacion);
            justL.setWrapText(true);
            justL.setStyle("-fx-font-size:13px; -fx-text-fill:#43474e;");
            box.getChildren().add(justL);
        }
        return box;
    }

    private VBox crearSeccionChips(String titulo, List<String> items,
                                    String bgColor, String textColor) {
        VBox box = new VBox(8);
        box.getStyleClass().add("clinical-section");
        Label tit = new Label(titulo);
        tit.getStyleClass().add("detail-section-label");
        box.getChildren().add(tit);

        FlowPane chips = new FlowPane();
        chips.setHgap(8);
        chips.setVgap(8);
        for (String item : items) {
            Label chip = new Label(item);
            chip.setStyle("-fx-background-color:" + bgColor + "; -fx-background-radius:20; "
                + "-fx-border-color:rgba(0,0,0,0.08); -fx-border-radius:20; -fx-border-width:1; "
                + "-fx-padding:4 12 4 12; -fx-font-size:12px; -fx-font-weight:bold; "
                + "-fx-text-fill:" + textColor + ";");
            chips.getChildren().add(chip);
        }
        box.getChildren().add(chips);
        return box;
    }

    private VBox crearSeccionRevision(String revision) {
        VBox box = new VBox(6);
        box.setStyle("-fx-background-color:#EFF4FF; -fx-background-radius:10; " +
                     "-fx-border-color:#2E86DE; -fx-border-radius:10; -fx-border-width:1; " +
                     "-fx-padding:12 16;");
        Label tit = new Label("¿CUÁNDO CONSULTAR AL MÉDICO?");
        tit.getStyleClass().add("detail-section-label");
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label("📅");
        ico.setStyle("-fx-font-size:16px;");
        Label txt = new Label(revision);
        txt.setWrapText(true);
        txt.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#002444;");
        row.getChildren().addAll(ico, txt);
        box.getChildren().addAll(tit, row);
        return box;
    }

    private Label crearGravBadge(String nivel) {
        String[] colors = switch (nivel.toLowerCase()) {
            case "leve"       -> new String[]{"#D1FAE5", "#065F46"};
            case "moderado"   -> new String[]{"#FEF3C7", "#92400E"};
            case "grave"      -> new String[]{"#FFEDD5", "#9A3412"};
            case "emergencia" -> new String[]{"#ba1a1a", "white"};
            default           -> new String[]{"#e5e7eb", "#374151"};
        };
        Label badge = new Label(nivel);
        badge.setStyle("-fx-background-color:" + colors[0] + "; -fx-text-fill:" + colors[1]
            + "; -fx-background-radius:12; -fx-padding:2 8; "
            + "-fx-font-size:10px; -fx-font-weight:bold;");
        return badge;
    }

    /* ── Navegación ─────────────────────────────────────────── */

    @FXML
    public void onVolverPortal(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/mediconsult/beta/view/fxml/portal_paciente.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) generateBtn.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle("CLINEX — Portal del Paciente");
        } catch (IOException e) {
            mostrarAlerta("No se pudo volver al portal: " + e.getMessage());
        }
    }

    @FXML
    public void onCerrarSesion(ActionEvent event) {
        Sesion.cerrar();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/mediconsult/beta/view/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) generateBtn.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle("CLINEX — Iniciar Sesión");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setTitle("CLINEX");
        a.setHeaderText(null);
        a.showAndWait();
    }
}
