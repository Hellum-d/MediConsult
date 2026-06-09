package com.mediconsult.beta.util;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;
import javafx.util.Duration;

/**
 * Sistema de notificaciones visual para CLINEX.
 *
 * <p>Provee dos tipos de notificaciones:</p>
 * <ul>
 *   <li>{@link #toast} — Notificación flotante no bloqueante que se cierra
 *       automáticamente en la esquina superior derecha.</li>
 *   <li>{@link #alerta} — Modal estilizado bloqueante para mensajes
 *       importantes (info, éxito, advertencia, error).</li>
 *   <li>{@link #confirmar} — Modal de confirmación con dos botones;
 *       retorna {@code true} si el usuario confirma.</li>
 * </ul>
 *
 * <p>Todos los estilos siguen el design system de CLINEX (navy #002444,
 * redondeados, sombra suave) para mantener consistencia visual.</p>
 *
 * @author MediConsult Team
 */
public final class NotificacionUtil {

    private NotificacionUtil() {}

    /* ══════════════════════════════════════════════════════════════════════
       TIPOS DE NOTIFICACIÓN
       ══════════════════════════════════════════════════════════════════════ */

    public enum Tipo {
        EXITO,
        ERROR,
        ADVERTENCIA,
        INFO
    }

    /* ══════════════════════════════════════════════════════════════════════
       TOAST — notificación flotante, no bloqueante, auto-cierre
       ══════════════════════════════════════════════════════════════════════ */

    /**
     * Muestra un toast en la esquina superior derecha de la ventana dada.
     * Se anima (desliza hacia abajo + fade in) y se cierra automáticamente
     * tras {@code duracionMs} milisegundos.
     *
     * @param owner      ventana padre (Stage)
     * @param tipo       tipo de notificación
     * @param mensaje    texto del toast
     */
    public static void toast(Stage owner, Tipo tipo, String mensaje) {
        toast(owner, tipo, mensaje, 3200);
    }

    public static void toast(Stage owner, Tipo tipo, String mensaje, int duracionMs) {
        // ── Config visual por tipo ───────────────────────────────
        String bgColor, borderColor, icono, textColor;
        switch (tipo) {
            case EXITO -> {
                bgColor     = "#f0fdf4";
                borderColor = "#86efac";
                icono       = "✓";
                textColor   = "#166534";
            }
            case ERROR -> {
                bgColor     = "#fff1f2";
                borderColor = "#fca5a5";
                icono       = "✕";
                textColor   = "#991b1b";
            }
            case ADVERTENCIA -> {
                bgColor     = "#fffbeb";
                borderColor = "#fcd34d";
                icono       = "⚠";
                textColor   = "#92400e";
            }
            default -> {              // INFO
                bgColor     = "#eff6ff";
                borderColor = "#93c5fd";
                icono       = "ℹ";
                textColor   = "#1e40af";
            }
        }

        // ── Ícono ────────────────────────────────────────────────
        StackPane icoPane = new StackPane();
        icoPane.setMinSize(32, 32);
        icoPane.setMaxSize(32, 32);
        icoPane.setStyle(
            "-fx-background-color:" + borderColor + ";" +
            "-fx-background-radius:50%;"
        );
        Label icoL = new Label(icono);
        icoL.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + textColor + ";");
        icoPane.getChildren().add(icoL);

        // ── Mensaje ──────────────────────────────────────────────
        Label msgL = new Label(mensaje);
        msgL.setStyle("-fx-font-size:13px; -fx-text-fill:" + textColor + "; -fx-wrap-text:true;");
        msgL.setWrapText(true);
        msgL.setMaxWidth(270);

        // ── Botón cerrar ─────────────────────────────────────────
        Label closeL = new Label("✕");
        closeL.setStyle(
            "-fx-font-size:11px; -fx-text-fill:" + textColor + "; " +
            "-fx-cursor:hand; -fx-opacity:0.6;"
        );

