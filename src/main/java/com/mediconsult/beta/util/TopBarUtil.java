package com.mediconsult.beta.util;

import com.mediconsult.beta.model.Usuario;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Utilidad que conecta los tres botones de la barra superior (notificaciones,
 * ajustes y perfil) en todas las pantallas del sistema CLINEX.
 *
 * <p>Se invoca una sola vez desde el {@code initialize()} de cada controller
 * pasando la referencia al {@code topBar}. Localiza los botones por texto
 * y el avatar por su {@code fx:id="avatarCircle"}.</p>
 *
 * <p>No requiere cambios en los archivos FXML ni en los controladores
 * más allá de una única llamada en {@code initialize()}.</p>
 *
 * @author MediConsult Team
 */
public class TopBarUtil {

    private TopBarUtil() {}

    // ── CSS stylesheet del sistema ─────────────────────────────────────────

    private static final String CSS_PATH =
            "/com/mediconsult/beta/view/fxml/styles.css";

    // ── Punto de entrada ───────────────────────────────────────────────────

    /**
     * Configura los tres botones superiores de la pantalla actual.
     *
     * <p>Localiza el botón 🔔 y ⚙ como hijos directos de {@code topBar},
     * y el avatar Label usando {@code topBar.lookup("#avatarCircle")}.</p>
     *
     * @param topBar HBox de la barra superior, inyectado vía @FXML en el controller
     */
    public static void configurar(HBox topBar) {
        Button notifBtn    = null;
        Button ajustesBtn  = null;

        for (Node node : topBar.getChildren()) {
            if (node instanceof Button btn) {
                if ("🔔".equals(btn.getText()))  notifBtn   = btn;
                else if ("⚙".equals(btn.getText())) ajustesBtn = btn;
            }
        }

        // Avatar: tiene fx:id="avatarCircle" → JavaFX asigna el CSS id automáticamente
        Node avatarNode = topBar.lookup("#avatarCircle");

        // Actualizar letra del avatar con la sesión activa
        if (avatarNode instanceof Label avatarLabel) {
            Usuario u = Sesion.getUsuario();
            if (u != null && !u.getUsername().isEmpty()) {
                avatarLabel.setText(u.getUsername().substring(0, 1).toUpperCase());
            }
        }

        // Conectar botón de notificaciones
        if (notifBtn != null) {
            final Button btn = notifBtn;
            btn.setOnAction(e -> {
                Bounds b = btn.localToScreen(btn.getBoundsInLocal());
                mostrarNotificacionesPopup(btn, b.getMaxX() - 292, b.getMaxY() + 8);
            });
        }

        // Conectar botón de ajustes
        if (ajustesBtn != null) {
            final Button btn = ajustesBtn;
            btn.setOnAction(e ->
                mostrarAjustesModal((Stage) topBar.getScene().getWindow()));
        }

        // Conectar clic en avatar
        if (avatarNode instanceof Label avatarLabel) {
            final HBox tb = topBar;
            avatarLabel.setOnMouseClicked(e ->
                mostrarPerfilModal((Stage) tb.getScene().getWindow()));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PANEL DE NOTIFICACIONES
    // ══════════════════════════════════════════════════════════════════════

    private static void mostrarNotificacionesPopup(Button anchor, double x, double y) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setAutoFix(true);

        VBox panel = new VBox(0);
        panel.setStyle(
            "-fx-background-color:#ffffff; -fx-background-radius:14;" +
            "-fx-border-color:rgba(46,134,222,0.18); -fx-border-radius:14;" +
            "-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.18),18,0,0,6);" +
            "-fx-pref-width:292; -fx-min-width:292;"
        );

        // ── Header ──────────────────────────────────────────────────────
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
            "-fx-padding:14 18 12 18;" +
            "-fx-border-color:transparent transparent rgba(195,198,207,0.4) transparent;" +
            "-fx-border-width:0 0 1 0;"
        );
        Label icono = new Label("🔔");
        icono.setStyle("-fx-font-size:16px;");
        Label titulo = new Label("Notificaciones");
        titulo.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#002444;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label badge = new Label("3");
        badge.setStyle(
            "-fx-background-color:#2E86DE; -fx-text-fill:white;" +
            "-fx-background-radius:10; -fx-padding:1 7 1 7;" +
            "-fx-font-size:11px; -fx-font-weight:bold;"
        );
        header.getChildren().addAll(icono, titulo, spacer, badge);

        // ── Lista de notificaciones ──────────────────────────────────────
        VBox lista = new VBox(0);

        // Notif 1: sistema
        lista.getChildren().add(crearItemNotif(
            "🟢", "Sistema activo",
            "CLINEX v0.2.0 funcionando correctamente", false));

        // Notif 2: sesión
        Usuario u = Sesion.getUsuario();
        String sesInfo = u != null
                ? "Usuario: " + u.getUsername() + "  ·  " + u.getRol()
                : "Sesión anónima";
        lista.getChildren().add(crearItemNotif(
            "👤", "Sesión iniciada", sesInfo, false));

        // Notif 3: recordatorio del día
        String hoy = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        lista.getChildren().add(crearItemNotif(
            "📋", "Revisión diaria",
            hoy + " — Revisa las citas pendientes del día", true));

        // ── Footer ──────────────────────────────────────────────────────
        Label footer = new Label("Todas las notificaciones vistas");
        footer.setStyle(
            "-fx-font-size:11px; -fx-text-fill:#73777f;" +
            "-fx-padding:10 18 10 18; -fx-cursor:default;"
        );
        footer.setMaxWidth(Double.MAX_VALUE);
        footer.setAlignment(Pos.CENTER);

        panel.getChildren().addAll(header, lista, footer);
        popup.getContent().add(panel);
        popup.show(anchor, x, y);
    }

