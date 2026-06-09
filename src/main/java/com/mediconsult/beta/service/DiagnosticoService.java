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
 * Servicio de negocio para diagnósticos asistidos por IA en MediConsult-Beta.
 *
 * <p>Es el <strong>único</strong> componente del sistema autorizado a instanciar
 * y llamar a {@link OpenAIClient}. Orquesta el flujo completo de generación de
 * diagnóstico:</p>
 * <ol>
 *   <li>Persiste el diagnóstico con {@code resultadoIa} vacío (fail-safe).</li>
 *   <li>Consulta a OpenAI GPT-4o-mini con los síntomas del médico.</li>
 *   <li>Actualiza el diagnóstico con la respuesta recibida.</li>
 *   <li>Acumula una entrada en el historial del paciente.</li>
 * </ol>
 *
 * <p>Si la llamada a la IA falla (red caída, timeout, API key inválida),
 * el registro de síntomas ya está guardado en el CSV y el médico
 * puede reintentar sin perder información.</p>
 *
 * @author MediConsult Team
 * @version 0.1
 */
public class DiagnosticoService {

    private final IDiagnosticoDAO diagnosticoDAO;
    private final IPacienteDAO    pacienteDAO;
    private final OpenAIClient    openAIClient;

    /** Constructor por defecto: instancia los DAOs y el cliente de IA reales. */
    public DiagnosticoService() {
        this.diagnosticoDAO = new DiagnosticoDAOImpl();
        this.pacienteDAO    = new PacienteDAOImpl();
        this.openAIClient   = new OpenAIClient();
    }

    /** Constructor para inyección de dependencia (tests o alternativas). */
    public DiagnosticoService(IDiagnosticoDAO diagnosticoDAO,
                               IPacienteDAO pacienteDAO,
                               OpenAIClient openAIClient) {
        this.diagnosticoDAO = diagnosticoDAO;
        this.pacienteDAO    = pacienteDAO;
        this.openAIClient   = openAIClient;
    }

    // ── Operación principal ───────────────────────────────────────────────

    /**
     * Genera un diagnóstico asistido por OpenAI GPT-4o-mini para una cita dada.
     *
     * <p><strong>Flujo detallado:</strong></p>
     * <ol>
     *   <li>Valida que los síntomas no estén vacíos.</li>
     *   <li>Crea un {@link Diagnostico} con {@code resultadoIa} vacío y lo
     *       persiste (garantiza que el registro no se pierda si la IA falla).</li>
     *   <li>Llama a {@link OpenAIClient#consultarDiagnostico(String)} con
     *       los síntomas. Si la llamada falla, retorna el diagnóstico
     *       con el mensaje de error en {@code resultadoIa}.</li>
     *   <li>Actualiza el diagnóstico con la respuesta de la IA.</li>
     *   <li>Acumula una entrada en el historial del paciente.</li>
     * </ol>
     *
     * @param citaId     identificador de la cita asociada al diagnóstico
     * @param pacienteId identificador del paciente (para actualizar historial)
     * @param sintomas   síntomas descritos por el médico; no puede estar vacío
     * @return diagnóstico completo con {@code resultadoIa} de la IA
     * @throws IllegalArgumentException si los síntomas están vacíos
     */
    public Diagnostico generarDiagnostico(int citaId, int pacienteId, String sintomas) {
        validar(sintomas != null && !sintomas.trim().isEmpty(),
                "Los síntomas no pueden estar vacíos para generar un diagnóstico.");

        // Paso 1: Crear y persistir el diagnóstico con resultadoIa vacío (fail-safe)
        Diagnostico diagnostico = new Diagnostico(citaId, sintomas.trim());
        diagnosticoDAO.guardar(diagnostico);

        // Paso 2: Consultar a OpenAI GPT-4o-mini
        String resultadoIa;
        try {
            resultadoIa = openAIClient.consultarDiagnostico(sintomas.trim());
        } catch (Exception e) {
            // Captura cualquier error inesperado no manejado en el cliente
            resultadoIa = "Error inesperado al consultar la IA: " + e.getMessage();
            System.err.println("[DiagnosticoService] " + resultadoIa);
        }

        // Paso 3: Actualizar el diagnóstico con la respuesta de la IA
        diagnostico.setResultadoIa(resultadoIa);
        diagnosticoDAO.actualizar(diagnostico);

        // Paso 4: Acumular entrada en el historial del paciente
        actualizarHistorialPaciente(pacienteId, sintomas.trim(), resultadoIa);

        return diagnostico;
    }

    // ── Operaciones de consulta ───────────────────────────────────────────

    /**
     * Busca el diagnóstico asociado a una cita específica.
     *
     * @param citaId identificador de la cita
     * @return {@link Optional} con el diagnóstico, o vacío si aún no se generó
     */
    public Optional<Diagnostico> obtenerPorCita(int citaId) {
        return diagnosticoDAO.buscarPorCita(citaId);
    }

    /**
     * Busca un diagnóstico por su identificador único.
     *
     * @param id identificador del diagnóstico
     * @return {@link Optional} con el diagnóstico, o vacío si no existe
     */
    public Optional<Diagnostico> obtenerPorId(int id) {
        return diagnosticoDAO.buscarPorId(id);
    }

    /**
     * Retorna todos los diagnósticos de un paciente, más recientes primero.
     *
     * <p>El DAO realiza internamente el cruce con {@code citas.csv} para
     * acceder al {@code pacienteId}, ya que {@code diagnosticos.csv} no lo
     * almacena directamente.</p>
     *
     * @param pacienteId identificador del paciente
     * @return lista de diagnósticos; vacía si no tiene ninguno
     */
    public List<Diagnostico> listarPorPaciente(int pacienteId) {
        return diagnosticoDAO.listarPorPaciente(pacienteId);
    }

    // ── Métodos privados auxiliares ───────────────────────────────────────

    /**
     * Acumula una nueva entrada de diagnóstico en el historial del paciente.
     *
     * <p>Formato de la entrada añadida al historial:</p>
     * <pre>Diagnóstico: [síntomas] → [resultado IA]</pre>
     *
     * <p>Si el paciente no existe o la actualización falla, se registra el
     * error en consola pero no se interrumpe el flujo principal (el diagnóstico
     * ya fue guardado correctamente).</p>
     *
     * @param pacienteId  identificador del paciente
     * @param sintomas    síntomas del diagnóstico
     * @param resultadoIa respuesta de la IA
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

                String historialNuevo = historialActual.trim().isEmpty()
                        ? nuevaEntrada
                        : nuevaEntrada + "\n" + historialActual;

                pacienteDAO.actualizarHistorial(pacienteId, historialNuevo);
            });
        } catch (Exception e) {
            System.err.println("[DiagnosticoService] No se pudo actualizar el historial "
                    + "del paciente id=" + pacienteId + ": " + e.getMessage());
        }
    }

    private void validar(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalArgumentException(mensaje);
    }
}
