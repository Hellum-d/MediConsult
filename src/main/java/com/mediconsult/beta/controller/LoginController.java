package com.mediconsult.beta.controller;

import com.mediconsult.beta.model.Medico;
import com.mediconsult.beta.model.Usuario;
import com.mediconsult.beta.service.MedicoService;
import com.mediconsult.beta.service.PacienteService;
import com.mediconsult.beta.service.UsuarioService;
import com.mediconsult.beta.util.Sesion;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Controller de login.fxml — CLINEX
 *
 * <p>Autentica al usuario usando {@link UsuarioService#autenticar},
 * almacena la sesión en {@link Sesion} y navega a la pantalla de pacientes.</p>
 *
 * @author MediConsult Team
 */
public class LoginController {

    /* ── Inyección FXML ─────────────────────────────────────── */

    /** Panel izquierdo de branding (55%). */
    @FXML private VBox brandingPane;

    /** Panel derecho con el formulario de login (45%). */
    @FXML private VBox loginFormPane;

    /** Campo de texto para usuario o correo electrónico. */
    @FXML private TextField usuarioField;

    /** Campo de contraseña (enmascarado). */
    @FXML private PasswordField passwordField;

    /** Botón para mostrar / ocultar la contraseña. */
    @FXML private Button togglePasswordBtn;

    /** Botón "Ingresar". */
    @FXML private Button loginBtn;

    /** Etiqueta para mostrar mensajes de error al usuario. */
    @FXML private Label errorLabel;

    /** Labels de las stat pills — cargados dinámicamente desde la BD. */
    @FXML private Label statPacientesLabel;
    @FXML private Label statMedicosLabel;
    @FXML private Label statEspecialidadesLabel;

    /** Área central del panel izquierdo donde se insertan las tarjetas de características. */
    @FXML private VBox  featureArea;

    /** Eslogan y subtítulo — se configuran para que nunca se trunquen. */
    @FXML private Label taglineLabel;
    @FXML private Label subLabel;

    /* ── Servicios ──────────────────────────────────────────── */

    private final UsuarioService  usuarioService  = new UsuarioService();
    private final PacienteService pacienteService = new PacienteService();
    private final MedicoService   medicoService   = new MedicoService();

    /** Campo de texto plano usado cuando el usuario activa el toggle (ver contraseña). */
    private TextField passwordVisible;
    private boolean   showingPassword = false;

    /* ── Inicialización ─────────────────────────────────────── */

    /**
     * Llamado automáticamente por FXMLLoader después de cargar el FXML.
     * Configura el campo de contraseña visible (toggle) y los listeners.
     */
    @FXML
    public void initialize() {
        // Limpiar el mensaje de error al empezar a escribir
        usuarioField.textProperty().addListener((obs, o, n) -> clearError());
        passwordField.textProperty().addListener((obs, o, n) -> clearError());

        // Permitir login con la tecla Enter desde cualquier campo
        usuarioField.setOnAction(this::onLogin);
        passwordField.setOnAction(this::onLogin);

        // Forzar wrap correcto en los labels del eslogan
        taglineLabel.setWrapText(true);
        taglineLabel.setMaxWidth(Double.MAX_VALUE);
        subLabel.setWrapText(true);
        subLabel.setMaxWidth(Double.MAX_VALUE);

        // Cargar contadores reales desde la BD en un hilo aparte
        cargarEstadisticas();

        // Construir el fondo médico animado
        construirFondoMedico();
    }

    // ── Fondo médico animado ───────────────────────────────────────────────

    /**
     * Construye el área central del panel izquierdo con:
     * 1. Círculos decorativos semi-transparentes de fondo.
     * 2. Símbolos médicos flotantes (+).
     * 3. Canvas con línea ECG animada (scroll continuo).
     */
    private void construirFondoMedico() {
        featureArea.setStyle("-fx-padding:0;");

        // ── StackPane que llena todo el espacio disponible ───────
        StackPane contenedor = new StackPane();
        VBox.setVgrow(contenedor, Priority.ALWAYS);
        contenedor.setStyle("-fx-padding:0;");

        // ── 1. Círculos decorativos de fondo ─────────────────────
        Pane circulos = new Pane();
        circulos.setMouseTransparent(true);

        // Círculo grande inferior derecha
        Circle c1 = new Circle(200);
        c1.setFill(Color.rgb(46, 134, 222, 0.055));
        c1.setLayoutX(520); c1.setLayoutY(320);

        // Círculo mediano superior izquierda
        Circle c2 = new Circle(120);
        c2.setFill(Color.rgb(46, 134, 222, 0.04));
        c2.setLayoutX(-20); c2.setLayoutY(80);

        // Círculo pequeño centro
        Circle c3 = new Circle(70);
        c3.setFill(Color.rgb(46, 134, 222, 0.03));
        c3.setLayoutX(280); c3.setLayoutY(180);

        // Anillo sin relleno
        Circle c4 = new Circle(150);
        c4.setFill(Color.TRANSPARENT);
        c4.setStroke(Color.rgb(46, 134, 222, 0.10));
        c4.setStrokeWidth(1.2);
        c4.setLayoutX(400); c4.setLayoutY(100);

        circulos.getChildren().addAll(c1, c2, c3, c4);

        // ── 2. Símbolos médicos + flotantes ───────────────────────
        Pane cruces = new Pane();
        cruces.setMouseTransparent(true);

        String[][] posIconos = {
            {"80",  "60",  "+", "0.12"},
            {"340", "40",  "⊕", "0.07"},
            {"500", "200", "+", "0.09"},
            {"180", "280", "+", "0.06"},
            {"440", "320", "⊕", "0.07"},
            {"60",  "320", "+", "0.05"},
            {"260", "120", "+", "0.05"},
        };
        for (String[] pos : posIconos) {
            Label l = new Label(pos[2]);
            l.setLayoutX(Double.parseDouble(pos[0]));
            l.setLayoutY(Double.parseDouble(pos[1]));
            l.setStyle("-fx-font-size:24px; -fx-text-fill:#002444; -fx-font-weight:bold;");
            l.setOpacity(Double.parseDouble(pos[3]));
            cruces.getChildren().add(l);
        }

        // ── 3. Canvas ECG animado ────────────────────────────────
        Canvas ecgCanvas = new Canvas(560, 110);
        ecgCanvas.setMouseTransparent(true);
        StackPane.setAlignment(ecgCanvas, javafx.geometry.Pos.BOTTOM_LEFT);

        iniciarEcgAnimation(ecgCanvas);

        // ── Ensamblar ────────────────────────────────────────────
        contenedor.getChildren().addAll(circulos, cruces, ecgCanvas);
        featureArea.getChildren().add(contenedor);
    }

    // ── ECG ──────────────────────────────────────────────────────────────

    /**
     * Genera un ciclo ECG realista de alta resolución usando funciones
     * gaussianas para P-wave, QRS y T-wave. 200 puntos = curvas perfectamente suaves.
     * Valores Y: 0 = arriba del canvas, 1 = abajo. Baseline en 0.5.
     */
    private static final int    ECG_PUNTOS = 200;
    private static final double[] ECG_Y    = generarEcg();

    private static double[] generarEcg() {
        double[] y = new double[ECG_PUNTOS];
        for (int i = 0; i < ECG_PUNTOS; i++) {
            double t = (double) i / ECG_PUNTOS;
            double v = 0.0; // desviación de baseline (negativo = sube, positivo = baja)

            // P wave — onda suave pequeña hacia arriba (t ≈ 0.10)
            v -= 0.10 * gauss(t, 0.10, 0.025);

            // Q — pequeña depresión antes del pico (t ≈ 0.27)
            v += 0.06 * gauss(t, 0.27, 0.010);

            // R — pico principal muy alto hacia arriba (t ≈ 0.30)
            v -= 0.82 * gauss(t, 0.30, 0.014);

            // S — depresión post-pico (t ≈ 0.34)
            v += 0.14 * gauss(t, 0.34, 0.010);

            // T wave — onda suave hacia arriba (t ≈ 0.52)
            v -= 0.16 * gauss(t, 0.52, 0.038);

            // Valor final: baseline 0.5 + desviación
            y[i] = Math.max(0.04, Math.min(0.96, 0.5 + v));
        }
        return y;
    }

    /** Función gaussiana normalizada. */
    private static double gauss(double x, double mu, double sigma) {
        return Math.exp(-0.5 * Math.pow((x - mu) / sigma, 2));
    }

    private double ecgOffset = 0;

    /**
     * Animación ECG fluida a 60fps.
     * Dibuja la onda con efecto glow (capa difusa + capa nítida)
     * y scrollea de derecha a izquierda suavemente.
     */
    private void iniciarEcgAnimation(Canvas canvas) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // px por punto ECG — con 200 puntos a scaleX=3 tenemos 600px por ciclo
        final double scaleX    = 3.0;
        final double scrollVel = 0.18;  // puntos avanzados por frame (60fps = ~11px/s)

        Timeline tl = new Timeline(new KeyFrame(Duration.millis(16), e -> {
            gc.clearRect(0, 0, w, h);

            // ── Línea de base sutil ──────────────────────────────
            gc.setStroke(Color.rgb(0, 36, 68, 0.06));
            gc.setLineWidth(1);
            gc.strokeLine(0, h * 0.5, w, h * 0.5);

            // ── Capa 1: GLOW (línea gruesa semi-transparente) ────
            gc.setStroke(Color.rgb(46, 134, 222, 0.18));
            gc.setLineWidth(6.0);
            gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            dibujarEcg(gc, w, h, scaleX);

            // ── Capa 2: línea nítida principal ───────────────────
            gc.setStroke(Color.rgb(46, 134, 222, 0.92));
            gc.setLineWidth(2.0);
            dibujarEcg(gc, w, h, scaleX);

            // ── Punto brillante en la punta activa ───────────────
            int    idxActivo = (int) ecgOffset % ECG_PUNTOS;
            double yActivo   = ECG_Y[Math.abs(idxActivo)] * (h - 16) + 8;
            // Halo exterior
            gc.setFill(Color.rgb(46, 134, 222, 0.20));
            gc.fillOval(w - 14, yActivo - 7, 14, 14);
            // Punto sólido
            gc.setFill(Color.rgb(46, 134, 222, 0.95));
            gc.fillOval(w - 9, yActivo - 4.5, 9, 9);

            ecgOffset += scrollVel;
        }));

        tl.setCycleCount(Animation.INDEFINITE);
        tl.play();
    }

    /** Dibuja la onda ECG completa sobre el canvas (reutilizable para glow y línea). */
    private void dibujarEcg(GraphicsContext gc, double w, double h, double scaleX) {
        int totalPuntos = (int)(w / scaleX) + ECG_PUNTOS + 2;
        double offsetPx = (ecgOffset % 1.0) * scaleX; // fracción sub-punto para suavidad

        gc.beginPath();
        boolean iniciado = false;

        for (int i = 0; i < totalPuntos; i++) {
            int idx = (int)(Math.abs(ecgOffset + i) % ECG_PUNTOS);
            double yFrac = ECG_Y[idx];
            double x     = i * scaleX - offsetPx;
            double y     = yFrac * (h - 16) + 8;

            if (!iniciado) { gc.moveTo(x, y); iniciado = true; }
            else            gc.lineTo(x, y);
        }
        gc.stroke();
    }

    // ── Estadísticas ───────────────────────────────────────────────────────

    /**
     * Carga los contadores desde la BD en un hilo de fondo para no
     * bloquear la UI, luego los anima con un efecto de conteo progresivo.
     */
    private void cargarEstadisticas() {
        Task<int[]> task = new Task<>() {
            @Override
            protected int[] call() {
                int pacientes     = 0;
                int medicos       = 0;
                int especialidades = 0;

                try {
                    pacientes = pacienteService.listarTodos().size();
                } catch (Exception ignored) {}

                try {
                    List<Medico> listaMedicos = medicoService.listarTodos();
                    medicos = listaMedicos.size();
                    especialidades = (int) listaMedicos.stream()
                            .map(Medico::getEspecialidad)
                            .filter(e -> e != null && !e.isBlank())
                            .distinct()
                            .count();
                } catch (Exception ignored) {}

                return new int[]{ pacientes, medicos, especialidades };
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            int[] datos = task.getValue();
            animarContador(statPacientesLabel,      datos[0], " PACIENTES ACTIVOS");
            animarContador(statMedicosLabel,         datos[1], " MÉDICOS DISPONIBLES");
            animarContador(statEspecialidadesLabel,  datos[2], " ESPECIALIDADES");
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            statPacientesLabel.setText("— PACIENTES ACTIVOS");
            statMedicosLabel.setText("— MÉDICOS DISPONIBLES");
            statEspecialidadesLabel.setText("— ESPECIALIDADES");
        }));

        new Thread(task, "login-stats-thread").start();
    }

    /**
     * Anima el Label contando desde 0 hasta {@code objetivo} en ~1.4 segundos.
     * El conteo acelera al inicio y desacelera al final (ease-out).
     *
     * @param label    Label a animar
     * @param objetivo valor final real
     * @param sufijo   texto que sigue al número (ej. " PACIENTES ACTIVOS")
     */
    private void animarContador(Label label, int objetivo, String sufijo) {
        if (objetivo == 0) {
            label.setText("0" + sufijo);
            return;
        }

        final int duracionMs = 1400;
        final int pasos      = Math.min(objetivo, 60);  // máx 60 frames
        final long intervalMs = duracionMs / pasos;

        Timeline tl = new Timeline();
        for (int i = 1; i <= pasos; i++) {
            final int paso = i;
            // Interpolación ease-out: avanza rápido al inicio, lento al final
            double progreso = 1.0 - Math.pow(1.0 - (double) paso / pasos, 2);
            final int valor  = (int) Math.round(progreso * objetivo);

            KeyFrame kf = new KeyFrame(
                Duration.millis(intervalMs * paso),
                ev -> label.setText(valor + sufijo)
            );
            tl.getKeyFrames().add(kf);
        }

        // Frame final: asegura que el valor exacto quede bien
        tl.getKeyFrames().add(new KeyFrame(
            Duration.millis(duracionMs + 50),
            ev -> label.setText(objetivo + sufijo)
        ));

        tl.play();
    }

    /* ── Manejadores de eventos ─────────────────────────────── */

    /**
     * Autenticar al usuario contra la base de datos.
     * Vinculado al botón "Ingresar" (loginBtn) y a la tecla Enter de los campos.
     */
    @FXML
    public void onLogin(ActionEvent event) {
        String username = usuarioField.getText().trim();
        String password = showingPassword
                ? passwordVisible.getText()
                : passwordField.getText();

        // Validación básica en cliente
        if (username.isEmpty()) {
            showError("El campo de usuario no puede estar vacío.");
            usuarioField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError("El campo de contraseña no puede estar vacío.");
            passwordField.requestFocus();
            return;
        }

        try {
            Optional<Usuario> opt = usuarioService.autenticar(username, password);
            if (opt.isEmpty()) {
                showError("Credenciales incorrectas. Verifique usuario y contraseña.");
                passwordField.clear();
                usuarioField.requestFocus();
                return;
            }

            // Sesión exitosa — guardar en Sesion y navegar por rol
            Sesion.iniciar(opt.get());
            String rol = opt.get().getRol();
            if (Usuario.ROL_PACIENTE.equals(rol)) {
                navegarA("/com/mediconsult/beta/view/fxml/portal_paciente.fxml",
                         "CLINEX — Portal del Paciente");
            } else if (Usuario.ROL_MEDICO_ESPECIALISTA.equals(rol)) {
                navegarA("/com/mediconsult/beta/view/fxml/panel_especialista.fxml",
                         "CLINEX — Panel Especialista");
            } else if (Usuario.ROL_MEDICO_GENERAL.equals(rol)
                    || Usuario.ROL_MEDICO.equals(rol)) {
                navegarA("/com/mediconsult/beta/view/fxml/panel_medico.fxml",
                         "CLINEX — Panel Médico");
            } else {
                navegarA("/com/mediconsult/beta/view/fxml/pacientes.fxml",
                         "CLINEX — Gestión de Pacientes");
            }

        } catch (IllegalArgumentException e) {
            showError("Datos inválidos: " + e.getMessage());
        } catch (Exception e) {
            showError("Error al conectar con el servidor. Intente nuevamente.");
        }
    }

    /**
     * Alternar la visibilidad de la contraseña.
     * Vinculado al botón togglePasswordBtn (ícono ojo).
     */
    @FXML
    public void onTogglePassword(ActionEvent event) {
        if (!showingPassword) {
            // Mostrar contraseña: reemplazar PasswordField por TextField
            if (passwordVisible == null) {
                passwordVisible = new TextField();
                passwordVisible.setStyle(passwordField.getStyle());
                passwordVisible.getStyleClass().addAll(passwordField.getStyleClass());
                passwordVisible.setPromptText(passwordField.getPromptText());
                passwordVisible.textProperty().addListener((obs, o, n) -> clearError());
                passwordVisible.setOnAction(this::onLogin);
            }
            passwordVisible.setText(passwordField.getText());

            // Intercambiar nodos en el padre
            javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) passwordField.getParent();
            int idx = parent.getChildren().indexOf(passwordField);
            parent.getChildren().set(idx, passwordVisible);
            passwordVisible.requestFocus();
            togglePasswordBtn.setText("🙈");
            showingPassword = true;

        } else {
            // Ocultar contraseña: volver al PasswordField
            passwordField.setText(passwordVisible.getText());
            javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) passwordVisible.getParent();
            int idx = parent.getChildren().indexOf(passwordVisible);
            parent.getChildren().set(idx, passwordField);
            passwordField.requestFocus();
            togglePasswordBtn.setText("👁");
            showingPassword = false;
        }
    }

    /** Navega a la pantalla de registro de paciente. */
    @FXML
    public void onRegistrarse(ActionEvent event) {
        navegarA("/com/mediconsult/beta/view/fxml/registro.fxml", "CLINEX — Crear Cuenta");
    }

    /** Navega a la pantalla de recuperación de contraseña. */
    @FXML
    public void onRecuperarContrasena(ActionEvent event) {
        navegarA("/com/mediconsult/beta/view/fxml/recuperar.fxml", "CLINEX — Recuperar Contraseña");
    }

    /* ── Utilidades ─────────────────────────────────────────── */

    /** Muestra un mensaje de error en la etiqueta visible al usuario. */
    private void showError(String mensaje) {
        errorLabel.setText(mensaje);
        errorLabel.setVisible(true);
    }

    /** Limpia el mensaje de error. */
    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    /**
     * Navega a otra pantalla FXML en la misma ventana.
     *
     * @param fxmlPath ruta del recurso FXML (ej. "/com/mediconsult/beta/view/fxml/pacientes.fxml")
     * @param titulo   título de la ventana
     */
    private void navegarA(String fxmlPath, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) loginBtn.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(titulo);
        } catch (IOException e) {
            showError("No se pudo cargar la pantalla: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
