package com.mediconsult.beta.controller;

import com.mediconsult.beta.model.Cita;
import com.mediconsult.beta.model.Medico;
import com.mediconsult.beta.model.Paciente;
import com.mediconsult.beta.service.CitaService;
import com.mediconsult.beta.service.MedicoService;
import com.mediconsult.beta.service.PacienteService;
import com.mediconsult.beta.util.NotificacionUtil;
import com.mediconsult.beta.util.TopBarUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Controller de citas.fxml — CLINEX
 *
 * <p>Gestiona el calendario de citas, muestra la lista del día seleccionado
 * y permite agendar nuevas citas mediante el modal integrado en el FXML.</p>
 *
 * @author MediConsult Team
 */
public class CitasController {

    /* ── Inyección FXML ─────────────────────────────────────── */

    @FXML private HBox  topBar;

    // Calendario
    @FXML private Label     calendarMonthLabel;
    @FXML private Button    prevMonthBtn;
    @FXML private Button    nextMonthBtn;
    @FXML private GridPane  calendarGrid;
    @FXML private Label     citasHoyLabel;

    // Solicitudes
    @FXML private VBox  solicitudesView;
    @FXML private Label solicitudesCountLabel;

    // Lista de citas
    @FXML private VBox  citasListView;

    // FAB
    @FXML private Button newAppointmentBtn;

    // Modal de nueva cita
    @FXML private StackPane  scheduleModal;
    @FXML private ComboBox<String> modalPacienteCombo;
    @FXML private ComboBox<String> modalMedicoCombo;
    @FXML private DatePicker modalFechaField;
    @FXML private TextField  modalHoraField;
    @FXML private Button     modalConfirmBtn;
    @FXML private Button     modalCancelBtn;

    // Navegación
    @FXML private Button navPacientesBtn;
    @FXML private Button navMedicosBtn;
    @FXML private Button navCitasBtn;
    @FXML private Button navDiagnosticoBtn;

    /* ── Servicios ──────────────────────────────────────────── */

    private final CitaService     citaService     = new CitaService();
    private final PacienteService pacienteService = new PacienteService();
    private final MedicoService   medicoService   = new MedicoService();

    /* ── Estado interno ─────────────────────────────────────── */

    /** Mes actualmente mostrado en el calendario. */
    private YearMonth mesActual = YearMonth.now();

    /** Día seleccionado en el calendario (por defecto hoy). */
    private LocalDate diaSeleccionado = LocalDate.now();

