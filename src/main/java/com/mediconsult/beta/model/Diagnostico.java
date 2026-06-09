package com.mediconsult.beta.model;

/**
 * Representa un diagnóstico generado para una cita médica en MediConsult-Beta.
 *
 * <p>El médico ingresa los síntomas; el sistema consulta OpenAI (GPT-4o-mini)
 * y almacena la respuesta en {@code resultadoIa}. Ambos campos se persisten
 * en {@code diagnosticos.csv}.</p>
 *
 * <p><strong>Flujo:</strong></p>
 * <ol>
 *   <li>La vista ({@code MenuDiagnostico}) captura los síntomas.</li>
 *   <li>{@code DiagnosticoService} llama a {@code OpenAIClient}.</li>
 *   <li>El resultado se almacena aquí y se persiste vía {@code DiagnosticoDAOImpl}.</li>
 * </ol>
 *
 * <p><strong>Regla de capa:</strong> Esta clase no importa ningún paquete de
 * las capas superiores (dao, service, view).</p>
 *
 * @author MediConsult Team
 * @version 0.1
 */
public class Diagnostico {

    /** Identificador único. Valor {@code 0} significa "no persistido aún". */
    private int id;

    /** Clave foránea a la cita asociada ({@code citas.csv → id}). */
    private int citaId;

    /** Síntomas descritos por el médico. Se usa como parte del prompt a la IA. */
    private String sintomas;

    /**
     * Respuesta devuelta por OpenAI GPT-4o-mini.
     * Puede ser {@code null} o vacío si la llamada a la IA aún no se realizó.
     */
    private String resultadoIa;

    // ── Constructores ──────────────────────────────────────────────────────

    /**
     * Constructor completo; se usa al cargar un diagnóstico desde el CSV.
     *
     * @param id          identificador único (0 si es nuevo)
     * @param citaId      FK a la cita correspondiente
     * @param sintomas    síntomas ingresados por el médico
     * @param resultadoIa respuesta de OpenAI (puede ser null o vacío)
     */
    public Diagnostico(int id, int citaId, String sintomas, String resultadoIa) {
        this.id          = id;
        this.citaId      = citaId;
        this.sintomas    = sintomas;
        this.resultadoIa = resultadoIa;
    }

    /**
     * Constructor conveniente para crear un diagnóstico nuevo antes de consultar la IA.
     * El campo {@code resultadoIa} se inicializa como cadena vacía.
     *
     * @param citaId   FK a la cita correspondiente
     * @param sintomas síntomas descritos por el médico
     */
    public Diagnostico(int citaId, String sintomas) {
        this(0, citaId, sintomas, "");
    }

    // ── Getters y Setters ──────────────────────────────────────────────────

    /** @return identificador único del diagnóstico */
    public int getId() { return id; }

    /** @param id nuevo identificador (asignado tras persistir en CSV) */
    public void setId(int id) { this.id = id; }

    /** @return FK a la cita asociada */
    public int getCitaId() { return citaId; }

    /** @param citaId nuevo FK a la cita */
    public void setCitaId(int citaId) { this.citaId = citaId; }

    /** @return síntomas ingresados por el médico */
    public String getSintomas() { return sintomas; }

    /** @param sintomas nuevos síntomas */
    public void setSintomas(String sintomas) { this.sintomas = sintomas; }

    /** @return resultado generado por OpenAI; puede ser null o vacío */
    public String getResultadoIa() { return resultadoIa; }

    /** @param resultadoIa respuesta recibida de OpenAI */
    public void setResultadoIa(String resultadoIa) { this.resultadoIa = resultadoIa; }

    // ── toString ───────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Diagnostico{id=" + id
                + ", citaId=" + citaId
                + ", sintomas='" + sintomas + "'"
                + ", resultadoIa='" + resultadoIa + "'}";
    }
}
