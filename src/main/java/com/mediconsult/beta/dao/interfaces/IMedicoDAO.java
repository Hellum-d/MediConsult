package com.mediconsult.beta.dao.interfaces;

import com.mediconsult.beta.model.Medico;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad {@link Medico}.
 *
 * <p>Define las operaciones CRUD completas más consultas específicas para
 * la gestión de médicos: filtrado por disponibilidad y por especialidad.
 * Estas consultas son utilizadas principalmente por {@code CitaService}
 * para verificar que un médico puede recibir nuevas citas.</p>
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
 * @see com.mediconsult.beta.dao.impl.MedicoDAOImpl
 */
public interface IMedicoDAO {

    /**
     * Persiste un nuevo médico en el archivo CSV.
     *
     * <p>Si la operación es exitosa, el campo {@code id} del objeto recibido
     * se actualiza con el identificador generado (max id + 1).</p>
     *
     * @param medico entidad a insertar; {@code medico.getId()} debe ser 0
     * @throws RuntimeException si ocurre un error al escribir el archivo
     */
    void guardar(Medico medico);

    /**
     * Actualiza los datos de un médico existente en el archivo CSV.
     *
     * <p>Se actualizan los campos: {@code nombre}, {@code especialidad}
     * y {@code disponible}. El {@code id} se usa como criterio de búsqueda
     * y no se modifica.</p>
     *
     * @param medico entidad con los datos actualizados; debe tener un
     *               {@code id} válido (distinto de 0)
     * @throws RuntimeException si ocurre un error al escribir el archivo
     */
    void actualizar(Medico medico);

    /**
     * Elimina un médico del archivo CSV por su identificador.
     *
     * <p><strong>Advertencia:</strong> Esta operación no verifica la existencia
     * de citas dependientes. El {@code service} debe manejar esta situación
     * antes de invocar este método.</p>
     *
     * @param id identificador del médico a eliminar
     * @throws RuntimeException si ocurre un error al escribir el archivo
     */
    void eliminar(int id);

    /**
     * Busca un médico por su identificador único.
     *
     * @param id identificador del médico
     * @return {@link Optional} con el médico si existe, o vacío si no se encuentra
     * @throws RuntimeException si ocurre un error al leer el archivo
     */
    Optional<Medico> buscarPorId(int id);

    /**
     * Recupera todos los médicos registrados en el sistema.
     *
     * @return lista de médicos; lista vacía si no hay registros
     * @throws RuntimeException si ocurre un error al leer el archivo
     */
    List<Medico> listarTodos();

    /**
     * Recupera únicamente los médicos marcados como disponibles
     * para recibir nuevas citas ({@code disponible = true}).
     *
     * <p>La implementación filtra en memoria sobre la lista completa
     * retornada por {@link #listarTodos()}.</p>
     *
     * <p>Este método es el punto de entrada principal para mostrar
     * los médicos disponibles en el menú de agendamiento de citas.</p>
     *
     * @return lista de médicos disponibles; lista vacía si ninguno está disponible
     * @throws RuntimeException si ocurre un error al leer el archivo
     */
    List<Medico> listarDisponibles();

    /**
     * Busca médicos cuya especialidad contenga la cadena indicada
     * (búsqueda parcial, insensible a mayúsculas/minúsculas).
     *
     * <p>La implementación filtra en memoria usando
     * {@code especialidad.toLowerCase().contains(criterio.toLowerCase())}.</p>
     *
     * @param especialidad cadena de búsqueda (nombre completo o parcial)
     * @return lista de médicos que coinciden; lista vacía si no hay resultados
     * @throws RuntimeException si ocurre un error al leer el archivo
     */
    List<Medico> buscarPorEspecialidad(String especialidad);

    /** Retorna todos los médicos marcados como inactivos (solo admin). */
    List<Medico> listarInactivos();

    /** Reactiva un médico previamente inactivado. */
    void reactivar(int id);
}