        // ── Layout ───────────────────────────────────────────────
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(14, 16, 14, 14));
        box.setMaxWidth(340);
        box.setMinWidth(260);
        box.setStyle(
            "-fx-background-color:" + bgColor + ";" +
            "-fx-background-radius:14;" +
            "-fx-border-color:" + borderColor + ";" +
            "-fx-border-radius:14;" +
            "-fx-border-width:1.5;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),16,0,0,4);"
        );
        HBox.setHgrow(msgL, Priority.ALWAYS);
        box.getChildren().addAll(icoPane, msgL, closeL);

        // ── Popup ────────────────────────────────────────────────
        Popup popup = new Popup();
        popup.setAutoFix(false);
        popup.setAutoHide(true);
        popup.getContent().add(box);

        // Posición: esquina superior derecha con margen
        double x = owner.getX() + owner.getWidth() - 360;
        double y = owner.getY() + 70;
        popup.show(owner, x, y);

        // ── Animación entrada: deslizar hacia abajo + fade in ────
        box.setOpacity(0);
        box.setTranslateY(-12);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(240), box);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(240), box);
        slideIn.setFromY(-12);
        slideIn.setToY(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition entrar = new ParallelTransition(fadeIn, slideIn);
        entrar.play();

        // ── Cerrar al hacer clic en X ────────────────────────────
        closeL.setOnMouseClicked(e -> cerrarToast(popup, box));

        // ── Auto-cierre ──────────────────────────────────────────
        PauseTransition espera = new PauseTransition(Duration.millis(duracionMs));
        espera.setOnFinished(e -> cerrarToast(popup, box));
        espera.play();
    }

    private static void cerrarToast(Popup popup, HBox box) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(280), box);
        fadeOut.setFromValue(box.getOpacity());
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(280), box);
        slideOut.setToY(-8);
        slideOut.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition salir = new ParallelTransition(fadeOut, slideOut);
        salir.setOnFinished(e -> popup.hide());
        salir.play();
    }

    /* ══════════════════════════════════════════════════════════════════════
       ALERTA MODAL — bloqueante, estilizada al diseño CLINEX
       ══════════════════════════════════════════════════════════════════════ */

    /**
     * Muestra un modal de alerta estilizado que bloquea hasta que el usuario
     * lo cierra.
     *
     * @param owner   ventana padre
     * @param tipo    tipo de notificación
     * @param titulo  título de la alerta
     * @param mensaje cuerpo del mensaje
     */
    public static void alerta(Stage owner, Tipo tipo, String titulo, String mensaje) {
        Stage modal = crearModalBase(owner);

        // ── Config visual ────────────────────────────────────────
        String headerBg, icono, icoColor, titleColor;
        String btnBg, btnHover;
        switch (tipo) {
            case EXITO -> {
                headerBg   = "#f0fdf4";
                icono      = "✓";
                icoColor   = "#22c55e";
                titleColor = "#166534";
                btnBg      = "#22c55e";
                btnHover   = "#16a34a";
            }
            case ERROR -> {
                headerBg   = "#fff1f2";
                icono      = "✕";
                icoColor   = "#ef4444";
                titleColor = "#991b1b";
                btnBg      = "#dc2626";
                btnHover   = "#b91c1c";
            }
            case ADVERTENCIA -> {
                headerBg   = "#fffbeb";
                icono      = "⚠";
                icoColor   = "#f59e0b";
                titleColor = "#92400e";
                btnBg      = "#d97706";
                btnHover   = "#b45309";
            }
            default -> {              // INFO
                headerBg   = "#eff6ff";
                icono      = "ℹ";
                icoColor   = "#3b82f6";
                titleColor = "#1e3a8a";
                btnBg      = "#2E86DE";
                btnHover   = "#1a6bb5";
            }
        }

        // ── Ícono ────────────────────────────────────────────────
        StackPane icoBox = new StackPane();
        icoBox.setMinSize(48, 48);
        icoBox.setMaxSize(48, 48);
        icoBox.setStyle(
            "-fx-background-color:" + icoColor + "22;" +  // 13% opacidad
            "-fx-background-radius:14;"
        );
        Label icoL = new Label(icono);
        icoL.setStyle("-fx-font-size:22px; -fx-text-fill:" + icoColor + ";");
        icoBox.getChildren().add(icoL);

        // ── Textos encabezado ────────────────────────────────────
        VBox encTextos = new VBox(3);
        HBox.setHgrow(encTextos, Priority.ALWAYS);
        Label titL = new Label(titulo);
        titL.setStyle("-fx-font-size:17px; -fx-font-weight:bold; -fx-text-fill:" + titleColor + ";");
        encTextos.getChildren().add(titL);

        HBox encHeader = new HBox(14);
        encHeader.setAlignment(Pos.CENTER_LEFT);
        encHeader.setStyle(
            "-fx-background-color:" + headerBg + ";" +
            "-fx-background-radius:16 16 0 0;" +
            "-fx-padding:20 24 18 24;" +
            "-fx-border-color:transparent transparent rgba(195,198,207,0.4) transparent;" +
            "-fx-border-width:0 0 1 0;"
        );
        encHeader.getChildren().addAll(icoBox, encTextos);

        // ── Cuerpo ───────────────────────────────────────────────
        Label msgL = new Label(mensaje);
        msgL.setStyle("-fx-font-size:13px; -fx-text-fill:#43474e; -fx-wrap-text:true;");
        msgL.setWrapText(true);
        msgL.setMaxWidth(380);

        VBox body = new VBox();
        body.setStyle("-fx-padding:22 28 20 28;");
        body.getChildren().add(msgL);

        // ── Footer ───────────────────────────────────────────────
        final String btnStyle = String.format(
            "-fx-background-color:%s; -fx-text-fill:white; -fx-font-weight:bold;" +
            "-fx-background-radius:10; -fx-cursor:hand; -fx-padding:9 32; -fx-font-size:13px;",
            btnBg
        );
        final String btnHoverStyle = btnStyle.replace(btnBg, btnHover);

        Button okBtn = new Button("Aceptar");
        okBtn.setStyle(btnStyle);
        okBtn.setOnMouseEntered(e -> okBtn.setStyle(btnHoverStyle));
        okBtn.setOnMouseExited(e  -> okBtn.setStyle(btnStyle));
        okBtn.setOnAction(e -> modal.close());

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle(
            "-fx-padding:14 24 20 24;" +
            "-fx-background-color:#f8f9ff;" +
            "-fx-background-radius:0 0 16 16;" +
            "-fx-border-color:rgba(195,198,207,0.4) transparent transparent transparent;" +
            "-fx-border-width:1 0 0 0;"
        );
        footer.getChildren().add(okBtn);

        // ── Tarjeta ──────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setMaxWidth(440);
        card.setMinWidth(360);
        card.setStyle(
            "-fx-background-color:#ffffff;" +
            "-fx-background-radius:16;" +
            "-fx-border-color:rgba(195,198,207,0.3);" +
            "-fx-border-radius:16; -fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.22),28,0,0,8);"
        );
        card.getChildren().addAll(encHeader, body, footer);

        mostrarModal(modal, owner, card);
        modal.showAndWait();
    }

    /* ══════════════════════════════════════════════════════════════════════
       CONFIRMAR — modal de confirmación, retorna boolean
       ══════════════════════════════════════════════════════════════════════ */

    /**
     * Muestra un modal de confirmación y retorna {@code true} si el usuario
     * hace clic en el botón afirmativo.
     *
     * @param owner          ventana padre
     * @param titulo         título de la confirmación
     * @param mensaje        descripción de la acción a confirmar
     * @return {@code true} si el usuario confirmó, {@code false} si canceló
     */
    public static boolean confirmar(Stage owner, String titulo, String mensaje) {
        return confirmar(owner, titulo, mensaje, "Confirmar", "Cancelar", false);
    }

    /**
     * Versión extendida: permite personalizar los textos de los botones
     * y si la acción es destructiva (botón rojo).
     */
    public static boolean confirmar(Stage owner, String titulo, String mensaje,
                                     String textoConfirmar, String textoCancelar,
                                     boolean esDestructivo) {
        Stage modal = crearModalBase(owner);
        final boolean[] resultado = {false};

        // ── Ícono ────────────────────────────────────────────────
        StackPane icoBox = new StackPane();
        icoBox.setMinSize(48, 48);
        icoBox.setMaxSize(48, 48);
        String icoColor = esDestructivo ? "#ef4444" : "#f59e0b";
        icoBox.setStyle(
            "-fx-background-color:" + icoColor + "22;" +
            "-fx-background-radius:14;"
        );
        Label icoL = new Label(esDestructivo ? "⚠" : "?");
        icoL.setStyle("-fx-font-size:22px; -fx-text-fill:" + icoColor + ";");
        icoBox.getChildren().add(icoL);

        // ── Encabezado ───────────────────────────────────────────
        VBox encTextos = new VBox(2);
        HBox.setHgrow(encTextos, Priority.ALWAYS);
        Label titL = new Label(titulo);
        titL.setStyle("-fx-font-size:17px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");
        Label subL = new Label(esDestructivo ? "Esta acción no se puede deshacer" : "Confirma para continuar");
        subL.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f;");
        encTextos.getChildren().addAll(titL, subL);

        HBox encHeader = new HBox(14);
        encHeader.setAlignment(Pos.CENTER_LEFT);
        encHeader.setStyle(
            "-fx-background-color:" + (esDestructivo ? "#fff1f2" : "#fffbeb") + ";" +
            "-fx-background-radius:16 16 0 0;" +
            "-fx-padding:20 24 18 24;" +
            "-fx-border-color:transparent transparent rgba(195,198,207,0.4) transparent;" +
            "-fx-border-width:0 0 1 0;"
        );
        encHeader.getChildren().addAll(icoBox, encTextos);

        // ── Mensaje ──────────────────────────────────────────────
        Label msgL = new Label(mensaje);
        msgL.setStyle("-fx-font-size:13px; -fx-text-fill:#43474e; -fx-wrap-text:true;");
        msgL.setWrapText(true);
        msgL.setMaxWidth(380);

        VBox body = new VBox();
        body.setStyle("-fx-padding:22 28 20 28;");
        body.getChildren().add(msgL);

        // ── Botones ──────────────────────────────────────────────
        String cancelStyle =
            "-fx-background-color:transparent; -fx-text-fill:#002444; -fx-font-weight:bold;" +
            "-fx-background-radius:10; -fx-border-color:rgba(46,134,222,0.35);" +
            "-fx-border-radius:10; -fx-cursor:hand; -fx-padding:9 22; -fx-font-size:13px;";

        String confirmBg    = esDestructivo ? "#dc2626" : "#002444";
        String confirmHover = esDestructivo ? "#b91c1c" : "#1a3a5c";
        String confirmStyle = String.format(
            "-fx-background-color:%s; -fx-text-fill:white; -fx-font-weight:bold;" +
            "-fx-background-radius:10; -fx-cursor:hand; -fx-padding:9 22; -fx-font-size:13px;",
            confirmBg
        );
        String confirmHoverStyle = confirmStyle.replace(confirmBg, confirmHover);

        Button cancelBtn = new Button(textoCancelar);
        cancelBtn.setStyle(cancelStyle);
        cancelBtn.setOnAction(e -> { resultado[0] = false; modal.close(); });

        Button confirmBtn = new Button(textoConfirmar);
        confirmBtn.setStyle(confirmStyle);
        confirmBtn.setOnMouseEntered(e -> confirmBtn.setStyle(confirmHoverStyle));
        confirmBtn.setOnMouseExited(e  -> confirmBtn.setStyle(confirmStyle));
        confirmBtn.setOnAction(e -> { resultado[0] = true; modal.close(); });

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle(
            "-fx-padding:14 24 20 24;" +
            "-fx-background-color:#f8f9ff;" +
            "-fx-background-radius:0 0 16 16;" +
            "-fx-border-color:rgba(195,198,207,0.4) transparent transparent transparent;" +
            "-fx-border-width:1 0 0 0;"
        );
        footer.getChildren().addAll(cancelBtn, confirmBtn);

        // ── Tarjeta ──────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setMaxWidth(440);
        card.setMinWidth(360);
        card.setStyle(
            "-fx-background-color:#ffffff;" +
            "-fx-background-radius:16;" +
            "-fx-border-color:rgba(195,198,207,0.3);" +
            "-fx-border-radius:16; -fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.22),28,0,0,8);"
        );
        card.getChildren().addAll(encHeader, body, footer);

        mostrarModal(modal, owner, card);
        modal.showAndWait();
        return resultado[0];
    }

    /* ══════════════════════════════════════════════════════════════════════
       HELPERS INTERNOS
       ══════════════════════════════════════════════════════════════════════ */

    private static Stage crearModalBase(Stage owner) {
        Stage modal = new Stage();
        modal.initOwner(owner);
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.setResizable(false);
        return modal;
    }

    private static void mostrarModal(Stage modal, Stage owner, VBox card) {
        StackPane overlay = new StackPane(card);
        overlay.setStyle("-fx-background-color:rgba(0,36,68,0.55);");
        StackPane.setAlignment(card, Pos.CENTER);

        // Animación de entrada: escalar desde 0.92 a 1.0
        card.setScaleX(0.92);
        card.setScaleY(0.92);
        card.setOpacity(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(200), card);
        scale.setFromX(0.92); scale.setToX(1.0);
        scale.setFromY(0.92); scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(200), card);
        fade.setFromValue(0); fade.setToValue(1);

        new ParallelTransition(scale, fade).play();

        Scene scene = new Scene(overlay, owner.getWidth(), owner.getHeight());
        scene.setFill(Color.TRANSPARENT);
        modal.setScene(scene);
    }
}
