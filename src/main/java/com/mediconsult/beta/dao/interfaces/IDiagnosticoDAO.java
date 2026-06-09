package com.mediconsult.beta.dao.interfaces;

import com.mediconsult.beta.model.Diagnostico;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad {@link Diagnostico}.
 *
 * <p>Gestiona la persistencia de los diagnósticos asistidos por IA.
 * El método {@link #listarPorPaciente(int)} es especialmente relevante
 * porque requiere cruzar {@code diagnosticos.csv} con {@code citas.csv},
 * ya que {@code diagnosticos} no almacena directamente el {@code pacienteId};
 * esta lógica de cruce es responsabilidad exclusiva de la implementación del DAO.</p>
 *
 * <p><strong>Relación cita-diagnóstico:</strong> Se asume una relación
 * uno a uno: cada cita puede tener como máximo un diagnóstico.
 * Por eso {@link #buscarPorCita(int)} retorna {@link Optional} y no
 * {@link List}.</p>
 *
 * <p><strong>Manejo de errores:</strong> Las implementaciones deben capturar
 * {@code IOException} internamente y relanzarla envuelta en
 * {@code RuntimeException}, de modo que la capa {@code service} no necesite
 * importar {@code java.io}.</p>
 *
 * <p><strong>Regla de capa:</strong> Solo las clases de {@code service}
 * deben referenciar esta interfaz. Las vistas (view) no deben
 * acceder directamente a ningún DAO.</p>
 *
 * @author MediConsult Team
 * @version 0.1
 * @see com.mediconsult.beta.dao.impl.DiagnosticoDAOImpl
 */
public interface IDiagnosticoDAO {

    /**
     * Persiste un nuevo diagnóstico en el archivo CSV.
     *
     * <p>Si la operación es exitosa, el campo {@code id} del objeto recibido
     * se actualiza con el identificador generado (max id + 1).</p>
     *
     * <p>El campo {@code resultadoIa} puede estar vacío en el momento del
     * {@code guardar} si el diagnóstico se crea antes de consultar a
     * OpenAI; en ese caso se actualiza posteriormente con
     * {@link #actualizar(Diagnostico)}.</p>
     *
     * @param diagnostico entidad a insertar; {@code diagnostico.getId()} debe ser 0
     * @throws RuntimeException si ocurre un error al escribir el archivo
     */
    void guardar(Diagnostico diagnostico);

    /**
     * Actualiza un diagnóstico existente en el archivo CSV.
     *
     * <p>Principalmente usado para guardar el {@code resultadoIa} una vez
     * que {@code DiagnosticoService} recibe la respuesta de OpenAI,
     * y también para actualizar los {@code sintomas} si el médico los corrige.</p>
     *
     * @param diagnostico entidad con los datos actualizados; debe tener un
     *                    {@code id} válido (distinto de 0)
     * @throws RuntimeException si ocurre un error al escribir el archivo
     */
    void actualizar(Diagnostico diagnostico);

    /**
     * Busca un diagnóstico por su identificador único.
     *
     * @param id identificador del diagnóstico
     * @return {@link Optional} con el diagnóstico si existe,
     *         o vacío si no se encuentra
     * @throws RuntimeException si ocurre un error al leer el archivo
     */
    Optional<Diagnostico> buscarPorId(int id);

    /**
     * Busca el diagnóstico asociado a una cita específica.
     *
     * <p>Dado que la relación cita-diagnóstico es uno a uno, retorna
     * un {@link Optional}. Si la cita aún no tiene diagnóstico registrado,
     * el Optional estará vacío.</p>
     *
     * <p>La implementación filtra en memoria buscando la primera entrada
     * de {@code diagnosticos.csv} donde {@code citaId} coincida.</p>
     *
     * @param citaId identificador de la cita
     * @return {@link Optional} con el diagnóstico si existe para esa cita,
     *         o vacío si la cita no tiene diagnóstico aún
     * @throws RuntimeException si ocurre un error al leer el archivo
     */
    Optional<Diagnostico> buscarPorCita(int citaId);

    /**
     * Recupera todos los diagnósticos generados para un paciente específico,
     * a lo largo de todas sus citas.
     *
     * <p>Esta consulta requiere cruzar {@code diagnosticos.csv} con
     * {@code citas.csv} para acceder al {@code pacienteId}. El cruce
     * se realiza en memoria dentro de la implementación:</p>
     * <ol>
     *   <li>Cargar todas las citas del paciente desde {@code citas.csv}.</li>
     *   <li>Recolectar sus ids.</li>
     *   <li>Filtrar {@code diagnosticos.csv} retaining solo los que
     *       tengan un {@code citaId} en ese conjunto.</li>
     *   <li>Ordenar por fecha de cita descendente.</li>
     * </ol>
     *
     * <p>El cruce es responsabilidad exclusiva de la implementación;
     * la capa {@code service} recibe únicamente la lista de objetos
     * {@link Diagnostico} sin conocer cómo se obtuvo.</p>
     *
     * @param pacienteId identificador del paciente
     * @return lista de diagnósticos del paciente ordenados por fecha
     *         descendente; lista vacía si no tiene diagnósticos
     * @throws RuntimeException si ocurre un error al leer alguno de los archivos
     */
    List<Diagnostico> listarPorPaciente(int pacienteId);
}
