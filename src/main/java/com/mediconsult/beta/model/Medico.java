package com.mediconsult.beta.model;

/**
 * Representa a un médico dentro del sistema Clinex.
 *
 * <p>Extiende {@link Persona} añadiendo especialidad, disponibilidad y estado
 * clínico (uno de cinco valores: Disponible, En consulta, Descansando,
 * Fuera de turno, Emergencia).</p>
 *
 * @author MediConsult Team
 * @version 0.3 — estados clínicos múltiples
 */
public class Medico extends Persona {

    // ── Constantes de estado ───────────────────────────────────────────────

    public static final String ESTADO_DISPONIBLE   = "Disponible";
    public static final String ESTADO_EN_CONSULTA  = "En consulta";
    public static final String ESTADO_DESCANSANDO  = "Descansando";
    public static final String ESTADO_FUERA_TURNO  = "Fuera de turno";
    public static final String ESTADO_EMERGENCIA   = "Emergencia";

    /**
     * Valor centinela almacenado en la columna {@code especialidad} cuando el
     * médico es de tipo General. La columna DB es NOT NULL, por eso se usa
     * esta cadena en lugar de NULL.
     */
    public static final String ESPECIALIDAD_GENERAL = "Médico General";

    // ── Campos ────────────────────────────────────────────────────────────

    private String  especialidad;
    private boolean disponible;

    /**
     * Estado clínico del médico. Persiste en columna {@code estado} de la BD.
     * Si la columna aún no existe, se infiere de {@code disponible} al cargar.
     */
    private String estado;

    // ── Constructores ──────────────────────────────────────────────────────

    /**
     * Constructor completo (incluye estado). Usado por el DAO al leer de MySQL
     * cuando la columna {@code estado} ya existe.
     */
    public Medico(int id, String nombre, String apellido,
                  String telefono, String direccion, String correo,
                  String especialidad, boolean disponible, String estado) {
        super(id, nombre, apellido, telefono, direccion, correo);
        this.especialidad = especialidad;
        this.disponible   = disponible;
        this.estado       = (estado != null && !estado.isBlank()) ? estado
                            : (disponible ? ESTADO_DISPONIBLE : ESTADO_FUERA_TURNO);
    }

    /**
     * Constructor de compatibilidad (sin estado): infiere el estado desde
     * {@code disponible}. Se mantiene para evitar romper código existente.
     */
    public Medico(int id, String nombre, String apellido,
                  String telefono, String direccion, String correo,
                  String especialidad, boolean disponible) {
        this(id, nombre, apellido, telefono, direccion, correo, especialidad,
             disponible, disponible ? ESTADO_DISPONIBLE : ESTADO_FUERA_TURNO);
    }

    /** Constructor conveniente para registrar un médico nuevo (disponible por defecto). */
    public Medico(String nombre, String apellido, String especialidad, String correo) {
        this(0, nombre, apellido, "", "", correo, especialidad, true, ESTADO_DISPONIBLE);
    }

    // ── Método abstracto heredado ──────────────────────────────────────────

    @Override
    public String getResumen() {
        return "Dr(a). " + getNombreCompleto() + " — " + especialidad;
    }

    // ── Getters y Setters ──────────────────────────────────────────────────

    public String  getEspecialidad()                    { return especialidad; }
    public void    setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    /** {@code true} si el médico tiene una especialidad clínica real (no es Médico General). */
    public boolean isEspecialista() {
        return especialidad != null && !especialidad.isBlank()
               && !ESPECIALIDAD_GENERAL.equals(especialidad);
    }

    /** Devuelve la especialidad para mostrar en UI; nunca null ni vacío. */
    public String getEspecialidadDisplay() {
        return (especialidad != null && !especialidad.isBlank()) ? especialidad : ESPECIALIDAD_GENERAL;
    }

    public boolean isDisponible()                       { return disponible; }
    public void    setDisponible(boolean disponible)    { this.disponible = disponible; }

    public String  getEstado()                          { return estado; }

    /**
     * Cambia el estado clínico y sincroniza el flag {@code disponible}:
     * solo "Disponible" pone {@code disponible = true}.
     */
    public void setEstado(String estado) {
        this.estado    = (estado != null) ? estado : ESTADO_DISPONIBLE;
        this.disponible = ESTADO_DISPONIBLE.equals(this.estado);
    }

    @Override
    public String toString() {
        return "Medico{id=" + getId()
                + ", nombre='" + getNombreCompleto() + "'"
                + ", especialidad='" + especialidad + "'"
                + ", estado='" + estado + "'}";
    }
}
