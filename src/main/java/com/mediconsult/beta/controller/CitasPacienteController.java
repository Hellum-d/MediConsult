package com.mediconsult.beta.controller;

import com.mediconsult.beta.model.Cita;
import com.mediconsult.beta.model.Medico;
import com.mediconsult.beta.service.CitaService;
import com.mediconsult.beta.service.MedicoService;
import com.mediconsult.beta.service.PacienteService;
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

/**
 * Controller de citas_paciente.fxml — CLINEX
 *
 * Flujo corregido: el paciente solo registra el motivo de consulta.
 * El administrador revisa la solicitud, asigna médico, fecha y hora,
 * y la convierte en cita PENDIENTE confirmada.
 */
public class CitasPacienteController {

    /* ── Inyección FXML ─────────────────────────────────────── */

    @FXML private Label     nombrePacienteLabel;
    @FXML private TextField motivoField;
    @FXML private Label     errorLabel;
    @FXML private Button    solicitarBtn;

    @FXML private VBox  citasListView;
    @FXML private Label statPendientesLabel;
    @FXML private Label statTotalLabel;

    /* ── Servicios ──────────────────────────────────────────── */

    private final CitaService    citaService    = new CitaService();
    private final MedicoService  medicoService  = new MedicoService();
    private final PacienteService pacienteService = new PacienteService();

    private int pacienteId;

    /* ── Inicialización ─────────────────────────────────────── */

    @FXML
    public void initialize() {
        if (Sesion.getUsuario() != null) {
            pacienteId = Sesion.getUsuario().getPacienteId();
            nombrePacienteLabel.setText("Paciente · " + Sesion.getUsuario().getUsername());
        }
        motivoField.textProperty().addListener((obs, o, n) -> clearError());
        cargarCitas();
    }

    /* ── Enviar solicitud de cita ────────────────────────────── */

    @FXML
    public void onSolicitarCita(ActionEvent event) {
        String motivo = motivoField.getText().trim();

        if (motivo.isBlank()) {
            showError("Describe el motivo de tu consulta antes de enviar la solicitud.");
            return;
        }
        if (pacienteId <= 0) {
            showError("No se encontró la cuenta de paciente asociada a esta sesión.");
            return;
        }

        try {
            citaService.solicitarCita(pacienteId, motivo);

            Alert ok = new Alert(Alert.AlertType.INFORMATION,
                    "Solicitud enviada exitosamente.\n\n"
                    + "El administrador revisará tu solicitud y te asignará un médico, "
                    + "fecha y horario. Recibirás una notificación con la confirmación.",
                    ButtonType.OK);
            ok.setTitle("Solicitud enviada — CLINEX");
            ok.setHeaderText("Solicitud registrada correctamente");
            ok.showAndWait();

            motivoField.clear();
            cargarCitas();

        } catch (Exception e) {
            showError("Error al enviar la solicitud: " + e.getMessage());
        }
    }

    /* ── Cargar y renderizar citas del paciente ─────────────── */

    private void cargarCitas() {
        citasListView.getChildren().clear();

        if (pacienteId <= 0) return;

        try {
            List<Cita> citas = citaService.listarPorPaciente(pacienteId);

            long pendientes = citas.stream()
                    .filter(c -> Cita.ESTADO_PENDIENTE.equals(c.getEstado())
                              || Cita.ESTADO_SOLICITUD.equals(c.getEstado()))
                    .count();
            statPendientesLabel.setText(String.valueOf(pendientes));
            statTotalLabel.setText(String.valueOf(citas.size()));

            if (citas.isEmpty()) {
                Label empty = new Label("Aún no tienes citas ni solicitudes registradas.");
                empty.setStyle("-fx-font-size:13px; -fx-text-fill:#73777f; -fx-font-style:italic; -fx-padding:12 0;");
                citasListView.getChildren().add(empty);
                return;
            }

            for (Cita c : citas) {
                citasListView.getChildren().add(crearTarjetaCita(c));
            }

        } catch (Exception e) {
            Label err = new Label("No se pudieron cargar las citas: " + e.getMessage());
            err.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");
            citasListView.getChildren().add(err);
        }
    }