    /** Formatter para mostrar fechas en encabezados. */
    private static final DateTimeFormatter FMT_MES =
            DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "ES"));

    /* ── Inicialización ─────────────────────────────────────── */

    private static final List<String> SLOTS_HORA =
            List.of("08:00", "09:00", "10:00", "11:00", "14:00", "15:00", "16:00", "17:00");

    @FXML
    public void initialize() {
        TopBarUtil.configurar(topBar);
        renderizarCalendario();
        cargarCitasDelDia(diaSeleccionado);
        cargarCombosModal();
        cargarSolicitudes();
    }

    /* ── Calendario ─────────────────────────────────────────── */

    /** Genera el grid de días para el mes actual. */
    private void renderizarCalendario() {
        // Encabezado del mes
        String mesStr = mesActual.atDay(1).format(FMT_MES);
        calendarMonthLabel.setText(
                mesStr.substring(0, 1).toUpperCase() + mesStr.substring(1));

        // Limpiar celdas anteriores (filas 1..6)
        calendarGrid.getChildren().removeIf(node ->
                GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) > 0);

        LocalDate primerDia = mesActual.atDay(1);
        // Lunes = 1 ... Domingo = 7; ajustamos a columna 0-6
        int colInicio = primerDia.getDayOfWeek().getValue() - 1;
        int diasEnMes = mesActual.lengthOfMonth();

        // Obtener citas del mes para marcar puntos
        List<Cita> citasDelMes;
        try {
            citasDelMes = citaService.listarTodas();
        } catch (Exception e) {
            citasDelMes = List.of();
        }
        final List<Cita> citasMesRef = citasDelMes;

        int col = colInicio;
        int fila = 1;

        // Días del mes anterior (grises)
        LocalDate diaMesAnterior = primerDia.minusDays(colInicio);
        for (int i = 0; i < colInicio; i++) {
            Label lbl = new Label(String.valueOf(diaMesAnterior.plusDays(i).getDayOfMonth()));
            lbl.getStyleClass().add("calendar-day-other-month");
            lbl.setMinWidth(32);
            lbl.setAlignment(Pos.CENTER);
            GridPane.setConstraints(lbl, i, fila);
            calendarGrid.getChildren().add(lbl);
        }

        // Días del mes actual
        for (int dia = 1; dia <= diasEnMes; dia++) {
            LocalDate fecha = mesActual.atDay(dia);
            boolean esHoy = fecha.equals(LocalDate.now());
            boolean esSeleccionado = fecha.equals(diaSeleccionado);

            // ¿Tiene citas ese día?
            final LocalDate fechaRef = fecha;
            boolean tieneCitas = citasMesRef.stream()
                    .anyMatch(c -> c.getFecha() != null && c.getFecha().equals(fechaRef));

            if (esSeleccionado) {
                // Día seleccionado: botón con fondo navy
                Button btn = new Button(String.valueOf(dia));
                btn.getStyleClass().add("calendar-day-selected");
                if (tieneCitas) btn.setText(dia + "·");
                final LocalDate f = fecha;
                btn.setOnAction(e -> seleccionarDia(f));
                GridPane.setConstraints(btn, col, fila);
                calendarGrid.getChildren().add(btn);

            } else if (esHoy) {
                // Hoy: botón azul
                Button btn = new Button(String.valueOf(dia));
                btn.getStyleClass().add("calendar-day-today");
                if (tieneCitas) btn.setText(dia + "·");
                final LocalDate f = fecha;
                btn.setOnAction(e -> seleccionarDia(f));
                GridPane.setConstraints(btn, col, fila);
                calendarGrid.getChildren().add(btn);

            } else {
                // Día normal
                Button btn = new Button(String.valueOf(dia));
                btn.getStyleClass().add("calendar-day-btn");
                if (tieneCitas) {
                    // Punto indicador de cita
                    btn.setStyle(btn.getStyle() + " -fx-underline:true;");
                }
                final LocalDate f = fecha;
                btn.setOnAction(e -> seleccionarDia(f));
                GridPane.setConstraints(btn, col, fila);
                calendarGrid.getChildren().add(btn);
            }

            col++;
            if (col > 6) {
                col = 0;
                fila++;
            }
        }
    }

    /** Cambia al día seleccionado y recarga la lista de citas. */
    private void seleccionarDia(LocalDate fecha) {
        diaSeleccionado = fecha;
        renderizarCalendario();
        cargarCitasDelDia(fecha);
    }

    @FXML public void onPrevMonth(ActionEvent e) {
        mesActual = mesActual.minusMonths(1);
        renderizarCalendario();
    }

    @FXML public void onNextMonth(ActionEvent e) {
        mesActual = mesActual.plusMonths(1);
        renderizarCalendario();
    }

    /* ── Lista de citas del día ──────────────────────────────── */

    private void cargarCitasDelDia(LocalDate fecha) {
        citasListView.getChildren().clear();

        List<Cita> todas;
        try {
            todas = citaService.listarTodas();
        } catch (Exception ex) {
            todas = List.of();
            mostrarError("No se pudieron cargar las citas: " + ex.getMessage());
        }

        List<Cita> delDia = todas.stream()
                .filter(c -> c.getFecha() != null && c.getFecha().equals(fecha))
                .toList();

        citasHoyLabel.setText(String.valueOf(delDia.size()));

        if (delDia.isEmpty()) {
            Label vacio = new Label("No hay citas para este día.");
            vacio.getStyleClass().add("label-body-lg");
            vacio.setStyle("-fx-padding:20; -fx-text-fill:#73777f;");
            citasListView.getChildren().add(vacio);
            return;
        }

        for (Cita cita : delDia) {
            citasListView.getChildren().add(crearTarjetaCita(cita));
        }
    }

    /** Construye visualmente una tarjeta de cita al estilo Stitch. */
    private HBox crearTarjetaCita(Cita cita) {
        HBox card = new HBox(16);
        card.getStyleClass().add("appointment-card");
        card.setAlignment(Pos.CENTER_LEFT);

        String borderColor;
        String statusStyleClass;
        switch (cita.getEstado()) {
            case Cita.ESTADO_COMPLETADA -> {
                borderColor = "#0EA5E9";
                statusStyleClass = "status-completed";
            }
            case Cita.ESTADO_CANCELADA -> {
                borderColor = "#EF4444";
                statusStyleClass = "status-cancelled";
            }
            default -> {
                borderColor = "#F59E0B";
                statusStyleClass = "status-pending";
            }
        }
        card.setStyle(card.getStyle()
                + " -fx-border-width:0 0 0 4;"
                + " -fx-border-color:" + borderColor + " transparent transparent " + borderColor + ";");

        // Fecha y hora
        VBox horaBox = new VBox(3);
        horaBox.setAlignment(Pos.CENTER);
        horaBox.setMinWidth(90);
        String fechaStr = cita.getFecha() != null
                ? cita.getFecha().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
        Label horaLabel = new Label(fechaStr);
        horaLabel.getStyleClass().add("time-chip");
        Label horaCita = new Label(cita.getHora() != null
                ? cita.getHora().substring(0, Math.min(5, cita.getHora().length())) : "—");
        horaCita.getStyleClass().add("label-body-md");
        horaBox.getChildren().addAll(horaLabel, horaCita);

        // Resolver nombre del paciente
        String nombrePac = "Paciente #" + cita.getPacienteId();
        try {
            Optional<com.mediconsult.beta.model.Paciente> p = pacienteService.obtenerPorId(cita.getPacienteId());
            if (p.isPresent()) nombrePac = p.get().getNombreCompleto();
        } catch (Exception ignored) {}

        // Avatar con inicial del paciente
        StackPane avatarPane = new StackPane();
        avatarPane.setStyle("-fx-background-color:#e5eeff; -fx-background-radius:24;"
                + " -fx-min-width:48; -fx-min-height:48; -fx-max-width:48; -fx-max-height:48;"
                + " -fx-alignment:center;");
        String inicial = nombrePac.isBlank() ? "P" : nombrePac.substring(0, 1).toUpperCase();
        Label avatarLbl = new Label(inicial);
        avatarLbl.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#002444;");
        avatarPane.getChildren().add(avatarLbl);

        // Resolver nombre del médico
        String nombreMed = "Médico #" + cita.getMedicoId();
        try {
            Optional<com.mediconsult.beta.model.Medico> m = medicoService.obtenerPorId(cita.getMedicoId());
            if (m.isPresent()) nombreMed = "Dr(a). " + m.get().getNombreCompleto();
        } catch (Exception ignored) {}

        // Datos de la cita
        VBox datosBox = new VBox(4);
        HBox.setHgrow(datosBox, Priority.ALWAYS);
        Label pacLabel = new Label(nombrePac);
        pacLabel.getStyleClass().add("item-card-name");
        Label medLabel = new Label(nombreMed);
        medLabel.getStyleClass().add("item-card-sub");
        datosBox.getChildren().addAll(pacLabel, medLabel);

        // Estado + acciones
        VBox accionesBox = new VBox(8);
        accionesBox.setAlignment(Pos.TOP_RIGHT);
        Label statusLabel = new Label(cita.getEstado());
        statusLabel.getStyleClass().add(statusStyleClass);

        HBox btnRow = new HBox(4);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        if (cita.getEstado().equals(Cita.ESTADO_PENDIENTE)) {
            Button completarBtn = new Button("✔");
            completarBtn.setStyle("-fx-background-color:transparent; -fx-font-size:15px;"
                    + " -fx-text-fill:#0EA5E9; -fx-cursor:hand; -fx-padding:4;");
            completarBtn.setTooltip(new Tooltip("Completar cita"));
            completarBtn.setOnAction(e -> {
                try {
                    citaService.completarCita(cita.getId());
                    cargarCitasDelDia(diaSeleccionado);
                    renderizarCalendario();
                } catch (Exception ex) {
                    mostrarError("Error: " + ex.getMessage());
                }
            });

            Button cancelarBtn = new Button("✕");
            cancelarBtn.setStyle("-fx-background-color:transparent; -fx-font-size:15px;"
                    + " -fx-text-fill:#EF4444; -fx-cursor:hand; -fx-padding:4;");
            cancelarBtn.setTooltip(new Tooltip("Cancelar cita"));
            cancelarBtn.setOnAction(e -> {
                Stage st = (Stage) calendarMonthLabel.getScene().getWindow();
                boolean ok = NotificacionUtil.confirmar(st,
                        "Cancelar cita",
                        "¿Cancelar la cita #" + cita.getId() + " del paciente?\nEl médico quedará disponible en ese horario.",
                        "Sí, cancelar", "No", true);
                if (ok) {
                    try {
                        citaService.cancelarCita(cita.getId());
                        cargarCitasDelDia(diaSeleccionado);
                        renderizarCalendario();
                        NotificacionUtil.toast(st, NotificacionUtil.Tipo.ADVERTENCIA, "Cita #" + cita.getId() + " cancelada.");
                    } catch (Exception ex) {
                        NotificacionUtil.toast(st, NotificacionUtil.Tipo.ERROR, "Error: " + ex.getMessage());
                    }
                }
            });
            btnRow.getChildren().addAll(completarBtn, cancelarBtn);
        }

        accionesBox.getChildren().addAll(statusLabel, btnRow);
        card.getChildren().addAll(horaBox, avatarPane, datosBox, accionesBox);
        return card;
    }

    /* ── Modal: Nueva Cita ──────────────────────────────────── */

    /** Llena los ComboBox del modal con pacientes y médicos disponibles. */
    private void cargarCombosModal() {
        try {
            List<Paciente> pacientes = pacienteService.listarTodos();
            pacientes.forEach(p ->
                    modalPacienteCombo.getItems().add(p.getId() + " — " + p.getNombreCompleto()));
        } catch (Exception e) {
            modalPacienteCombo.setPromptText("Error al cargar pacientes");
        }

        try {
            List<Medico> medicos = medicoService.listarDisponibles();
            medicos.forEach(m ->
                    modalMedicoCombo.getItems().add(m.getId() + " — " + m.getNombreCompleto()
                            + " (" + m.getEspecialidadDisplay() + ")"));
        } catch (Exception e) {
            modalMedicoCombo.setPromptText("Error al cargar médicos");
        }

        // Fecha por defecto: hoy — bloquear visualmente fechas pasadas
        modalFechaField.setValue(LocalDate.now());
        modalFechaField.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color:#f2f3f7; -fx-text-fill:#c3c6cf;");
                }
            }
        });
    }

    /** Abre el modal de nueva cita. Vinculado al FAB. */
    @FXML
    public void onNewAppointment(ActionEvent event) {
        scheduleModal.setVisible(true);
        scheduleModal.setManaged(true);
    }

    /** Cierra el modal sin guardar. */
    @FXML
    public void onCloseModal(ActionEvent event) {
        scheduleModal.setVisible(false);
        scheduleModal.setManaged(false);
        limpiarModal();
    }

    /** Confirma y guarda la nueva cita. */
    @FXML
    public void onConfirmAppointment(ActionEvent event) {
        String selPaciente = modalPacienteCombo.getValue();
        String selMedico   = modalMedicoCombo.getValue();
        LocalDate fecha    = modalFechaField.getValue();
        String horaStr     = modalHoraField.getText().trim();

        if (selPaciente == null || selMedico == null || fecha == null) {
            mostrarError("Completa todos los campos obligatorios (paciente, médico y fecha).");
            return;
        }
        if (fecha.isBefore(LocalDate.now())) {
            mostrarError("No puedes agendar una cita en una fecha pasada.\nSelecciona hoy o una fecha futura.");
            return;
        }
        if (horaStr.isEmpty()) {
            mostrarError("Ingresa la hora de la cita (ej: 09:30).");
            return;
        }
        if (!horaStr.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            mostrarError("Formato de hora inválido. Usa HH:mm (ej: 09:30).");
            return;
        }

        try {
            int pacienteId = Integer.parseInt(selPaciente.split("\\s—\\s")[0].trim());
            int medicoId   = Integer.parseInt(selMedico.split("\\s—\\s")[0].trim());

            citaService.agendarCita(pacienteId, medicoId, fecha, horaStr, "");

            onCloseModal(event);
            cargarCitasDelDia(diaSeleccionado);
            renderizarCalendario();

            Stage st = (Stage) calendarMonthLabel.getScene().getWindow();
            NotificacionUtil.toast(st, NotificacionUtil.Tipo.EXITO,
                    "Cita agendada correctamente para el " + fecha + ".");

        } catch (NumberFormatException e) {
            mostrarError("Selección de paciente o médico inválida.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarError("No se pudo agendar la cita: " + e.getMessage());
        }
    }

    private void limpiarModal() {
        modalPacienteCombo.setValue(null);
        modalMedicoCombo.setValue(null);
        modalFechaField.setValue(LocalDate.now());
        modalHoraField.clear();
    }

    /* ── Solicitudes de cita ────────────────────────────────── */

    private void cargarSolicitudes() {
        solicitudesView.getChildren().clear();
        try {
            List<Cita> lista = citaService.listarSolicitudes();
            solicitudesCountLabel.setText(String.valueOf(lista.size()));

            if (lista.isEmpty()) {
                Label empty = new Label("No hay solicitudes pendientes.");
                empty.setStyle("-fx-font-size:13px; -fx-text-fill:#73777f; -fx-font-style:italic; -fx-padding:8 0;");
                solicitudesView.getChildren().add(empty);
                return;
            }
            for (Cita c : lista) {
                solicitudesView.getChildren().add(crearTarjetaSolicitud(c));
            }
        } catch (Exception e) {
            Label err = new Label("Error al cargar solicitudes: " + e.getMessage());
            err.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a;");
            solicitudesView.getChildren().add(err);
        }
    }

    private HBox crearTarjetaSolicitud(Cita c) {
        HBox card = new HBox(14);
        card.getStyleClass().add("appointment-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(card.getStyle()
                + " -fx-border-width:0 0 0 4; -fx-border-color:#6366F1 transparent transparent #6366F1;");

        // ID de la solicitud
        VBox idBox = new VBox(4);
        idBox.setAlignment(Pos.CENTER);
        idBox.setMinWidth(60);
        Label idLabel = new Label("#" + c.getId());
        idLabel.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#6366F1;");
        Label tipoLabel = new Label("Solicitud");
        tipoLabel.setStyle("-fx-font-size:10px; -fx-text-fill:#73777f;");
        idBox.getChildren().addAll(idLabel, tipoLabel);

        // Nombre del paciente
        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        String nombrePacTmp = "Paciente ID " + c.getPacienteId();
        try {
            Optional<Paciente> p = pacienteService.obtenerPorId(c.getPacienteId());
            if (p.isPresent()) nombrePacTmp = p.get().getNombreCompleto();
        } catch (Exception ignored) {}
        final String nombrePac = nombrePacTmp;
        Label pacLabel = new Label(nombrePac);
        pacLabel.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");
        String motivo = (c.getMotivo() != null && !c.getMotivo().isBlank())
                ? c.getMotivo() : "Sin motivo especificado";
        if (motivo.length() > 60) motivo = motivo.substring(0, 60) + "…";
        Label motivoLabel = new Label(motivo);
        motivoLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#73777f;");
        infoBox.getChildren().addAll(pacLabel, motivoLabel);

        // Botones de acción
        HBox btnBox = new HBox(8);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button aprobarBtn = new Button("✔  Aprobar");
        aprobarBtn.setStyle("-fx-background-color:#002444; -fx-text-fill:white; " +
                            "-fx-font-weight:bold; -fx-background-radius:8; " +
                            "-fx-padding:6 12; -fx-cursor:hand; -fx-font-size:12px;");
        aprobarBtn.setOnAction(ev -> abrirModalProcesarSolicitud(c));

        Button rechazarBtn = new Button("✕");
        rechazarBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#ba1a1a; " +
                             "-fx-font-size:16px; -fx-cursor:hand; -fx-padding:4 8;");
        rechazarBtn.setTooltip(new Tooltip("Rechazar solicitud"));
        rechazarBtn.setOnAction(ev -> {
            Stage st = (Stage) calendarMonthLabel.getScene().getWindow();
            boolean ok = NotificacionUtil.confirmar(st,
                    "Rechazar solicitud",
                    "¿Rechazar la solicitud #" + c.getId() + " de " + nombrePac + "?\nSe marcará como cancelada.",
                    "Sí, rechazar", "No", true);
            if (ok) {
                try {
                    citaService.cancelarCita(c.getId());
                    cargarSolicitudes();
                    NotificacionUtil.toast(st, NotificacionUtil.Tipo.ADVERTENCIA,
                            "Solicitud #" + c.getId() + " rechazada.");
                } catch (Exception ex) {
                    NotificacionUtil.toast(st, NotificacionUtil.Tipo.ERROR,
                            "Error al rechazar: " + ex.getMessage());
                }
            }
        });

        btnBox.getChildren().addAll(aprobarBtn, rechazarBtn);
        card.getChildren().addAll(idBox, infoBox, btnBox);
        return card;
    }

    private void abrirModalProcesarSolicitud(Cita solicitud) {
        Stage owner = (Stage) calendarMonthLabel.getScene().getWindow();
        Stage modal = new Stage();
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.initOwner(owner);
        modal.initModality(Modality.APPLICATION_MODAL);

        // Nombre del paciente
        String nombrePac = "Paciente ID " + solicitud.getPacienteId();
        try {
            Optional<Paciente> p = pacienteService.obtenerPorId(solicitud.getPacienteId());
            if (p.isPresent()) nombrePac = p.get().getNombreCompleto();
        } catch (Exception ignored) {}

        // ComboBox médicos
        List<Medico> medicosDisp = new ArrayList<>();
        ComboBox<String> medicoCombo = new ComboBox<>();
        try {
            medicosDisp.addAll(medicoService.listarDisponibles());
            medicosDisp.forEach(m -> medicoCombo.getItems().add(
                    "Dr(a). " + m.getNombreCompleto() + "  —  " + m.getEspecialidadDisplay()));
        } catch (Exception ignored) {}
        medicoCombo.setPromptText("Seleccionar médico disponible...");
        medicoCombo.setMaxWidth(Double.MAX_VALUE);
        medicoCombo.setStyle("-fx-background-radius:8; -fx-border-radius:8; " +
                             "-fx-border-color:rgba(46,134,222,0.3); -fx-font-size:13px;");

        // DatePicker — bloquea visualmente fechas pasadas
        DatePicker fechaPicker = new DatePicker(LocalDate.now());
        fechaPicker.setMaxWidth(Double.MAX_VALUE);
        fechaPicker.setStyle("-fx-background-radius:8; -fx-font-size:13px;");
        fechaPicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color:#f2f3f7; -fx-text-fill:#c3c6cf;");
                }
            }
        });

        // ComboBox horarios
        ComboBox<String> horaCombo = new ComboBox<>();
        horaCombo.getItems().addAll(SLOTS_HORA);
        horaCombo.setPromptText("Selecciona médico y fecha primero");
        horaCombo.setMaxWidth(Double.MAX_VALUE);
        horaCombo.setStyle("-fx-background-radius:8; -fx-border-radius:8; " +
                           "-fx-border-color:rgba(46,134,222,0.3); -fx-font-size:13px;");

        List<Medico> medicosRef = medicosDisp;
        Runnable actualizarSlots = () -> {
            int idx = medicoCombo.getSelectionModel().getSelectedIndex();
            LocalDate fecha = fechaPicker.getValue();
            if (idx < 0 || idx >= medicosRef.size() || fecha == null) return;
            horaCombo.getItems().clear();
            int medicoId = medicosRef.get(idx).getId();
            List<String> ocupadas;
            try { ocupadas = citaService.horasOcupadas(medicoId, fecha); }
            catch (Exception ex) { ocupadas = List.of(); }
            List<String> ocup = ocupadas;
            SLOTS_HORA.stream().filter(h -> !ocup.contains(h))
                    .forEach(horaCombo.getItems()::add);
            horaCombo.setPromptText(horaCombo.getItems().isEmpty()
                    ? "Sin horarios disponibles" : "Selecciona un horario");
        };
        medicoCombo.valueProperty().addListener((o, a, b) -> actualizarSlots.run());
        fechaPicker.valueProperty().addListener((o, a, b) -> actualizarSlots.run());

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#ba1a1a; -fx-wrap-text:true;");
        errorLabel.setWrapText(true);

        Button confirmarBtn = new Button("✔  Confirmar Cita");
        confirmarBtn.setStyle("-fx-background-color:#002444; -fx-text-fill:white; " +
                              "-fx-font-weight:bold; -fx-background-radius:10; " +
                              "-fx-padding:10 24; -fx-cursor:hand; -fx-font-size:13px;");

        Button cancelarBtn = new Button("Cancelar");
        cancelarBtn.setStyle("-fx-background-color:transparent; " +
                             "-fx-border-color:rgba(46,134,222,0.35); -fx-border-radius:10; " +
                             "-fx-background-radius:10; -fx-text-fill:#002444; " +
                             "-fx-font-weight:bold; -fx-padding:10 24; -fx-cursor:hand; -fx-font-size:13px;");
        cancelarBtn.setOnAction(e -> modal.close());

        confirmarBtn.setOnAction(e -> {
            int idx = medicoCombo.getSelectionModel().getSelectedIndex();
            LocalDate fecha = fechaPicker.getValue();
            String hora = horaCombo.getValue();
            if (idx < 0 || idx >= medicosRef.size()) { errorLabel.setText("Selecciona un médico."); return; }
            if (fecha == null) { errorLabel.setText("Selecciona una fecha."); return; }
            if (fecha.isBefore(LocalDate.now())) { errorLabel.setText("⚠ No puedes agendar en una fecha pasada. Selecciona hoy o una fecha futura."); return; }
            if (hora == null || hora.isBlank()) { errorLabel.setText("Selecciona un horario disponible."); return; }
            try {
                citaService.procesarSolicitud(solicitud.getId(), medicosRef.get(idx).getId(), fecha, hora);
                modal.close();
                cargarSolicitudes();
                renderizarCalendario();
                NotificacionUtil.toast((Stage) calendarMonthLabel.getScene().getWindow(),
                        NotificacionUtil.Tipo.EXITO,
                        "Solicitud aprobada — cita agendada correctamente.");
            } catch (Exception ex) {
                errorLabel.setText("Error: " + ex.getMessage());
            }
        });

        // Layout del modal
        Label titleL = new Label("Aprobar Solicitud #" + solicitud.getId());
        titleL.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#0b1c30;");

        String motivoMostrar = (solicitud.getMotivo() != null && !solicitud.getMotivo().isBlank())
                ? solicitud.getMotivo() : "Sin motivo especificado";
        Label infoL = new Label("Paciente: " + nombrePac + "\nMotivo: " + motivoMostrar);
        infoL.setStyle("-fx-font-size:12px; -fx-text-fill:#43474e; -fx-wrap-text:true; " +
                       "-fx-background-color:#f8f9ff; -fx-background-radius:8; -fx-padding:8 10;");
        infoL.setWrapText(true);

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.getChildren().addAll(cancelarBtn, confirmarBtn);

        VBox card = new VBox(12);
        card.setStyle("-fx-background-color:#ffffff; -fx-background-radius:16; " +
                      "-fx-padding:28; -fx-max-width:480; " +
                      "-fx-effect:dropshadow(gaussian,rgba(0,36,68,0.22),24,0,0,6);");

        Label lMedico = new Label("MÉDICO DISPONIBLE");
        lMedico.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#2E86DE;");
        Label lFecha = new Label("FECHA");
        lFecha.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#2E86DE;");
        Label lHora = new Label("HORARIO");
        lHora.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#2E86DE;");

        card.getChildren().addAll(titleL, infoL, lMedico, medicoCombo,
                                  lFecha, fechaPicker, lHora, horaCombo,
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

    /* ── Navegación ─────────────────────────────────────────── */

    @FXML public void onNavPacientes(ActionEvent e)   { navegarA("/com/mediconsult/beta/view/fxml/pacientes.fxml",  "CLINEX — Pacientes"); }
    @FXML public void onNavMedicos(ActionEvent e)     { navegarA("/com/mediconsult/beta/view/fxml/medicos.fxml",    "CLINEX — Médicos"); }
    @FXML public void onNavCitas(ActionEvent e)       { /* ya estamos aquí */ }
    @FXML public void onNavDiagnostico(ActionEvent e) { navegarA("/com/mediconsult/beta/view/fxml/diagnostico.fxml","CLINEX — Diagnóstico IA"); }
    @FXML public void onNavRemisiones(ActionEvent e)   { navegarA("/com/mediconsult/beta/view/fxml/gestion_remisiones.fxml", "CLINEX — Remisiones"); }
    @FXML public void onNavListaEspera(ActionEvent e)  { navegarA("/com/mediconsult/beta/view/fxml/lista_espera.fxml",        "CLINEX — Lista de Espera"); }
    @FXML public void onNotificaciones(ActionEvent e) { navegarA("/com/mediconsult/beta/view/fxml/notificaciones_admin.fxml", "CLINEX — Notificaciones"); }

    /* ── Utilidades ─────────────────────────────────────────── */

    private void navegarA(String fxmlPath, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) calendarMonthLabel.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(titulo);
        } catch (IOException ex) {
            mostrarError("No se pudo cargar la pantalla: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void mostrarError(String msg) {
        Stage st = (Stage) calendarMonthLabel.getScene().getWindow();
        NotificacionUtil.alerta(st, NotificacionUtil.Tipo.ERROR, "Error — CLINEX", msg);
    }
}
