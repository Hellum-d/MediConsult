package com.mediconsult.beta.controller;

import com.mediconsult.beta.model.*;
import com.mediconsult.beta.service.*;
import com.mediconsult.beta.util.NotificacionUtil;
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
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConsultaMedicaController {

    /* ── Contexto de la consulta (inyectado por PanelMedicoController) ── */

    private int citaIdActual;
    private int pacienteIdActual;

    public void inicializar(int citaId, int pacienteId) {
        this.citaIdActual     = citaId;
        this.pacienteIdActual = pacienteId;
        cargarTodo();
    }

    /* ── Inyección FXML — Panel izquierdo ───────────────────── */

    @FXML private Label nombrePacienteLabel;
    @FXML private Label edadLabel;
    @FXML private Label tipoSangreLabel;
    @FXML private Label alergiasLabel;
    @FXML private Label correoPacienteLabel;
    @FXML private VBox  autodiagHistorialView;
    @FXML private VBox  historiasAnterioresView;

    /* ── Inyección FXML — Panel derecho (formulario) ─────────── */

    @FXML private Label    infoCitaLabel;
    @FXML private TextArea anamnesisArea;
    @FXML private TextArea examenFisicoArea;
    @FXML private TextField diagnosticoField;
    @FXML private TextArea tratamientoArea;
    @FXML private TextArea observacionesArea;
    @FXML private VBox     ordenesView;
    @FXML private Button   nuevaOrdenBtn;
    @FXML private VBox     remisionesView;
    @FXML private Button   nuevaRemisionBtn;
    @FXML private Label    statusLabel;
    @FXML private Button   guardarBtn;
    @FXML private Button   completarBtn;

    /* ── Servicios ──────────────────────────────────────────── */

    private final CitaService            citaService     = new CitaService();
    private final PacienteService        pacienteService = new PacienteService();
    private final AutodiagnosticoService autodiagService = new AutodiagnosticoService();
    private final HistoriaClinicaService historiaService = new HistoriaClinicaService();
    private final OrdenMedicaService     ordenService    = new OrdenMedicaService();
    private final RemisionService        remisionService = new RemisionService();

    private HistoriaClinica historiaActual;

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        // Los datos se cargan en inicializar() tras inyectar citaId y pacienteId
    }

    private void cargarTodo() {
        cargarDatosPaciente();
        cargarAutodiagnosticos();
        cargarHistoriasAnteriores();
        cargarHistoriaExistente();
        mostrarInfoCita();
        cargarOrdenes();
        cargarRemisiones();
    }

    private void cargarDatosPaciente() {
        try {
            Optional<Paciente> opt = pacienteService.obtenerPorId(pacienteIdActual);
            if (opt.isPresent()) {
                Paciente p = opt.get();
                nombrePacienteLabel.setText(p.getNombreCompleto());
                edadLabel.setText(p.getEdad() + " años");
                tipoSangreLabel.setText(p.getTipoSangre() != null && !p.getTipoSangre().isBlank()
                        ? p.getTipoSangre() : "No registrado");
                alergiasLabel.setText(p.getAlergias() != null && !p.getAlergias().isBlank()
                        ? p.getAlergias() : "Ninguna");
                correoPacienteLabel.setText(p.getCorreo() != null && !p.getCorreo().isBlank()
                        ? p.getCorreo() : "—");
            } else {
                nombrePacienteLabel.setText("Paciente ID " + pacienteIdActual);
            }
        } catch (Exception e) {
            nombrePacienteLabel.setText("Error al cargar datos");
        }
    }

    private void cargarAutodiagnosticos() {
        autodiagHistorialView.getChildren().clear();
        try {
            List<Autodiagnostico> lista = autodiagService.listarPorPaciente(pacienteIdActual);
            if (lista.isEmpty()) {
                Label empty = new Label("Sin autodiagnósticos previos.");
                empty.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f; -fx-font-style:italic;");
                autodiagHistorialView.getChildren().add(empty);
                return;
            }
            lista.stream().limit(3).forEach(ad ->
                autodiagHistorialView.getChildren().add(crearTarjetaAutodiag(ad))
            );
        } catch (Exception e) {
            Label err = new Label("Error al cargar autodiagnósticos.");
            err.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");
            autodiagHistorialView.getChildren().add(err);
        }
    }

    private VBox crearTarjetaAutodiag(Autodiagnostico ad) {
        VBox card = new VBox(6);
        card.setStyle(
            "-fx-background-color:#f0f6ff; -fx-background-radius:10;" +
            "-fx-border-color:rgba(46,134,222,0.3); -fx-border-radius:10;" +
            "-fx-border-width:1; -fx-padding:10 12;"
        );

        // ── Fila superior: fecha + badges ─────────────────────
        HBox topRow = new HBox(6);
        topRow.setAlignment(Pos.CENTER_LEFT);

        String fecha = ad.getFechaRegistro() != null
                ? ad.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";
        Label fechaL = new Label("🤖  " + fecha);
        fechaL.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#2E86DE;");
        HBox.setHgrow(fechaL, Priority.ALWAYS);

        // Badge imagen
        if (ad.tieneImagen()) {
            Label imgBadge = new Label("🩻 Imagen");
            imgBadge.setStyle(
                "-fx-background-color:#e0f2fe; -fx-text-fill:#0369a1;" +
                "-fx-background-radius:10; -fx-padding:2 7;" +
                "-fx-font-size:10px; -fx-font-weight:bold;"
            );
            topRow.getChildren().add(imgBadge);
        }

        // Badge gravedad
        String raw = ad.getResultadoIa() != null
                ? ad.getResultadoIa().replace("\r\n", "\n") : "";
        String grav = parsearSeccion(raw, "GRAVEDAD");
        if (!grav.isEmpty()) {
            Label gravBadge = new Label(getGravIcon(grav) + " " + grav.toUpperCase());
            gravBadge.setStyle(getGravStyle(grav));
            topRow.getChildren().add(gravBadge);
        }

        topRow.getChildren().add(0, fechaL);

        // ── Síntomas reportados ────────────────────────────────
        String sint = ad.getSintomas() != null ? ad.getSintomas() : "";
        if (sint.length() > 100) sint = sint.substring(0, 100) + "…";
        Label sintL = new Label(sint);
        sintL.setStyle("-fx-font-size:12px; -fx-text-fill:#43474e; -fx-wrap-text:true;");
        sintL.setWrapText(true);

        // ── Causas resumidas ───────────────────────────────────
        String causasRaw = parsearSeccion(raw, "CAUSAS");
        VBox causasBox = new VBox(2);
        if (!causasRaw.isEmpty()) {
            Label causasTit = new Label("POSIBLES CAUSAS");
            causasTit.setStyle("-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#0060aa;");
            List<String> causas = Arrays.stream(causasRaw.split("\\|"))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .limit(3).collect(Collectors.toList());
            VBox listaC = new VBox(1);
            for (String c : causas) {
                Label cl = new Label("• " + c);
                cl.setStyle("-fx-font-size:11px; -fx-text-fill:#0b1c30; -fx-wrap-text:true;");
                cl.setWrapText(true);
                listaC.getChildren().add(cl);
            }
            causasBox.getChildren().addAll(causasTit, listaC);
        }

        // ── Exámenes sugeridos ─────────────────────────────────
        String examRaw = parsearSeccion(raw, "EXAMENES");
        VBox examBox = new VBox(2);
        if (!examRaw.isEmpty()) {
            Label examTit = new Label("EXÁMENES SUGERIDOS");
            examTit.setStyle("-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#065f46;");
            FlowPane chips = new FlowPane();
            chips.setHgap(4); chips.setVgap(4);
            Arrays.stream(examRaw.split("\\|")).map(String::trim)
                    .filter(s -> !s.isEmpty()).limit(3).forEach(ex -> {
                Label chip = new Label(ex);
                chip.setStyle(
                    "-fx-background-color:#d1fae5; -fx-background-radius:8;" +
                    "-fx-padding:2 7; -fx-font-size:10px; -fx-text-fill:#065f46;" +
                    "-fx-font-weight:bold;"
                );
                chips.getChildren().add(chip);
            });
            examBox.getChildren().addAll(examTit, chips);
        }

        // ── Botón "Ver análisis completo" ──────────────────────
        Button verBtn = new Button("📋  Ver análisis completo");
        verBtn.setStyle(
            "-fx-background-color:#2E86DE; -fx-text-fill:white; -fx-font-weight:bold;" +
            "-fx-background-radius:8; -fx-cursor:hand; -fx-padding:5 12; -fx-font-size:11px;"
        );
        verBtn.setMaxWidth(Double.MAX_VALUE);
        verBtn.setOnAction(e -> mostrarDetalleAutodiagnostico(ad));

        card.getChildren().addAll(topRow, sintL);
        if (!causasBox.getChildren().isEmpty()) card.getChildren().add(causasBox);
        if (!examBox.getChildren().isEmpty())   card.getChildren().add(examBox);
        card.getChildren().add(verBtn);

        return card;
    }

    /** Abre un modal con el análisis IA completo + imagen si existe. */
    private void mostrarDetalleAutodiagnostico(Autodiagnostico ad) {
        Stage owner = (Stage) guardarBtn.getScene().getWindow();
        Stage modal = new Stage();
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.initOwner(owner);
        modal.initModality(Modality.APPLICATION_MODAL);

        String raw = ad.getResultadoIa() != null
                ? ad.getResultadoIa().replace("\r\n", "\n") : "";
        String fecha = ad.getFechaRegistro() != null
                ? ad.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";

        // ── Contenido del scroll ───────────────────────────────
        VBox contenido = new VBox(14);
        contenido.setPadding(new Insets(4));

        // Encabezado
        HBox enc = new HBox(10);
        enc.setAlignment(Pos.CENTER_LEFT);
        Label icoL = new Label("🤖");
        icoL.setStyle("-fx-font-size:22px;");
        VBox encTxt = new VBox(2);
        Label titL = new Label("Autodiagnóstico IA — " + fecha);
        titL.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#002444;");
        Label subL = new Label("Análisis previo a la consulta. Úsalo como referencia clínica.");
        subL.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f;");
        encTxt.getChildren().addAll(titL, subL);
        HBox.setHgrow(encTxt, Priority.ALWAYS);
        enc.getChildren().addAll(icoL, encTxt);
        contenido.getChildren().add(enc);

        // ── Imagen si existe ───────────────────────────────────
        if (ad.tieneImagen()) {
            File imgFile = new File(ad.getImagenPath());
            if (imgFile.exists()) {
                VBox imgSection = new VBox(6);
                imgSection.setStyle(
                    "-fx-background-color:#f0f9ff; -fx-background-radius:10;" +
                    "-fx-border-color:#bae6fd; -fx-border-radius:10;" +
                    "-fx-border-width:1.5; -fx-padding:10 12;"
                );
                Label imgTit = new Label("🩻  IMAGEN MÉDICA ADJUNTA");
                imgTit.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#0369a1;");
                Label imgNom = new Label(imgFile.getName()
                        + "  (" + (imgFile.length() / 1024) + " KB)");
                imgNom.setStyle("-fx-font-size:10px; -fx-text-fill:#64748b; -fx-font-style:italic;");

                ImageView imgView = new ImageView();
                try {
                    Image img = new Image(imgFile.toURI().toString(),
                            420, 300, true, true, true);
                    imgView.setImage(img);
                    imgView.setFitWidth(420);
                    imgView.setFitHeight(300);
                    imgView.setPreserveRatio(true);
                    imgView.setStyle("-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),8,0,0,2);");
                } catch (Exception ignored) {
                    imgNom.setText(imgFile.getName() + " (no se pudo previsualizar)");
                }

                imgSection.getChildren().addAll(imgTit, imgNom, imgView);
                contenido.getChildren().add(imgSection);
            }
        }

        // ── Síntomas reportados ────────────────────────────────
        contenido.getChildren().add(crearSeccionModal("SÍNTOMAS REPORTADOS",
                ad.getSintomas() != null ? ad.getSintomas() : "—", "#002444"));

        boolean estructurado = raw.contains("CAUSAS:") || raw.contains("GRAVEDAD:");

        if (estructurado) {
            // Gravedad
            String grav    = parsearSeccion(raw, "GRAVEDAD");
            String justif  = parsearSeccion(raw, "JUSTIFICACION");
            if (!grav.isEmpty()) {
                VBox gravBox = new VBox(6);
                gravBox.setStyle(getGravBgStyle(grav));
                Label gravTit = new Label("NIVEL DE GRAVEDAD");
                gravTit.setStyle("-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#555;");
                Label gravVal = new Label(getGravIcon(grav) + "  " + grav.toUpperCase());
                gravVal.setStyle("-fx-font-size:16px; -fx-font-weight:bold; " + getGravTextColor(grav));
                gravBox.getChildren().addAll(gravTit, gravVal);
                if (!justif.isEmpty()) {
                    Label justL = new Label(justif);
                    justL.setStyle("-fx-font-size:12px; -fx-text-fill:#43474e; -fx-wrap-text:true;");
                    justL.setWrapText(true);
                    gravBox.getChildren().add(justL);
                }
                contenido.getChildren().add(gravBox);
            }

            // Causas
            String causas = parsearSeccion(raw, "CAUSAS");
            if (!causas.isEmpty())
                contenido.getChildren().add(crearSeccionLista("POSIBLES CAUSAS", causas, "#0060aa"));

            // Recomendaciones
            String recs = parsearSeccion(raw, "RECOMENDACIONES");
            if (!recs.isEmpty())
                contenido.getChildren().add(crearSeccionLista("RECOMENDACIONES", recs, "#0369a1"));

            // Exámenes
            String exams = parsearSeccion(raw, "EXAMENES");
            if (!exams.isEmpty())
                contenido.getChildren().add(crearSeccionChipsModal("EXÁMENES SUGERIDOS",
                        exams, "#d1fae5", "#065f46"));

            // Tratamiento
            String trat = parsearSeccion(raw, "TRATAMIENTO");
            if (!trat.isEmpty())
                contenido.getChildren().add(crearSeccionModal("TRATAMIENTO INICIAL", trat, "#0b1c30"));

            // Observaciones visuales (hallazgos de imagen)
            String obs = parsearSeccion(raw, "OBSERVACIONES");
            if (!obs.isEmpty())
                contenido.getChildren().add(crearSeccionModal("OBSERVACIONES CLÍNICAS / HALLAZGOS", obs, "#0b1c30"));

            // Evolución
            String evol = parsearSeccion(raw, "EVOLUCION");
            if (!evol.isEmpty())
                contenido.getChildren().add(crearSeccionModal("EVOLUCIÓN ESPERADA", evol, "#0b1c30"));

            // Próxima revisión
            String rev = parsearSeccion(raw, "REVISION");
            if (!rev.isEmpty()) {
                VBox revBox = new VBox(4);
                revBox.setStyle(
                    "-fx-background-color:#EFF4FF; -fx-background-radius:10;" +
                    "-fx-border-color:#2E86DE; -fx-border-radius:10; -fx-border-width:1;" +
                    "-fx-padding:10 14;"
                );
                Label revTit = new Label("📅  ¿CUÁNDO CONSULTAR AL MÉDICO?");
                revTit.setStyle("-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#2E86DE;");
                Label revVal = new Label(rev);
                revVal.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#002444; -fx-wrap-text:true;");
                revVal.setWrapText(true);
                revBox.getChildren().addAll(revTit, revVal);
                contenido.getChildren().add(revBox);
            }

        } else {
            contenido.getChildren().add(crearSeccionModal("ANÁLISIS IA", raw, "#0b1c30"));
        }

        // ── Aviso ──────────────────────────────────────────────
        Label aviso = new Label(
            "⚠  Este análisis fue generado por IA y es orientativo. "
            + "Úsalo como referencia para la historia clínica. No reemplaza el criterio médico."
        );
        aviso.setStyle(
            "-fx-font-size:11px; -fx-text-fill:#92400E; -fx-wrap-text:true;" +
            "-fx-background-color:#FEF3C7; -fx-background-radius:8; -fx-padding:8 12;"
        );
        aviso.setWrapText(true);
        contenido.getChildren().add(aviso);

        // ── Scroll y botón cerrar ──────────────────────────────
        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color:transparent; -fx-background:transparent;" +
                        "-fx-border-color:transparent; -fx-max-height:520;");

        Button cerrarBtn = new Button("✕  Cerrar");
        cerrarBtn.setStyle(
            "-fx-background-color:#002444; -fx-text-fill:white; -fx-font-weight:bold;" +
            "-fx-background-radius:10; -fx-padding:9 24; -fx-cursor:hand;"
        );
        cerrarBtn.setOnAction(e -> modal.close());

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 0, 0, 0));
        footer.getChildren().add(cerrarBtn);

        VBox card = new VBox(0);
        card.setStyle(
            "-fx-background-color:#ffffff; -fx-background-radius:16;" +
            "-fx-padding:24; -fx-max-width:520; -fx-min-width:480;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.25),28,0,0,6);"
        );
        card.getChildren().addAll(scroll, footer);

        StackPane overlay = new StackPane(card);
        overlay.setStyle("-fx-background-color:rgba(0,36,68,0.6);");
        StackPane.setAlignment(card, Pos.CENTER);
        overlay.setOnMouseClicked(ev -> { if (ev.getTarget() == overlay) modal.close(); });

        Scene scene = new Scene(overlay, owner.getWidth(), owner.getHeight());
        scene.setFill(Color.TRANSPARENT);
        modal.setScene(scene);
        modal.show();
    }

    // ── Helpers para el modal de autodiagnóstico ───────────────────────────

    private VBox crearSeccionModal(String titulo, String cuerpo, String titleColor) {
        VBox box = new VBox(4);
        box.setStyle(
            "-fx-background-color:#f8f9ff; -fx-background-radius:8;" +
            "-fx-border-color:rgba(46,134,222,0.15); -fx-border-radius:8;" +
            "-fx-border-width:1; -fx-padding:8 12;"
        );
        Label tit = new Label(titulo);
        tit.setStyle("-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:" + titleColor + ";");
        Label txt = new Label(cuerpo);
        txt.setStyle("-fx-font-size:13px; -fx-text-fill:#0b1c30; -fx-wrap-text:true;");
        txt.setWrapText(true);
        box.getChildren().addAll(tit, txt);
        return box;
    }

    private VBox crearSeccionLista(String titulo, String pipeSeparated, String titleColor) {
        VBox box = new VBox(5);
        box.setStyle(
            "-fx-background-color:#f8f9ff; -fx-background-radius:8;" +
            "-fx-border-color:rgba(46,134,222,0.15); -fx-border-radius:8;" +
            "-fx-border-width:1; -fx-padding:8 12;"
        );
        Label tit = new Label(titulo);
        tit.setStyle("-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:" + titleColor + ";");
        box.getChildren().add(tit);
        int num = 1;
        for (String item : pipeSeparated.split("\\|")) {
            String s = item.trim();
            if (s.isEmpty()) continue;
            HBox row = new HBox(6);
            row.setAlignment(Pos.TOP_LEFT);
            Label numL = new Label(num + ".");
            numL.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#2E86DE; -fx-min-width:16;");
            Label valL = new Label(s);
            valL.setStyle("-fx-font-size:13px; -fx-text-fill:#0b1c30; -fx-wrap-text:true;");
            valL.setWrapText(true);
            HBox.setHgrow(valL, Priority.ALWAYS);
            row.getChildren().addAll(numL, valL);
            box.getChildren().add(row);
            num++;
        }
        return box;
    }

    private VBox crearSeccionChipsModal(String titulo, String pipeSeparated,
                                         String bgColor, String fgColor) {
        VBox box = new VBox(6);
        box.setStyle(
            "-fx-background-color:#f8f9ff; -fx-background-radius:8;" +
            "-fx-border-color:rgba(46,134,222,0.15); -fx-border-radius:8;" +
            "-fx-border-width:1; -fx-padding:8 12;"
        );
        Label tit = new Label(titulo);
        tit.setStyle("-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:" + fgColor + ";");
        FlowPane chips = new FlowPane();
        chips.setHgap(6); chips.setVgap(6);
        Arrays.stream(pipeSeparated.split("\\|")).map(String::trim)
                .filter(s -> !s.isEmpty()).forEach(s -> {
            Label chip = new Label(s);
            chip.setStyle("-fx-background-color:" + bgColor + "; -fx-background-radius:12;" +
                    "-fx-padding:3 10; -fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:" + fgColor + ";");
            chips.getChildren().add(chip);
        });
        box.getChildren().addAll(tit, chips);
        return box;
    }

    private String getGravIcon(String grav) {
        return switch (grav.toLowerCase()) {
            case "leve"       -> "🟢";
            case "moderado"   -> "🟡";
            case "grave"      -> "🟠";
            case "emergencia" -> "🔴";
            default           -> "⚪";
        };
    }

    private String getGravStyle(String grav) {
        return switch (grav.toLowerCase()) {
            case "leve"       -> "-fx-background-color:#D1FAE5; -fx-text-fill:#065F46; -fx-background-radius:10; -fx-padding:2 8; -fx-font-size:10px; -fx-font-weight:bold;";
            case "moderado"   -> "-fx-background-color:#FEF3C7; -fx-text-fill:#92400E; -fx-background-radius:10; -fx-padding:2 8; -fx-font-size:10px; -fx-font-weight:bold;";
            case "grave"      -> "-fx-background-color:#FFEDD5; -fx-text-fill:#9A3412; -fx-background-radius:10; -fx-padding:2 8; -fx-font-size:10px; -fx-font-weight:bold;";
            case "emergencia" -> "-fx-background-color:#ba1a1a; -fx-text-fill:white; -fx-background-radius:10; -fx-padding:2 8; -fx-font-size:10px; -fx-font-weight:bold;";
            default           -> "-fx-background-color:#e0e0e0; -fx-text-fill:#374151; -fx-background-radius:10; -fx-padding:2 8; -fx-font-size:10px;";
        };
    }

    private String getGravBgStyle(String grav) {
        String bg = switch (grav.toLowerCase()) {
            case "leve"       -> "#f0fdf4";
            case "moderado"   -> "#fffbeb";
            case "grave"      -> "#fff7ed";
            case "emergencia" -> "#fff1f2";
            default           -> "#f8f9ff";
        };
        String border = switch (grav.toLowerCase()) {
            case "leve"       -> "#86efac";
            case "moderado"   -> "#fcd34d";
            case "grave"      -> "#fdba74";
            case "emergencia" -> "#fca5a5";
            default           -> "#e0e0e0";
        };
        return "-fx-background-color:" + bg + "; -fx-background-radius:10;" +
               "-fx-border-color:" + border + "; -fx-border-radius:10;" +
               "-fx-border-width:1.5; -fx-padding:10 12;";
    }

    private String getGravTextColor(String grav) {
        return switch (grav.toLowerCase()) {
            case "leve"       -> "-fx-text-fill:#065F46;";
            case "moderado"   -> "-fx-text-fill:#92400E;";
            case "grave"      -> "-fx-text-fill:#9A3412;";
            case "emergencia" -> "-fx-text-fill:#ba1a1a;";
            default           -> "-fx-text-fill:#374151;";
        };
    }

    private void cargarHistoriasAnteriores() {
        historiasAnterioresView.getChildren().clear();
        try {
            List<HistoriaClinica> lista = historiaService.listarPorPaciente(pacienteIdActual)
                    .stream().filter(h -> h.getCitaId() != citaIdActual).limit(3).toList();
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

    private void cargarHistoriaExistente() {
        try {
            Optional<HistoriaClinica> opt = historiaService.obtenerPorCita(citaIdActual);
            if (opt.isPresent()) {
                historiaActual = opt.get();
                anamnesisArea.setText(historiaActual.getAnamnesis());
                examenFisicoArea.setText(historiaActual.getExamenFisico());
                diagnosticoField.setText(historiaActual.getDiagnostico());
                tratamientoArea.setText(historiaActual.getTratamiento());
                observacionesArea.setText(historiaActual.getObservaciones());
                statusLabel.setText("Historia clínica cargada — guardado el "
                        + (historiaActual.getFechaRegistro() != null
                                ? historiaActual.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                : "—"));
            }
        } catch (Exception e) {
            statusLabel.setText("No se pudo cargar historia previa.");
        }
    }

    private void mostrarInfoCita() {
        try {
            Optional<Cita> opt = citaService.obtenerPorId(citaIdActual);
            if (opt.isPresent()) {
                Cita c = opt.get();
                String fecha = c.getFecha() != null
                        ? c.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
                infoCitaLabel.setText("Cita #" + citaIdActual + "  ·  " + fecha
                        + "  ·  " + (c.getHora() != null ? c.getHora() : "")
                        + "  ·  Estado: " + c.getEstado());
                completarBtn.setDisable(Cita.ESTADO_COMPLETADA.equals(c.getEstado())
                        || Cita.ESTADO_CANCELADA.equals(c.getEstado()));
            }
        } catch (Exception e) {
            infoCitaLabel.setText("Cita #" + citaIdActual);
        }
    }

    /* ── Órdenes Médicas ────────────────────────────────────── */

    private void cargarOrdenes() {
        ordenesView.getChildren().clear();
        try {
            List<OrdenMedica> ordenes = ordenService.listarPorCita(citaIdActual);
            if (ordenes.isEmpty()) {
                Label empty = new Label("Sin órdenes médicas registradas.");
                empty.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f; -fx-font-style:italic;");
                ordenesView.getChildren().add(empty);
                return;
            }
            for (OrdenMedica o : ordenes) {
                ordenesView.getChildren().add(crearTarjetaOrden(o));
            }
        } catch (Exception e) {
            Label err = new Label("Error al cargar órdenes: " + e.getMessage());
            err.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");
            ordenesView.getChildren().add(err);
        }
    }

    private VBox crearTarjetaOrden(OrdenMedica orden) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:10; " +
                      "-fx-border-color:rgba(46,134,222,0.2); -fx-border-radius:10; " +
                      "-fx-border-width:1; -fx-padding:10 14;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label tipoBadge = new Label(getTipoLabel(orden.getTipo()));
        tipoBadge.setStyle(getTipoStyle(orden.getTipo()));

        String fecha = orden.getFechaRegistro() != null
                ? orden.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";
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
            case OrdenMedica.TIPO_MEDICAMENTO   -> "💊  Medicamento";
            case OrdenMedica.TIPO_PROCEDIMIENTO -> "🩺  Procedimiento";
            case OrdenMedica.TIPO_EXAMEN        -> "🔬  Examen";
            default -> tipo;
        };
    }

    private String getTipoStyle(String tipo) {
        if (tipo == null) return "";
        return switch (tipo) {
            case OrdenMedica.TIPO_MEDICAMENTO ->
                "-fx-background-color:#DBEAFE; -fx-background-radius:12; -fx-padding:3 10; " +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1E40AF;";
            case OrdenMedica.TIPO_PROCEDIMIENTO ->
                "-fx-background-color:#EDE9FE; -fx-background-radius:12; -fx-padding:3 10; " +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#5B21B6;";
            case OrdenMedica.TIPO_EXAMEN ->
                "-fx-background-color:#D1FAE5; -fx-background-radius:12; -fx-padding:3 10; " +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#065F46;";
            default ->
                "-fx-background-color:#e0e0e0; -fx-background-radius:12; -fx-padding:3 10; " +
                "-fx-font-size:11px;";
        };
    }

    @FXML
    public void onNuevaOrden(ActionEvent event) {
        Stage owner = (Stage) nuevaOrdenBtn.getScene().getWindow();
        Stage modal = new Stage();
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.initOwner(owner);
        modal.initModality(Modality.APPLICATION_MODAL);

        /* ── Controles del formulario ── */
        ComboBox<String> tipoCombo = new ComboBox<>();
        tipoCombo.getItems().addAll(
                OrdenMedica.TIPO_MEDICAMENTO,
                OrdenMedica.TIPO_PROCEDIMIENTO,
                OrdenMedica.TIPO_EXAMEN);
        tipoCombo.setPromptText("Seleccionar tipo...");
        tipoCombo.setMaxWidth(Double.MAX_VALUE);
        tipoCombo.setStyle("-fx-background-radius:8; -fx-border-radius:8; " +
                           "-fx-border-color:rgba(46,134,222,0.3); -fx-font-size:13px;");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Describe la orden médica en detalle...");
        descArea.setPrefRowCount(4);
        descArea.setWrapText(true);
        descArea.setStyle("-fx-background-radius:8; -fx-border-color:rgba(46,134,222,0.3); " +
                          "-fx-border-radius:8; -fx-font-size:13px; -fx-padding:8;");

        TextArea obsArea = new TextArea();
        obsArea.setPromptText("Observaciones adicionales (opcional)...");
        obsArea.setPrefRowCount(2);
        obsArea.setWrapText(true);
        obsArea.setStyle("-fx-background-radius:8; -fx-border-color:rgba(46,134,222,0.3); " +
                         "-fx-border-radius:8; -fx-font-size:13px; -fx-padding:8;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");

        Button guardarModalBtn = new Button("Guardar Orden");
        guardarModalBtn.setStyle("-fx-background-color:#002444; -fx-text-fill:white; " +
                                 "-fx-font-weight:bold; -fx-background-radius:10; " +
                                 "-fx-padding:10 24; -fx-cursor:hand; -fx-font-size:13px;");

        Button cancelarBtn = new Button("Cancelar");
        cancelarBtn.setStyle("-fx-background-color:transparent; " +
                             "-fx-border-color:rgba(46,134,222,0.35); -fx-border-radius:10; " +
                             "-fx-background-radius:10; -fx-text-fill:#002444; " +
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
            OrdenMedica orden = new OrdenMedica(citaIdActual, pacienteIdActual, medicoId);
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

        /* ── Layout de la tarjeta modal ── */
        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.getChildren().addAll(cancelarBtn, guardarModalBtn);

        Label titleL = new Label("Nueva Orden Médica");
        titleL.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");

        Label lblTipo = new Label("TIPO DE ORDEN");
        lblTipo.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#2E86DE;");

        Label lblDesc = new Label("DESCRIPCIÓN");
        lblDesc.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#2E86DE;");

        Label lblObs = new Label("OBSERVACIONES");
        lblObs.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#2E86DE;");

        VBox card = new VBox(12);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:16; " +
                      "-fx-padding:28; -fx-max-width:480; -fx-effect:dropshadow(gaussian,rgba(0,36,68,0.22),24,0,0,6);");
        card.getChildren().addAll(titleL, lblTipo, tipoCombo, lblDesc, descArea, lblObs, obsArea, errorLabel, btnRow);
        VBox.setMargin(btnRow, new Insets(4, 0, 0, 0));

        StackPane overlay = new StackPane(card);
        overlay.setStyle("-fx-background-color:rgba(0,36,68,0.55);");
        StackPane.setAlignment(card, Pos.CENTER);

        Scene scene = new Scene(overlay, owner.getWidth(), owner.getHeight());
        scene.setFill(Color.TRANSPARENT);
        modal.setScene(scene);
        modal.show();
    }

    /* ── Remisión a Especialista ────────────────────────────── */

    private static final List<String> ESPECIALIDADES = List.of(
        "Cardiología", "Neurología", "Ortopedia", "Dermatología",
        "Gastroenterología", "Neumología", "Endocrinología", "Psiquiatría",
        "Oftalmología", "Otorrinolaringología", "Urología", "Ginecología",
        "Oncología", "Hematología", "Infectología", "Reumatología",
        "Nefrología", "Cirugía General", "Medicina Interna", "Otra"
    );

    private void cargarRemisiones() {
        remisionesView.getChildren().clear();
        try {
            List<Remision> remisiones = remisionService.listarPorCita(citaIdActual);
            if (remisiones.isEmpty()) {
                Label empty = new Label("Sin remisiones registradas.");
                empty.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f; -fx-font-style:italic;");
                remisionesView.getChildren().add(empty);
                return;
            }
            for (Remision r : remisiones) {
                remisionesView.getChildren().add(crearTarjetaRemision(r));
            }
        } catch (Exception e) {
            Label err = new Label("Error al cargar remisiones: " + e.getMessage());
            err.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");
            remisionesView.getChildren().add(err);
        }
    }

    private VBox crearTarjetaRemision(Remision r) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:10; " +
                      "-fx-border-color:rgba(26,58,92,0.25); -fx-border-radius:10; " +
                      "-fx-border-width:1; -fx-padding:10 14;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label espBadge = new Label("🔗  " + r.getEspecialidadDestino());
        espBadge.setStyle("-fx-background-color:#EFF6FF; -fx-background-radius:12; -fx-padding:3 10; " +
                          "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1E3A5F;");

        Label estadoBadge = new Label(getEstadoRemisionLabel(r.getEstado()));
        estadoBadge.setStyle(getEstadoRemisionStyle(r.getEstado()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String fecha = r.getFechaRegistro() != null
                ? r.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
        Label fechaL = new Label(fecha);
        fechaL.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f;");

        Button elimBtn = new Button("×");
        elimBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#ba1a1a; " +
                         "-fx-font-size:16px; -fx-cursor:hand; -fx-padding:0 4;");
        elimBtn.setOnAction(e -> {
            try {
                remisionService.eliminar(r.getId());
                cargarRemisiones();
            } catch (Exception ex) {
                statusLabel.setText("⚠  Error al eliminar remisión: " + ex.getMessage());
            }
        });

        header.getChildren().addAll(espBadge, estadoBadge, spacer, fechaL, elimBtn);

        String motivo = r.getMotivoRemision() != null ? r.getMotivoRemision() : "";
        Label motivoL = new Label(motivo);
        motivoL.setStyle("-fx-font-size:13px; -fx-text-fill:#0b1c30; -fx-wrap-text:true;");
        motivoL.setWrapText(true);

        card.getChildren().addAll(header, motivoL);

        if (r.getObservaciones() != null && !r.getObservaciones().isBlank()) {
            Label obsL = new Label("Obs: " + r.getObservaciones());
            obsL.setStyle("-fx-font-size:12px; -fx-text-fill:#43474e; " +
                          "-fx-font-style:italic; -fx-wrap-text:true;");
            obsL.setWrapText(true);
            card.getChildren().add(obsL);
        }

        return card;
    }

    private String getEstadoRemisionLabel(String estado) {
        if (estado == null) return "PENDIENTE";
        return switch (estado) {
            case Remision.ESTADO_PENDIENTE   -> "⏳ Pendiente";
            case Remision.ESTADO_ACEPTADA    -> "✓ Aceptada";
            case Remision.ESTADO_EN_ATENCION -> "⚕ En atención";
            case Remision.ESTADO_COMPLETADA  -> "✔ Completada";
            case Remision.ESTADO_CANCELADA   -> "✗ Cancelada";
            default -> estado;
        };
    }

    private String getEstadoRemisionStyle(String estado) {
        if (estado == null) return "";
        return switch (estado) {
            case Remision.ESTADO_PENDIENTE ->
                "-fx-background-color:#FEF3C7; -fx-background-radius:10; -fx-padding:2 8; " +
                "-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#92400E;";
            case Remision.ESTADO_ACEPTADA ->
                "-fx-background-color:#D1FAE5; -fx-background-radius:10; -fx-padding:2 8; " +
                "-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#065F46;";
            case Remision.ESTADO_EN_ATENCION ->
                "-fx-background-color:#DBEAFE; -fx-background-radius:10; -fx-padding:2 8; " +
                "-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#1E40AF;";
            case Remision.ESTADO_COMPLETADA ->
                "-fx-background-color:#E0E7FF; -fx-background-radius:10; -fx-padding:2 8; " +
                "-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#3730A3;";
            case Remision.ESTADO_CANCELADA ->
                "-fx-background-color:#FEE2E2; -fx-background-radius:10; -fx-padding:2 8; " +
                "-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#991B1B;";
            default ->
                "-fx-background-color:#e0e0e0; -fx-background-radius:10; -fx-padding:2 8; -fx-font-size:10px;";
        };
    }

    @FXML
    public void onNuevaRemision(ActionEvent event) {
        Stage owner = (Stage) nuevaRemisionBtn.getScene().getWindow();
        Stage modal = new Stage();
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.initOwner(owner);
        modal.initModality(Modality.APPLICATION_MODAL);

        ComboBox<String> espCombo = new ComboBox<>();
        espCombo.getItems().addAll(ESPECIALIDADES);
        espCombo.setPromptText("Seleccionar especialidad...");
        espCombo.setMaxWidth(Double.MAX_VALUE);
        espCombo.setStyle("-fx-background-radius:8; -fx-border-radius:8; " +
                          "-fx-border-color:rgba(26,58,92,0.3); -fx-font-size:13px;");

        TextArea motivoArea = new TextArea();
        motivoArea.setPromptText("Describe el motivo clínico de la remisión...");
        motivoArea.setPrefRowCount(4);
        motivoArea.setWrapText(true);
        motivoArea.setStyle("-fx-background-radius:8; -fx-border-color:rgba(26,58,92,0.3); " +
                            "-fx-border-radius:8; -fx-font-size:13px; -fx-padding:8;");

        TextArea obsArea = new TextArea();
        obsArea.setPromptText("Información adicional para el especialista (opcional)...");
        obsArea.setPrefRowCount(2);
        obsArea.setWrapText(true);
        obsArea.setStyle("-fx-background-radius:8; -fx-border-color:rgba(26,58,92,0.3); " +
                         "-fx-border-radius:8; -fx-font-size:13px; -fx-padding:8;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");

        Button guardarModalBtn = new Button("Enviar Remisión");
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
            if (espCombo.getValue() == null) {
                errorLabel.setText("Selecciona la especialidad de destino.");
                return;
            }
            if (motivoArea.getText().trim().isEmpty()) {
                errorLabel.setText("El motivo de la remisión es obligatorio.");
                return;
            }
            int medicoId = Sesion.getUsuario() != null ? Sesion.getUsuario().getMedicoId() : 0;
            Remision rem = new Remision(citaIdActual, pacienteIdActual, medicoId);
            rem.setEspecialidadDestino(espCombo.getValue());
            rem.setMotivoRemision(motivoArea.getText().trim());
            rem.setObservaciones(obsArea.getText().trim());
            try {
                remisionService.guardar(rem);
                modal.close();
                cargarRemisiones();
            } catch (Exception ex) {
                errorLabel.setText("Error: " + ex.getMessage());
            }
        });

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.getChildren().addAll(cancelarBtn, guardarModalBtn);

        Label titleL = new Label("Nueva Remisión a Especialista");
        titleL.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");

        Label subtitleL = new Label("La remisión quedará en estado PENDIENTE hasta que sea gestionada por administración.");
        subtitleL.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f; -fx-wrap-text:true;");
        subtitleL.setWrapText(true);

        Label lblEsp = new Label("ESPECIALIDAD DESTINO");
        lblEsp.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;");

        Label lblMotivo = new Label("MOTIVO DE REMISIÓN");
        lblMotivo.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;");

        Label lblObs = new Label("INFORMACIÓN PARA EL ESPECIALISTA");
        lblObs.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1a3a5c;");

        VBox card = new VBox(12);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:16; " +
                      "-fx-padding:28; -fx-max-width:500; " +
                      "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.22),24,0,0,6);");
        card.getChildren().addAll(titleL, subtitleL, lblEsp, espCombo,
                                  lblMotivo, motivoArea, lblObs, obsArea,
                                  errorLabel, btnRow);
        VBox.setMargin(btnRow, new Insets(4, 0, 0, 0));

        StackPane overlay = new StackPane(card);
        overlay.setStyle("-fx-background-color:rgba(0,36,68,0.55);");
        StackPane.setAlignment(card, Pos.CENTER);

        Scene scene = new Scene(overlay, owner.getWidth(), owner.getHeight());
        scene.setFill(Color.TRANSPARENT);
        modal.setScene(scene);
        modal.show();
    }

    /* ── Guardar historia clínica ───────────────────────────── */

    @FXML
    public void onGuardar(ActionEvent event) {
        int medicoId = Sesion.getUsuario() != null ? Sesion.getUsuario().getMedicoId() : 0;

        HistoriaClinica h = new HistoriaClinica(citaIdActual, pacienteIdActual, medicoId);
        if (historiaActual != null) h.setId(historiaActual.getId());
        h.setAnamnesis(anamnesisArea.getText().trim());
        h.setExamenFisico(examenFisicoArea.getText().trim());
        h.setDiagnostico(diagnosticoField.getText().trim());
        h.setTratamiento(tratamientoArea.getText().trim());
        h.setObservaciones(observacionesArea.getText().trim());

        guardarBtn.setDisable(true);
        guardarBtn.setText("Guardando…");

        Task<HistoriaClinica> task = new Task<>() {
            @Override protected HistoriaClinica call() { return historiaService.guardarOActualizar(h); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            historiaActual = task.getValue();
            statusLabel.setText("✓  Historia clínica guardada correctamente.");
            guardarBtn.setDisable(false);
            guardarBtn.setText("Guardar Historia Clínica");
            Stage st = (Stage) guardarBtn.getScene().getWindow();
            NotificacionUtil.toast(st, NotificacionUtil.Tipo.EXITO, "Historia clínica guardada correctamente.");
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            String msg = task.getException() != null ? task.getException().getMessage() : "Error desconocido";
            statusLabel.setText("⚠  Error al guardar: " + msg);
            guardarBtn.setDisable(false);
            guardarBtn.setText("Guardar Historia Clínica");
            Stage st = (Stage) guardarBtn.getScene().getWindow();
            NotificacionUtil.toast(st, NotificacionUtil.Tipo.ERROR, "Error al guardar: " + msg);
        }));
        new Thread(task, "hc-save-thread").start();
    }

    /* ── Completar cita ─────────────────────────────────────── */

    @FXML
    public void onCompletarCita(ActionEvent event) {
        Stage st = (Stage) guardarBtn.getScene().getWindow();
        boolean confirmo = NotificacionUtil.confirmar(st,
                "Completar cita",
                "¿Marcar la cita #" + citaIdActual + " como COMPLETADA?\n" +
                "Asegúrate de haber guardado la historia clínica antes de continuar.",
                "Sí, completar", "Cancelar", false);
        if (confirmo) {
            try {
                citaService.completarCita(citaIdActual);
                NotificacionUtil.toast(st, NotificacionUtil.Tipo.EXITO,
                        "Cita #" + citaIdActual + " marcada como COMPLETADA.");
                navegarA("/com/mediconsult/beta/view/fxml/panel_medico.fxml",
                         "CLINEX — Panel Médico");
            } catch (Exception ex) {
                NotificacionUtil.alerta(st, NotificacionUtil.Tipo.ERROR,
                        "Error", "No se pudo completar la cita: " + ex.getMessage());
            }
        }
    }

    /* ── Navegación ─────────────────────────────────────────── */

    @FXML
    public void onVolver(ActionEvent event) {
        navegarA("/com/mediconsult/beta/view/fxml/panel_medico.fxml", "CLINEX — Panel Médico");
    }

    @FXML
    public void onCerrarSesion(ActionEvent event) {
        Sesion.cerrar();
        navegarA("/com/mediconsult/beta/view/fxml/login.fxml", "CLINEX — Iniciar Sesión");
    }

    /* ── Utilidades ─────────────────────────────────────────── */

    private String parsearSeccion(String texto, String seccion) {
        for (String linea : texto.split("\n")) {
            if (linea.trim().startsWith(seccion + ":"))
                return linea.trim().substring(seccion.length() + 1).trim();
        }
        return "";
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