    private static HBox crearItemNotif(String icono, String titulo,
                                        String detalle, boolean isLast) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);
        String border = isLast ? ""
                : "-fx-border-color:transparent transparent rgba(195,198,207,0.35) transparent;" +
                  "-fx-border-width:0 0 1 0;";
        String baseStyle = "-fx-padding:12 18 12 18; " + border + " -fx-cursor:hand;";
        String hoverStyle = "-fx-background-color:#f0f4f8; " + baseStyle;
        row.setStyle(baseStyle);
        row.setOnMouseEntered(e -> row.setStyle(hoverStyle));
        row.setOnMouseExited(e  -> row.setStyle(baseStyle));

        Label iconLabel = new Label(icono);
        iconLabel.setStyle("-fx-font-size:18px; -fx-min-width:24;");

        VBox textCol = new VBox(2);
        Label tituloLabel = new Label(titulo);
        tituloLabel.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");
        Label detalleLabel = new Label(detalle);
        detalleLabel.setStyle(
            "-fx-font-size:11px; -fx-text-fill:#73777f; -fx-wrap-text:true;");
        detalleLabel.setMaxWidth(210);
        textCol.getChildren().addAll(tituloLabel, detalleLabel);

        row.getChildren().addAll(iconLabel, textCol);
        return row;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MODAL DE AJUSTES
    // ══════════════════════════════════════════════════════════════════════

    private static void mostrarAjustesModal(Stage ownerStage) {
        Stage modal = new Stage();
        modal.initOwner(ownerStage);
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.setResizable(false);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:rgba(0,36,68,0.55);");

        VBox card = new VBox(0);
        card.setMaxWidth(480);
        card.setMinWidth(480);
        card.setStyle(
            "-fx-background-color:#ffffff; -fx-background-radius:20;" +
            "-fx-border-color:rgba(46,134,222,0.2); -fx-border-radius:20;" +
            "-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.25),32,0,0,8);"
        );

        // Header
        HBox headerBox = new HBox(16);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setStyle(
            "-fx-background-color:#f8f9ff; -fx-background-radius:20 20 0 0;" +
            "-fx-padding:22 24 18 24;" +
            "-fx-border-color:transparent transparent rgba(195,198,207,0.4) transparent;" +
            "-fx-border-width:0 0 1 0;"
        );
        StackPane iconBox = new StackPane();
        iconBox.setMinSize(48,48); iconBox.setMaxSize(48,48);
        iconBox.setStyle("-fx-background-color:#eff4ff; -fx-background-radius:14;");
        Label icono = new Label("⚙");
        icono.setStyle("-fx-font-size:22px;");
        iconBox.getChildren().add(icono);

        VBox headerTexts = new VBox(4);
        HBox.setHgrow(headerTexts, Priority.ALWAYS);
        Label hTitulo = new Label("Configuración del Sistema");
        hTitulo.setStyle("-fx-font-size:19px; -fx-font-weight:bold; -fx-text-fill:#002444;");
        Label hSub = new Label("Información y preferencias de CLINEX");
        hSub.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f;");
        headerTexts.getChildren().addAll(hTitulo, hSub);
        headerBox.getChildren().addAll(iconBox, headerTexts, crearBtnCerrar(() -> modal.close()));

        // Body
        VBox body = new VBox(20);
        body.setStyle("-fx-padding:24 28 24 28;");

        // Sección: Sistema
        body.getChildren().add(crearSeccionInfo("INFORMACIÓN DEL SISTEMA", new String[][]{
            {"Aplicación",    "CLINEX — Sistema Hospitalario"},
            {"Versión",       "0.2.0 — Build 2025"},
            {"Plataforma",    "JavaFX 17  ·  Java 17 LTS"},
            {"Base de datos", "MySQL (JDBC — Conectada ✓)"}
        }));

        body.getChildren().add(crearDivisor());

        // Sección: Sesión
        Usuario u = Sesion.getUsuario();
        String rolStr = u != null
                ? (Usuario.ROL_ADMIN.equals(u.getRol()) ? "Administrador" : "Médico") : "—";
        body.getChildren().add(crearSeccionInfo("SESIÓN ACTUAL", new String[][]{
            {"Usuario", u != null ? u.getUsername() : "—"},
            {"Rol",     rolStr},
            {"ID",      u != null ? String.valueOf(u.getId()) : "—"}
        }));

        body.getChildren().add(crearDivisor());

        // Nota de apariencia
        Label nota = new Label(
            "🎨  Tema visual: CLINEX Blue (único disponible en v0.2.0).\n" +
            "Las versiones futuras incluirán modo oscuro y personalización.");
        nota.setStyle(
            "-fx-font-size:11px; -fx-text-fill:#2E86DE;" +
            "-fx-background-color:#eff4ff; -fx-background-radius:8;" +
            "-fx-padding:10 14; -fx-wrap-text:true;"
        );
        nota.setMaxWidth(Double.MAX_VALUE);
        body.getChildren().add(nota);

        // Footer
        HBox footer = crearFooter();
        Button cerrarBtn = new Button("Cerrar");
        cerrarBtn.setStyle(
            "-fx-background-color:#002444; -fx-text-fill:white;" +
            "-fx-font-weight:bold; -fx-background-radius:10;" +
            "-fx-padding:10 32; -fx-cursor:hand; -fx-font-size:13px;"
        );
        cerrarBtn.setOnAction(e -> modal.close());
        footer.getChildren().add(cerrarBtn);

        card.getChildren().addAll(headerBox, body, footer);
        montarYMostrar(modal, overlay, card, ownerStage);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MODAL DE PERFIL
    // ══════════════════════════════════════════════════════════════════════

    private static void mostrarPerfilModal(Stage ownerStage) {
        Stage modal = new Stage();
        modal.initOwner(ownerStage);
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.setResizable(false);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:rgba(0,36,68,0.55);");

        VBox card = new VBox(0);
        card.setMaxWidth(380);
        card.setMinWidth(380);
        card.setStyle(
            "-fx-background-color:#ffffff; -fx-background-radius:20;" +
            "-fx-border-color:rgba(46,134,222,0.2); -fx-border-radius:20;" +
            "-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.25),32,0,0,8);"
        );

        // Header azul oscuro con avatar
        Usuario u = Sesion.getUsuario();
        String inicial  = (u != null && !u.getUsername().isEmpty())
                ? u.getUsername().substring(0, 1).toUpperCase() : "U";
        String username = u != null ? u.getUsername() : "Usuario";
        String rolStr   = u != null
                ? (Usuario.ROL_ADMIN.equals(u.getRol()) ? "Administrador" : "Médico") : "—";

        HBox headerBox = new HBox(14);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setStyle("-fx-background-color:#002444; -fx-background-radius:20 20 0 0; -fx-padding:22 24 18 24;");

        StackPane avatarGrande = new StackPane();
        avatarGrande.setMinSize(52, 52); avatarGrande.setMaxSize(52, 52);
        avatarGrande.setStyle("-fx-background-color:#54a3fd; -fx-background-radius:26;");
        Label avatarLetra = new Label(inicial);
        avatarLetra.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:white;");
        avatarGrande.getChildren().add(avatarLetra);

        VBox headerTexts = new VBox(5);
        HBox.setHgrow(headerTexts, Priority.ALWAYS);
        Label usernameLabel = new Label(username);
        usernameLabel.setStyle("-fx-font-size:17px; -fx-font-weight:bold; -fx-text-fill:#ffffff;");
        Label rolBadge = new Label(rolStr);
        rolBadge.setStyle(
            "-fx-background-color:rgba(255,255,255,0.15); -fx-background-radius:12;" +
            "-fx-padding:2 10; -fx-font-size:11px;" +
            "-fx-text-fill:rgba(255,255,255,0.85); -fx-font-weight:bold;"
        );
        headerTexts.getChildren().addAll(usernameLabel, rolBadge);
        headerBox.getChildren().addAll(avatarGrande, headerTexts,
                                        crearBtnCerrarBlanco(() -> modal.close()));

        // Body
        VBox body = new VBox(16);
        body.setStyle("-fx-padding:24 28 20 28;");

        Label bodyTitulo = new Label("INFORMACIÓN DE CUENTA");
        bodyTitulo.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#2E86DE;");

        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(10);
        agregarFilaGrid(grid, "Usuario:",     username, 0);
        agregarFilaGrid(grid, "Rol:",         rolStr,   1);
        agregarFilaGrid(grid, "ID de cuenta:", u != null ? String.valueOf(u.getId()) : "—", 2);
        agregarFilaGrid(grid, "Estado:",      "Sesión activa ✓", 3);

        body.getChildren().addAll(bodyTitulo, grid, crearDivisor());

        Label notaSec = new Label("💡  Las contraseñas se almacenan cifradas con SHA-256.");
        notaSec.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f; -fx-wrap-text:true;");
        notaSec.setMaxWidth(Double.MAX_VALUE);
        body.getChildren().add(notaSec);

        // Footer con Cancelar + Cerrar Sesión
        HBox footer = crearFooter();

        Button cancelarBtn = new Button("Cancelar");
        cancelarBtn.setStyle(
            "-fx-background-color:transparent; -fx-border-color:rgba(46,134,222,0.35);" +
            "-fx-border-radius:10; -fx-text-fill:#002444; -fx-font-weight:bold;" +
            "-fx-padding:10 20; -fx-cursor:hand; -fx-font-size:13px;" +
            "-fx-background-radius:10;"
        );
        cancelarBtn.setOnAction(e -> modal.close());

        Button cerrarSesionBtn = new Button("⏻  Cerrar Sesión");
        cerrarSesionBtn.setStyle(
            "-fx-background-color:#ba1a1a; -fx-text-fill:white;" +
            "-fx-font-weight:bold; -fx-background-radius:10;" +
            "-fx-padding:10 20; -fx-cursor:hand; -fx-font-size:13px;"
        );
        cerrarSesionBtn.setOnMouseEntered(e -> cerrarSesionBtn.setStyle(
            "-fx-background-color:#93000a; -fx-text-fill:white;" +
            "-fx-font-weight:bold; -fx-background-radius:10;" +
            "-fx-padding:10 20; -fx-cursor:hand; -fx-font-size:13px;"));
        cerrarSesionBtn.setOnMouseExited(e -> cerrarSesionBtn.setStyle(
            "-fx-background-color:#ba1a1a; -fx-text-fill:white;" +
            "-fx-font-weight:bold; -fx-background-radius:10;" +
            "-fx-padding:10 20; -fx-cursor:hand; -fx-font-size:13px;"));
        cerrarSesionBtn.setOnAction(e -> {
            modal.close();
            Platform.runLater(() -> cerrarSesion(ownerStage));
        });

        footer.getChildren().addAll(cancelarBtn, cerrarSesionBtn);
        card.getChildren().addAll(headerBox, body, footer);
        montarYMostrar(modal, overlay, card, ownerStage);
    }

    // ── Cerrar sesión ──────────────────────────────────────────────────────

    private static void cerrarSesion(Stage stage) {
        Sesion.cerrar();
        try {
            FXMLLoader loader = new FXMLLoader(
                TopBarUtil.class.getResource("/com/mediconsult/beta/view/fxml/login.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root,
                    stage.getScene().getWidth(),
                    stage.getScene().getHeight()));
            stage.setTitle("CLINEX — Inicio de Sesión");
        } catch (IOException ex) {
            System.err.println("[TopBarUtil] No se pudo navegar al login: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS VISUALES
    // ══════════════════════════════════════════════════════════════════════

    private static VBox crearSeccionInfo(String titulo, String[][] filas) {
        VBox box = new VBox(10);
        Label tituloLabel = new Label(titulo);
        tituloLabel.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#2E86DE;");
        GridPane grid = new GridPane();
        grid.setHgap(28);
        grid.setVgap(8);
        for (int i = 0; i < filas.length; i++) {
            agregarFilaGrid(grid, filas[i][0], filas[i][1], i);
        }
        box.getChildren().addAll(tituloLabel, grid);
        return box;
    }

    private static void agregarFilaGrid(GridPane grid, String etiqueta, String valor, int row) {
        Label etq = new Label(etiqueta);
        etq.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f;");
        Label val = new Label(valor);
        val.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");
        grid.add(etq, 0, row);
        grid.add(val, 1, row);
    }

    private static Region crearDivisor() {
        Region div = new Region();
        div.setStyle("-fx-background-color:rgba(195,198,207,0.5);" +
                     "-fx-min-height:1; -fx-max-height:1;");
        return div;
    }

    private static HBox crearFooter() {
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle(
            "-fx-padding:16 28 22 28;" +
            "-fx-border-color:rgba(195,198,207,0.4) transparent transparent transparent;" +
            "-fx-border-width:1 0 0 0;" +
            "-fx-background-color:#f8f9ff; -fx-background-radius:0 0 20 20;"
        );
        return footer;
    }

    private static Button crearBtnCerrar(Runnable action) {
        Button btn = new Button("✕");
        String base = "-fx-background-color:transparent; -fx-text-fill:#73777f; -fx-font-size:16px; -fx-cursor:hand; -fx-padding:4 8; -fx-background-radius:8;";
        String hover = "-fx-background-color:#f2f3f7; -fx-text-fill:#0b1c30; -fx-font-size:16px; -fx-cursor:hand; -fx-padding:4 8; -fx-background-radius:8;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private static Button crearBtnCerrarBlanco(Runnable action) {
        Button btn = new Button("✕");
        String base  = "-fx-background-color:transparent; -fx-text-fill:rgba(255,255,255,0.65); -fx-font-size:16px; -fx-cursor:hand; -fx-padding:4 8; -fx-background-radius:8;";
        String hover = "-fx-background-color:rgba(255,255,255,0.15); -fx-text-fill:white; -fx-font-size:16px; -fx-cursor:hand; -fx-padding:4 8; -fx-background-radius:8;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private static void montarYMostrar(Stage modal, StackPane overlay,
                                        VBox card, Stage ownerStage) {
        StackPane.setAlignment(card, Pos.CENTER);
        overlay.getChildren().add(card);
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) modal.close(); });

        Scene scene = new Scene(overlay,
                ownerStage.getScene().getWidth(),
                ownerStage.getScene().getHeight());
        scene.setFill(Color.TRANSPARENT);
        try {
            String css = TopBarUtil.class.getResource(CSS_PATH).toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception ignored) {}

        modal.setScene(scene);
        modal.showAndWait();
    }
}