    private HBox crearTarjetaCita(Cita c) {
        HBox card = new HBox(14);
        card.getStyleClass().add("appointment-card");
        card.setAlignment(Pos.CENTER_LEFT);

        boolean esSolicitud = Cita.ESTADO_SOLICITUD.equals(c.getEstado());

        // Fecha + hora (null para solicitudes)
        VBox fechaBox = new VBox(4);
        fechaBox.setAlignment(Pos.CENTER);
        fechaBox.setMinWidth(80);

        if (esSolicitud || c.getFecha() == null) {
            Label pendL = new Label("Por");
            pendL.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#73777f;");
            Label pendL2 = new Label("definir");
            pendL2.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f;");
            fechaBox.getChildren().addAll(pendL, pendL2);
        } else {
            String diaStr = c.getFecha().format(DateTimeFormatter.ofPattern("dd/MM"));
            Label diaLabel = new Label(diaStr);
            diaLabel.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");
            Label anioLabel = new Label(String.valueOf(c.getFecha().getYear()));
            anioLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#73777f;");
            fechaBox.getChildren().addAll(diaLabel, anioLabel);
        }

        // Chip de hora
        Label horaChip = new Label(c.getHora() != null ? c.getHora() : "—");
        horaChip.getStyleClass().add("time-chip");
        if (esSolicitud || c.getHora() == null) {
            horaChip.setStyle("-fx-background-color:#f3f4f6; -fx-text-fill:#9ca3af; " +
                              "-fx-background-radius:6; -fx-padding:4 10; -fx-font-size:12px;");
        }

        // Datos del médico o indicador de solicitud
        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        String nombreMedico;
        if (esSolicitud || c.getMedicoId() <= 0) {
            nombreMedico = "Médico por asignar";
        } else {
            nombreMedico = "Médico ID " + c.getMedicoId();
            try {
                Optional<Medico> m = medicoService.obtenerPorId(c.getMedicoId());
                if (m.isPresent()) nombreMedico = "Dr(a). " + m.get().getNombreCompleto();
            } catch (Exception ignored) {}
        }
        Label medicoLabel = new Label(nombreMedico);
        medicoLabel.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");

        String motivoStr = (c.getMotivo() != null && !c.getMotivo().isBlank())
                ? c.getMotivo() : "Sin motivo especificado";
        if (motivoStr.length() > 60) motivoStr = motivoStr.substring(0, 60) + "…";
        Label motivoLabel = new Label(motivoStr);
        motivoLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f;");

        infoBox.getChildren().addAll(medicoLabel, motivoLabel);

        // Badge de estado
        Label estadoBadge = new Label();
        switch (c.getEstado()) {
            case Cita.ESTADO_SOLICITUD  -> { estadoBadge.setText("⏳ En revisión");
                                             estadoBadge.setStyle("-fx-background-color:#EEF2FF; -fx-background-radius:12; " +
                                                 "-fx-padding:3 10; -fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#4338CA;"); }
            case Cita.ESTADO_PENDIENTE  -> { estadoBadge.setText("● Pendiente");   estadoBadge.getStyleClass().add("status-pending"); }
            case Cita.ESTADO_COMPLETADA -> { estadoBadge.setText("✓ Completada");  estadoBadge.getStyleClass().add("status-completed"); }
            case Cita.ESTADO_CANCELADA  -> { estadoBadge.setText("✕ Cancelada");  estadoBadge.getStyleClass().add("status-cancelled"); }
            default                     -> { estadoBadge.setText(c.getEstado());   estadoBadge.getStyleClass().add("status-pending"); }
        }

        HBox rightBox = new HBox(10);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.getChildren().add(estadoBadge);

        // Botón cancelar solo para PENDIENTE (no para SOLICITUD)
        if (Cita.ESTADO_PENDIENTE.equals(c.getEstado())) {
            Button cancelBtn = new Button("Cancelar");
            cancelBtn.getStyleClass().add("btn-danger");
            cancelBtn.setOnAction(ev -> confirmarCancelacion(c));
            rightBox.getChildren().add(cancelBtn);
        }

        card.getChildren().addAll(fechaBox, horaChip, infoBox, rightBox);
        return card;
    }

    private void confirmarCancelacion(Cita c) {
        String fechaStr = c.getFecha() != null
                ? c.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Cancelar la cita del " + fechaStr + " a las " + c.getHora()
                + "?\nEsta acción no se puede deshacer.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Cancelar cita — CLINEX");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                try {
                    citaService.cancelarCita(c.getId());
                    cargarCitas();
                } catch (Exception ex) {
                    showError("No se pudo cancelar: " + ex.getMessage());
                }
            }
        });
    }

    /* ── Navegación ─────────────────────────────────────────── */

    @FXML
    public void onVolverPortal(ActionEvent event) {
        navegarA("/com/mediconsult/beta/view/fxml/portal_paciente.fxml",
                 "CLINEX — Portal del Paciente");
    }

    @FXML
    public void onCerrarSesion(ActionEvent event) {
        Sesion.cerrar();
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
            Stage stage = (Stage) solicitarBtn.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(titulo);
        } catch (IOException e) {
            showError("No se pudo navegar: " + e.getMessage());
        }
    }
}
