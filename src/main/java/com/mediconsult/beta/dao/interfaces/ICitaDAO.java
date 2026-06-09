package com.mediconsult.beta.dao.interfaces;

import com.mediconsult.beta.model.Cita;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad {@link Cita}.
 *
 * <p>Además del CRUD estándar, expone consultas específicas para el flujo de
 * agendamiento: historial de citas por paciente, agenda por médico y
 * verificación de disponibilidad antes de crear una nueva cita.</p>
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
 * @see com.mediconsult.beta.dao.impl.CitaDAOImpl
 */
public interface ICitaDAO {

    /**
     * Persiste una nueva cita en el archivo CSV.
     *
     * <p>Si la operación es exitosa, el campo {@code id} del objeto recibido
     * se actualiza con el identificador generado (max id + 1).</p>
     *
     * <p><strong>Precondición:</strong> El {@code CitaService} debe invocar
     * {@link #existeCitaEnFecha(int, LocalDate)} antes de llamar a este
     * método para garantizar la disponibilidad del médico.</p>
     *
     * @param cita entidad a insertar; {@code cita.getId()} debe ser 0
     * @throws RuntimeException si ocurre un error al escribir el archivo
     */
    void guardar(Cita cita);

    /**
     * Actualiza los datos de una cita existente en el archivo CSV.
     *
     * <p>Permite modificar la fecha y el estado de la cita
     * ({@code PENDIENTE → COMPLETADA | CANCELADA}). El {@code id} se usa
     * como criterio de búsqueda y no se modifica.</p>
     *
     * @param cita entidad con los datos actualizados; debe tener un
     *             {@code id} válido (distinto de 0)
     * @throws RuntimeException si ocurre un error al escribir el archivo
     */
    void actualizar(Cita cita);

    /**
     * Elimina una cita del archivo CSV por su identificador.
     *
     * <p><strong>Advertencia:</strong> Esta operación no verifica la existencia
     * de diagnósticos dependientes. Considerar cancelar la cita (cambio de
     * estado) en lugar de eliminarla.</p>
     *
     * @param id identificador de la cita a eliminar
     * @throws RuntimeException si ocurre un error al escribir el archivo
     */
    void eliminar(int id);

    /**
     * Busca una cita por su identificador único.
     *
     * @param id identificador de la cita
     * @return {@link Optional} con la cita si existe, o vacío si no se encuentra
     * @throws RuntimeException si ocurre un error al leer el archivo
     */
    Optional<Cita> buscarPorId(int id);

    /**
     * Recupera todas las citas registradas en el sistema.
     *
     * @return lista de todas las citas; lista vacía si no hay registros
     * @throws RuntimeException si ocurre un error al leer el archivo
     */
    List<Cita> listarTodos();

    /**
     * Recupera todas las citas asociadas a un paciente específico,
     * ordenadas por fecha descendente (la más reciente primero).
     *
     * <p>Usado para construir el historial clínico del paciente en el
     * menú de detalle de paciente.</p>
     *
     * <p>La implementación filtra en memoria sobre la lista completa y
     * ordena con {@code Comparator.comparing(Cita::getFecha).reversed()}.</p>
     *
     * @param pacienteId identificador del paciente
     * @return lista de citas del paciente; lista vacía si no tiene citas
     * @throws RuntimeException si ocurre un error al leer el archivo
     */
    List<Cita> listarPorPaciente(int pacienteId);

    /**
     * Recupera todas las citas asignadas a un médico específico,
     * ordenadas por fecha ascendente (agenda cronológica).
     *
     * <p>La implementación filtra en memoria sobre la lista completa y
     * ordena con {@code Comparator.comparing(Cita::getFecha)}.</p>
     *
     * @param medicoId identificador del médico
     * @return lista de citas del médico; lista vacía si no tiene citas
     * @throws RuntimeException si ocurre un error al leer el archivo
     */
    List<Cita> listarPorMedico(int medicoId);

    /**
     * Verifica si un médico ya tiene una cita activa (no cancelada)
     * en la fecha indicada.
     *
     * <p>Este método es la pieza central del mecanismo de verificación de
     * disponibilidad. El {@code CitaService} debe invocarlo antes de
     * persistir cualquier nueva cita.</p>
     *
     * <p>La implementación busca en memoria una cita donde
     * {@code medicoId} y {@code fecha} coincidan y el estado sea distinto
     * de {@code CANCELADA}.</p>
     *
     * @param medicoId identificador del médico
     * @param fecha    fecha a verificar
     * @return {@code true} si el médico ya tiene una cita activa esa fecha,
     *         {@code false} si está disponible
     * @throws RuntimeException si ocurre un error al leer el archivo
     */
    boolean existeCitaEnFecha(int medicoId, LocalDate fecha);

    /** Verifica si el médico tiene una cita activa (no cancelada) en esa fecha Y hora específicas. */
    boolean existeCitaEnFechaHora(int medicoId, LocalDate fecha, String hora);

    /** Retorna las horas ocupadas (no canceladas) del médico en esa fecha. */
    List<String> horasOcupadasEnFecha(int medicoId, LocalDate fecha);

    /** Retorna todas las citas con estado SOLICITUD, ordenadas por id descendente. */
    List<Cita> listarSolicitudes();
}
