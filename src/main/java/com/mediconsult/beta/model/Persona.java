package com.mediconsult.beta.model;

/**
 * Clase abstracta base que representa a una persona dentro del sistema Clinex.
 *
 * <p>Paciente y Medico heredan de esta clase. Ahora incluye apellido, telefono,
 * direccion y correo para alinearse con la tabla persona en MySQL.</p>
 *
 * @author MediConsult Team
 * @version 0.2 — migración JDBC
 */
public abstract class Persona {

    private int    id;
    private String nombre;
    private String apellido;
    private String telefono;
    private String direccion;
    private String correo;

    // ── Constructor completo ───────────────────────────────────────────────

    public Persona(int id, String nombre, String apellido,
                   String telefono, String direccion, String correo) {
        this.id        = id;
        this.nombre    = nombre;
        this.apellido  = apellido;
        this.telefono  = telefono;
        this.direccion = direccion;
        this.correo    = correo;
    }

    /** Constructor mínimo para compatibilidad con código existente.
     * @param id
     * @param nombre */
    public Persona(int id, String nombre) {
        this(id, nombre, "", "", "", "");
    }

    // ── Método abstracto ───────────────────────────────────────────────────

    public abstract String getResumen();

    // ── Getters y Setters ──────────────────────────────────────────────────

    public int    getId()        { return id; }
    public void   setId(int id)  { this.id = id; }

    public String getNombre()              { return nombre; }
    public void   setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido()                { return apellido; }
    public void   setApellido(String apellido) { this.apellido = apellido; }

    public String getTelefono()                { return telefono; }
    public void   setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion()                  { return direccion; }
    public void   setDireccion(String direccion)  { this.direccion = direccion; }

    public String getCorreo()              { return correo; }
    public void   setCorreo(String correo) { this.correo = correo; }

    /** Nombre completo: nombre + apellido.
     * @return  */
    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    @Override
    public String toString() {
        return "Persona{id=" + id + ", nombre='" + nombre + " " + apellido + "'}";
    }
}
