package com.mediconsult.beta.service;

import com.mediconsult.beta.dao.impl.DiagnosticoDAOImpl;
import com.mediconsult.beta.dao.impl.PacienteDAOImpl;
import com.mediconsult.beta.dao.interfaces.IDiagnosticoDAO;
import com.mediconsult.beta.dao.interfaces.IPacienteDAO;
import com.mediconsult.beta.model.Diagnostico;
import com.mediconsult.beta.util.OpenAIClient;

import java.util.List;
import java.util.Optional;

/**
 * Servicio encargado de generar diagnósticos usando IA.
 *
 * Es el único módulo autorizado a usar OpenAIClient.
 * Orquesta todo el flujo: guardar, consultar IA y actualizar historial.
 */
public class DiagnosticoService {

    private final IDiagnosticoDAO diagnosticoDAO;
    private final IPacienteDAO    pacienteDAO;
    private final OpenAIClient    openAIClient;

    /** Usa implementaciones reales de DAOs y cliente IA. */
    public DiagnosticoService() {
        this.diagnosticoDAO = new DiagnosticoDAOImpl();
        this.pacienteDAO    = new PacienteDAOImpl();
        this.openAIClient   = new OpenAIClient();
    }

    /** Constructor para pruebas o inyección de dependencias. */
    public DiagnosticoService(IDiagnosticoDAO diagnosticoDAO,
                               IPacienteDAO pacienteDAO,
                               OpenAIClient openAIClient) {
        this.diagnosticoDAO = diagnosticoDAO;
        this.pacienteDAO    = pacienteDAO;
        this.openAIClient   = openAIClient;
    }

    // ── Flujo principal ───────────────────────────────────────────────────

    /**
     * Genera un diagnóstico completo usando IA.
     *
     * Flujo:
     * 1. Valida síntomas.
     * 2. Guarda diagnóstico vacío (seguridad ante fallos).
     * 3. Consulta la IA.
     * 4. Actualiza resultado.
     * 5. Registra en historial del paciente.
     * @param citaId
     * @param pacienteId
     * @param sintomas
     * @return 
     */
    public Diagnostico generarDiagnostico(int citaId, int pacienteId, String sintomas) {
        validar(sintomas != null && !sintomas.isBlank(),
                "Los síntomas no pueden estar vacíos para generar un diagnóstico.");

        Diagnostico diagnostico = new Diagnostico(citaId, sintomas.trim());
        diagnosticoDAO.guardar(diagnostico);

        String resultadoIa;
        try {
            resultadoIa = openAIClient.consultarDiagnostico(sintomas.trim());
        } catch (Exception e) {
            resultadoIa = "Error inesperado al consultar la IA: " + e.getMessage();
            System.err.println("[DiagnosticoService] " + resultadoIa);
        }

        diagnostico.setResultadoIa(resultadoIa);
        diagnosticoDAO.actualizar(diagnostico);

        actualizarHistorialPaciente(pacienteId, sintomas.trim(), resultadoIa);

        return diagnostico;
    }

    // ── Consultas ────────────────────────────────────────────────────────

    /** Busca diagnóstico por cita.
     * @param citaId
     * @return  */
    public Optional<Diagnostico> obtenerPorCita(int citaId) {
        return diagnosticoDAO.buscarPorCita(citaId);
    }

    /** Busca diagnóstico por ID.
     * @param id
     * @return  */
    public Optional<Diagnostico> obtenerPorId(int id) {
        return diagnosticoDAO.buscarPorId(id);
    }

    /** Lista diagnósticos de un paciente (más recientes primero).
     * @param pacienteId
     * @return  */
    public List<Diagnostico> listarPorPaciente(int pacienteId) {
        return diagnosticoDAO.listarPorPaciente(pacienteId);
    }

    // ── Historial ─────────────────────────────────────────────────────────

    /**
     * Agrega una entrada al historial del paciente.
     *
     * Formato:
     * Diagnóstico: síntomas → resultado IA
     *
     * Si falla, solo se registra el error sin romper el flujo.
     */
    private void actualizarHistorialPaciente(int pacienteId,
                                              String sintomas,
                                              String resultadoIa) {
        try {
            pacienteDAO.buscarPorId(pacienteId).ifPresent(paciente -> {
                String historialActual = (paciente.getHistorial() == null)
                        ? "" : paciente.getHistorial();

                String nuevaEntrada = "Diagnóstico: " + sintomas
                        + " → " + resultadoIa;

                String historialNuevo = historialActual.isBlank()
                        ? nuevaEntrada
                        : nuevaEntrada + "\n" + historialActual;

                pacienteDAO.actualizarHistorial(pacienteId, historialNuevo);
            });
        } catch (Exception e) {
            System.err.println("[DiagnosticoService] No se pudo actualizar historial "
                    + "del paciente id=" + pacienteId + ": " + e.getMessage());
        }
    }

    /** Validación simple reutilizable. */
    private void validar(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalArgumentException(mensaje);
    }
}