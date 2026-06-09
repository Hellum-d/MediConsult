package com.mediconsult.beta.dao.interfaces;

import com.mediconsult.beta.model.Paciente;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la entidad {@link Paciente}.
 *
 * <p>Define las operaciones CRUD completas más consultas específicas del
 * dominio hospitalario. La implementación concreta ({@code PacienteDAOImpl})
 * es la única responsable de leer y escribir en {@code pacientes.csv};
 * esta interfaz solo expone tipos del modelo y de la librería estándar de Java.</p>
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
 * @see com.mediconsult.beta.dao.impl.PacienteDAOImpl
 */
public interface IPacienteDAO {

    /**
     * Persiste un nuevo paciente en el archivo CSV.
     *
     * <p>Si la operación es exitosa, el campo {@code id} del objeto recibido
     * se actualiza con el identificador generado (max id + 1).</p>
     *
     * @param paciente entidad a insertar; {@code paciente.getId()} debe ser 0
     * @throws RuntimeException si ocurre un error al escribir el archivo
     */
    void guardar(Paciente paciente);

    /**
     * Actualiza los datos de un paciente existente en el archivo CSV.
     *
     * <p>Se actualizan los campos: {@code nombre}, {@code edad},
     * {@code telefono} e {@code historial}. El {@code id} se usa como
     * criterio de búsqueda y no se modifica.</p>
     *
     * @param paciente entidad con los datos actualizados; debe tener un
     *                 {@code id} válido (distinto de 0)
     * @throws RuntimeException si ocurre un error al escribir el archivo
     */
    void actualizar(Paciente paciente);

    /**
     * Elimina un paciente del archivo CSV por su identificador.
     *
     * <p><strong>Advertencia:</strong> Esta operación no verifica la existencia
     * de citas o diagnósticos dependientes. El {@code service} debe manejar
     * esta situación antes de invocar este método.</p>
     *
     * @param id identificador del paciente a eliminar
     * @throws RuntimeException si ocurre un error al escribir el archivo
     */
    void eliminar(int id);

    /**
     * Busca un paciente por su identificador único.
     *
     * @param id identificador del paciente
     * @return {@link Optional} con el paciente si existe, o vacío si no se encuentra
     * @throws RuntimeException si ocurre un error al leer el archivo
     */
    Optional<Paciente> buscarPorId(int id);

    /**
     * Recupera todos los pacientes registrados en el sistema.
     *
     * @return lista de pacientes; lista vacía si no hay registros
     * @throws RuntimeException si ocurre un error al leer el archivo
     */
    List<Paciente> listarTodos();

    /**
     * Busca pacientes cuyo nombre contenga la cadena indicada
     * (búsqueda parcial, insensible a mayúsculas/minúsculas).
     *
     * <p>La implementación filtra en memoria usando
     * {@code nombre.toLowerCase().contains(criterio.toLowerCase())}.</p>
     *
     * @param nombre cadena de búsqueda (puede ser nombre completo o parcial)
     * @return lista de pacientes que coinciden; lista vacía si no hay resultados
     * @throws RuntimeException si ocurre un error al leer el archivo
     */
    List<Paciente> buscarPorNombre(String nombre);

    /**
     * Actualiza únicamente el campo {@code historial} de un paciente.
     *
     * <p>Este método existe como operación de primera clase porque el historial
     * se enriquece cada vez que se genera un diagnóstico, sin necesidad de
     * reescribir todos los demás campos del paciente.</p>
     *
     * @param id       identificador del paciente
     * @param historial nuevo contenido del historial médico
     * @throws RuntimeException si ocurre un error al escribir el archivo
     */
    void actualizarHistorial(int id, String historial);

    /** Retorna todos los pacientes marcados como inactivos (solo admin). */
    List<Paciente> listarInactivos();

    /** Reactiva un paciente previamente inactivado. */
    void reactivar(int id);
}
