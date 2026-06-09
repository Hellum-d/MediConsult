package com.mediconsult.beta.controller;

import com.mediconsult.beta.model.Paciente;
import com.mediconsult.beta.model.Usuario;
import com.mediconsult.beta.service.PacienteService;
import com.mediconsult.beta.util.Sesion;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

/**
 * Controller de portal_paciente.fxml — CLINEX
 *
 * <p>Portal inicial del paciente tras iniciar sesión.
 * Carga y muestra la información del paciente desde la base de datos
 * usando el {@code pacienteId} almacenado en la sesión activa.</p>
 */
public class PortalPacienteController {

    /* ── Inyección FXML ─────────────────────────────────────── */

    @FXML private Label bienvenidaLabel;
    @FXML private Label nombreCompletoLabel;
    @FXML private Label correoLabel;
    @FXML private Label telefonoLabel;
    @FXML private Label edadLabel;
    @FXML private Label tipoSangreLabel;
    @FXML private Label alergiasLabel;
    @FXML private Label rolLabel;

    /* ── Servicios ──────────────────────────────────────────── */

    private final PacienteService pacienteService = new PacienteService();

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        Usuario usuario = Sesion.getUsuario();
        if (usuario == null) {
            bienvenidaLabel.setText("Bienvenido");
            return;
        }

        int pacienteId = usuario.getPacienteId();
        if (pacienteId > 0) {
            Optional<Paciente> opt = pacienteService.obtenerPorId(pacienteId);
            if (opt.isPresent()) {
                Paciente p = opt.get();
                bienvenidaLabel.setText("Bienvenido, " + p.getNombre() + " " + p.getApellido());
                nombreCompletoLabel.setText(p.getNombre() + " " + p.getApellido());
                correoLabel.setText(p.getCorreo() != null && !p.getCorreo().isBlank()
                        ? p.getCorreo() : "No registrado");
                telefonoLabel.setText(p.getTelefono() != null && !p.getTelefono().isBlank()
                        ? p.getTelefono() : "No registrado");
                edadLabel.setText(p.getEdad() + " años");
                tipoSangreLabel.setText(p.getTipoSangre() != null && !p.getTipoSangre().isBlank()
                        ? p.getTipoSangre() : "No registrado");
                alergiasLabel.setText(p.getAlergias() != null && !p.getAlergias().isBlank()
                        ? p.getAlergias() : "Ninguna registrada");
            } else {
                bienvenidaLabel.setText("Bienvenido, " + usuario.getUsername());
            }
        } else {
            bienvenidaLabel.setText("Bienvenido, " + usuario.getUsername());
            nombreCompletoLabel.setText(usuario.getUsername());
            correoLabel.setText("—");
            telefonoLabel.setText("—");
            edadLabel.setText("—");
            tipoSangreLabel.setText("—");
            alergiasLabel.setText("—");
        }
        rolLabel.setText("Paciente");
    }

    /* ── Navegación: Autodiagnóstico IA ────────────────────── */

    @FXML
    public void onAutodiagnostico(MouseEvent event) {
        navegarA("/com/mediconsult/beta/view/fxml/autodiagnostico_paciente.fxml",
                 "CLINEX — Diagnóstico IA");
    }

    /* ── Navegación: Historial Clínico ─────────────────────── */

    @FXML
    public void onHistorialClinico(MouseEvent event) {
        navegarA("/com/mediconsult/beta/view/fxml/historial_clinico.fxml",
                 "CLINEX — Historial Clínico");
    }

    /* ── Navegación: Notificaciones ────────────────────────── */

    @FXML
    public void onNotificaciones(MouseEvent event) {
        navegarA("/com/mediconsult/beta/view/fxml/notificaciones_paciente.fxml",
                 "CLINEX — Notificaciones");
    }

    /* ── Navegación: Órdenes Médicas ───────────────────────── */

    @FXML
    public void onOrdenesMedicas(MouseEvent event) {
        navegarA("/com/mediconsult/beta/view/fxml/ordenes_paciente.fxml",
                 "CLINEX — Mis Órdenes Médicas");
    }

    /* ── Navegación: Agendar Cita ───────────────────────────── */

    @FXML
    public void onAgendarCita(MouseEvent event) {
        navegarA("/com/mediconsult/beta/view/fxml/citas_paciente.fxml",
                 "CLINEX — Mis Citas");
    }

    /* ── Cerrar sesión ──────────────────────────────────────── */

    @FXML
    public void onCerrarSesion(ActionEvent event) {
        Sesion.cerrar();
        navegarA("/com/mediconsult/beta/view/fxml/login.fxml", "CLINEX — Iniciar Sesión");
    }

    /* ── Utilidad de navegación ──────────────────────────────── */

    private void navegarA(String fxmlPath, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) bienvenidaLabel.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(titulo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
