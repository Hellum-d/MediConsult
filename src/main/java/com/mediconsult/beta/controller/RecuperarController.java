package com.mediconsult.beta.controller;

import com.mediconsult.beta.service.UsuarioService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller de recuperar.fxml — CLINEX
 *
 * <p>Flujo en dos pasos:</p>
 * <ol>
 *   <li>El usuario ingresa su username → el sistema genera un código de 6 dígitos
 *       válido por 30 minutos y lo muestra en pantalla.</li>
 *   <li>El usuario ingresa el código y su nueva contraseña → el sistema la restablece.</li>
 * </ol>
 */
public class RecuperarController {

    /* ── Paso 1 ─────────────────────────────────────────────── */

    @FXML private VBox   pasoUnoPane;
    @FXML private TextField usernameRecuperarField;
    @FXML private Label  errorPasoUnoLabel;
    @FXML private Button generarCodigoBtn;

    /* ── Paso 2 ─────────────────────────────────────────────── */

    @FXML private VBox   pasoDosPane;
    @FXML private Label  tokenMostradoLabel;
    @FXML private TextField tokenField;
    @FXML private PasswordField nuevaPasswordField;
    @FXML private PasswordField confirmarNuevaPasswordField;
    @FXML private Label  errorPasoDosLabel;
    @FXML private Button resetearBtn;

    /* ── Estado interno ─────────────────────────────────────── */

    private final UsuarioService usuarioService = new UsuarioService();

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        pasoDosPane.setVisible(false);
        pasoDosPane.setManaged(false);

        usernameRecuperarField.textProperty().addListener((obs, o, n) -> clearErrorPasoUno());
        tokenField.textProperty().addListener((obs, o, n) -> clearErrorPasoDos());
        nuevaPasswordField.textProperty().addListener((obs, o, n) -> clearErrorPasoDos());
        confirmarNuevaPasswordField.textProperty().addListener((obs, o, n) -> clearErrorPasoDos());
    }

    /* ── Paso 1: Generar código ─────────────────────────────── */

    @FXML
    public void onGenerarCodigo(ActionEvent event) {
        String username = usernameRecuperarField.getText().trim();

        if (username.isEmpty()) {
            showErrorPasoUno("Ingresa tu nombre de usuario.");
            usernameRecuperarField.requestFocus();
            return;
        }

        try {
            String token = usuarioService.solicitarResetToken(username);

            // Mostrar el código generado (en un sistema real se enviaría por correo)
            tokenMostradoLabel.setText(token);

            // Transición al paso 2
            pasoUnoPane.setVisible(false);
            pasoUnoPane.setManaged(false);
            pasoDosPane.setVisible(true);
            pasoDosPane.setManaged(true);

        } catch (IllegalArgumentException e) {
            showErrorPasoUno(e.getMessage());
        } catch (Exception e) {
            showErrorPasoUno("Error al generar código: " + e.getMessage());
        }
    }

    /* ── Paso 2: Restablecer contraseña ─────────────────────── */

    @FXML
    public void onResetear(ActionEvent event) {
        String token           = tokenField.getText().trim();
        String nuevaPassword   = nuevaPasswordField.getText();
        String confirmarPassword = confirmarNuevaPasswordField.getText();

        if (token.isEmpty() || nuevaPassword.isEmpty() || confirmarPassword.isEmpty()) {
            showErrorPasoDos("Complete todos los campos.");
            return;
        }
        if (nuevaPassword.length() < 6) {
            showErrorPasoDos("La nueva contraseña debe tener al menos 6 caracteres.");
            return;
        }
        if (!nuevaPassword.equals(confirmarPassword)) {
            showErrorPasoDos("Las contraseñas no coinciden.");
            return;
        }

        try {
            usuarioService.resetearConToken(token, nuevaPassword);

            Alert ok = new Alert(Alert.AlertType.INFORMATION,
                    "Contraseña restablecida exitosamente.\nYa puedes iniciar sesión con tu nueva contraseña.",
                    ButtonType.OK);
            ok.setTitle("Contraseña restablecida — CLINEX");
            ok.setHeaderText("¡Listo!");
            ok.showAndWait();

            navegarA("/com/mediconsult/beta/view/fxml/login.fxml", "CLINEX — Iniciar Sesión");

        } catch (IllegalArgumentException e) {
            showErrorPasoDos(e.getMessage());
        } catch (Exception e) {
            showErrorPasoDos("Error al restablecer: " + e.getMessage());
        }
    }

    /* ── Navegación ─────────────────────────────────────────── */

    @FXML
    public void onVolverLogin(ActionEvent event) {
        navegarA("/com/mediconsult/beta/view/fxml/login.fxml", "CLINEX — Iniciar Sesión");
    }

    /* ── Utilidades ─────────────────────────────────────────── */

    private void showErrorPasoUno(String msg) {
        errorPasoUnoLabel.setText("⚠  " + msg);
        errorPasoUnoLabel.setVisible(true);
        errorPasoUnoLabel.setManaged(true);
    }

    private void showErrorPasoDos(String msg) {
        errorPasoDosLabel.setText("⚠  " + msg);
        errorPasoDosLabel.setVisible(true);
        errorPasoDosLabel.setManaged(true);
    }

    private void clearErrorPasoUno() {
        if (errorPasoUnoLabel != null) {
            errorPasoUnoLabel.setVisible(false);
            errorPasoUnoLabel.setManaged(false);
        }
    }

    private void clearErrorPasoDos() {
        if (errorPasoDosLabel != null) {
            errorPasoDosLabel.setVisible(false);
            errorPasoDosLabel.setManaged(false);
        }
    }

    private void navegarA(String fxmlPath, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) generarCodigoBtn.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(titulo);
        } catch (IOException e) {
            showErrorPasoUno("No se pudo navegar: " + e.getMessage());
        }
    }
}
