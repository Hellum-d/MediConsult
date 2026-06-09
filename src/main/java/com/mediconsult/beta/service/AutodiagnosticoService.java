package com.mediconsult.beta.service;

import com.mediconsult.beta.dao.impl.AutodiagnosticoDAOImpl;
import com.mediconsult.beta.dao.impl.PacienteDAOImpl;
import com.mediconsult.beta.dao.interfaces.IAutodiagnosticoDAO;
import com.mediconsult.beta.dao.interfaces.IPacienteDAO;
import com.mediconsult.beta.model.Autodiagnostico;
import com.mediconsult.beta.util.OpenAIClient;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de autodiagnóstico IA para el paciente — CLINEX.
 *
 * <p>Único componente autorizado a instanciar {@link OpenAIClient} para
 * el flujo de autoservicio del paciente. Orquesta el flujo completo:</p>
 * <ol>
 *   <li>Persiste el autodiagnóstico con resultado vacío (fail-safe).</li>
 *   <li>Consulta OpenAI GPT-4o-mini con los síntomas.</li>
 *   <li>Actualiza el registro con la respuesta.</li>
 *   <li>Acumula una entrada en el historial clínico del paciente.</li>
 * </ol>
 */
public class AutodiagnosticoService {

    private final IAutodiagnosticoDAO autodiagnosticoDAO;
    private final IPacienteDAO        pacienteDAO;
    private final OpenAIClient        openAIClient;

    public AutodiagnosticoService() {
        this.autodiagnosticoDAO = new AutodiagnosticoDAOImpl();
        this.pacienteDAO        = new PacienteDAOImpl();
        this.openAIClient       = new OpenAIClient();
    }

    // ── Operación principal ───────────────────────────────────────────────

    /**
     * Genera un autodiagnóstico preliminar para el paciente.
     *
     * @param pacienteId id del paciente autenticado
     * @param sintomas   síntomas descritos por el paciente
     * @return autodiagnóstico con el resultado de la IA
     */
    public Autodiagnostico generarAutodiagnostico(int pacienteId, String sintomas) {
        validar(sintomas != null && !sintomas.trim().isEmpty(),
                "Los síntomas no pueden estar vacíos.");

        // Paso 1: Persistir registro inicial (fail-safe antes de llamar a la IA)
        Autodiagnostico ad = new Autodiagnostico(pacienteId, sintomas.trim());
        autodiagnosticoDAO.guardar(ad);

        // Paso 2: Consultar OpenAI
        String resultadoIa;
        try {
            resultadoIa = openAIClient.consultarDiagnostico(sintomas.trim());
        } catch (Exception e) {
            resultadoIa = "Error al consultar la IA: " + e.getMessage();
            System.err.println("[AutodiagnosticoService] " + resultadoIa);
        }

        // Paso 3: Actualizar con el resultado
        ad.setResultadoIa(resultadoIa);
        autodiagnosticoDAO.actualizarResultado(ad.getId(), resultadoIa);

        // Paso 4: Registrar en historial del paciente
        registrarEnHistorial(pacienteId, sintomas.trim(), resultadoIa);

        return ad;
    }

    /**
     * Genera un autodiagnóstico con análisis de imagen médica (radiografía,
     * ecografía, examen de laboratorio, foto clínica, etc.).
     *
     * @param pacienteId id del paciente autenticado
     * @param sintomas   síntomas descritos (puede ser vacío si solo hay imagen)
     * @param imagen     archivo de imagen seleccionado por el paciente
     * @return autodiagnóstico con resultado de la IA incluyendo análisis visual
     */
    public Autodiagnostico generarAutodiagnosticoConImagen(int pacienteId,
                                                           String sintomas,
                                                           File imagen) {
        validar(imagen != null && imagen.exists(), "El archivo de imagen no existe.");
        validar(imagen.length() <= 20 * 1024 * 1024L,
                "La imagen no puede superar 20 MB.");

        String sintomasFinales = (sintomas != null && !sintomas.isBlank())
                ? sintomas.trim()
                : "Paciente adjunta imagen para análisis sin síntomas adicionales.";

        // Paso 1: persistir con ruta de imagen (fail-safe)
        Autodiagnostico ad = new Autodiagnostico(pacienteId, sintomasFinales);
        ad.setImagenPath(imagen.getAbsolutePath());
        autodiagnosticoDAO.guardar(ad);

        // Paso 2: llamar a OpenAI con visión
        String resultadoIa;
        try {
            resultadoIa = openAIClient.consultarConImagen(sintomasFinales, imagen.getAbsolutePath());
        } catch (Exception e) {
            resultadoIa = "Error al analizar la imagen: " + e.getMessage();
            System.err.println("[AutodiagnosticoService] " + resultadoIa);
        }

        // Paso 3: actualizar resultado
        ad.setResultadoIa(resultadoIa);
        autodiagnosticoDAO.actualizarResultado(ad.getId(), resultadoIa);

        // Paso 4: registrar en historial
        registrarEnHistorial(pacienteId,
                "📷 " + sintomasFinales + " [imagen: " + imagen.getName() + "]",
                resultadoIa);

        return ad;
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    public List<Autodiagnostico> listarPorPaciente(int pacienteId) {
        return autodiagnosticoDAO.listarPorPaciente(pacienteId);
    }

    // ── Historial ─────────────────────────────────────────────────────────

    private void registrarEnHistorial(int pacienteId, String sintomas, String resultadoIa) {
        try {
            pacienteDAO.buscarPorId(pacienteId).ifPresent(p -> {
                String historialActual = p.getHistorial() != null ? p.getHistorial() : "";
                String entrada = "[" + LocalDate.now() + "] Autodiagnóstico IA — Síntomas: "
                        + sintomas + " → " + resumirResultado(resultadoIa);
                String historialNuevo = historialActual.trim().isEmpty()
                        ? entrada
                        : entrada + "\n" + historialActual;
                pacienteDAO.actualizarHistorial(pacienteId, historialNuevo);
            });
        } catch (Exception e) {
            System.err.println("[AutodiagnosticoService] No se pudo actualizar el historial: "
                    + e.getMessage());
        }
    }

    private String resumirResultado(String resultadoIa) {
        if (resultadoIa == null || resultadoIa.isBlank()) return "Sin resultado";
        for (String linea : resultadoIa.split("\n")) {
            if (linea.trim().startsWith("GRAVEDAD:")) {
                return linea.trim();
            }
        }
        return resultadoIa.length() > 80 ? resultadoIa.substring(0, 80) + "…" : resultadoIa;
    }

    private void validar(boolean cond, String msg) {
        if (!cond) throw new IllegalArgumentException(msg);
    }
}
