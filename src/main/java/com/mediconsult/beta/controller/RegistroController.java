package com.mediconsult.beta.controller;

import com.mediconsult.beta.model.Paciente;
import com.mediconsult.beta.service.PacienteService;
import com.mediconsult.beta.service.UsuarioService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller de registro.fxml — CLINEX
 *
 * <p>Permite a un nuevo paciente crear su cuenta en el sistema.
 * Crea el registro en {@code persona}/{@code paciente} y el {@code usuario} vinculado.</p>
 */
public class RegistroController {

    /* ── Inyección FXML ─────────────────────────────────────── */

    @FXML private TextField        nombreField;
    @FXML private TextField        apellidoField;
    @FXML private TextField        edadField;
    @FXML private TextField        telefonoField;
    @FXML private TextField        correoField;
    @FXML private TextField        direccionField;
    @FXML private ComboBox<String> tipoSangreCombo;
    @FXML private TextField        alergiasField;
    @FXML private TextField        usernameField;
    @FXML private PasswordField    passwordField;
    @FXML private PasswordField    confirmPasswordField;
    @FXML private Label            errorLabel;
    @FXML private Button           registrarBtn;

    /* ── Servicios ──────────────────────────────────────────── */

    private final PacienteService pacienteService = new PacienteService();
    private final UsuarioService  usuarioService  = new UsuarioService();

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        tipoSangreCombo.getItems().addAll("", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
        tipoSangreCombo.setValue("");

        edadField.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) edadField.setText(n.replaceAll("[^\\d]", ""));
            clearError();
        });
        telefonoField.textProperty().addListener((obs, o, n) -> {
            String digits = n.replaceAll("[^0-9]", "");
            if (digits.length() > 10) digits = digits.substring(0, 10);
            if (!n.equals(digits)) telefonoField.setText(digits);
        });

        nombreField.textProperty().addListener((obs, o, n) -> clearError());
        apellidoField.textProperty().addListener((obs, o, n) -> clearError());
        usernameField.textProperty().addListener((obs, o, n) -> clearError());
        passwordField.textProperty().addListener((obs, o, n) -> clearError());
        confirmPasswordField.textProperty().addListener((obs, o, n) -> clearError());
    }

    /* ── Acción: registrar ──────────────────────────────────── */

    @FXML
    public void onRegistrar(ActionEvent event) {
        String nombre          = nombreField.getText().trim();
        String apellido        = apellidoField.getText().trim();
        String edadStr         = edadField.getText().trim();
        String telefono        = telefonoField.getText().trim();
        String correo          = correoField.getText().trim();
        String direccion       = direccionField.getText().trim();
        String tipoSangre      = tipoSangreCombo.getValue() != null ? tipoSangreCombo.getValue() : "";
        String alergias        = alergiasField.getText().trim();
        String username        = usernameField.getText().trim();
        String password        = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (nombre.isEmpty() || apellido.isEmpty() || edadStr.isEmpty()
                || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Por favor complete todos los campos obligatorios marcados con *.");
            return;
        }

        int edad;
        try {
            edad = Integer.parseInt(edadStr);
            if (edad <= 0 || edad > 120) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("La edad debe ser un número válido entre 1 y 120.");
            return;
        }

        if (!correo.isBlank() && !correo.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError("El correo electrónico no tiene un formato válido (ej: usuario@correo.com).");
            return;
        }
        if (password.length() < 6) {
            showError("La contraseña debe tener al menos 6 caracteres.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Las contraseñas no coinciden. Verifique e intente de nuevo.");
            return;
        }
        if (usuarioService.existeUsername(username)) {
            showError("El nombre de usuario «" + username + "» ya está en uso. Elija otro.");
            return;
        }

        try {
            Paciente paciente = pacienteService.registrarPaciente(
                    nombre, apellido, edad, telefono, correo, direccion, tipoSangre, alergias);
            usuarioService.registrarUsuarioPaciente(username, password, paciente.getId());

            Alert ok = new Alert(Alert.AlertType.INFORMATION,
                    "¡Cuenta creada exitosamente!\n\nYa puedes iniciar sesión con tu usuario:\n"
                    + username,
                    ButtonType.OK);
            ok.setTitle("Registro exitoso — CLINEX");
            ok.setHeaderText("Bienvenido a CLINEX");
            ok.showAndWait();

            navegarA("/com/mediconsult/beta/view/fxml/login.fxml", "CLINEX — Iniciar Sesión");

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Error al crear la cuenta: " + e.getMessage());
        }
    }

    /* ── Acción: volver al login ────────────────────────────── */

    @FXML
    public void onVolverLogin(ActionEvent event) {
        navegarA("/com/mediconsult/beta/view/fxml/login.fxml", "CLINEX — Iniciar Sesión");
    }

    /* ── Utilidades ─────────────────────────────────────────── */

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    private void navegarA(String fxmlPath, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) registrarBtn.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(titulo);
        } catch (IOException e) {
            showError("No se pudo navegar: " + e.getMessage());
        }
    }
}
