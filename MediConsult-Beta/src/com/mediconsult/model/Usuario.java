package com.mediconsult.model;

/**
 * Credenciales de acceso al sistema.
 * La contrasena se almacena hasheada con SHA-256.
 */
public class Usuario {

    private int id;
    private String username;
    private String password; // SHA-256
    private String rol;      // ADMIN | MEDICO

    public Usuario(int id, String username, String password, String rol) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.rol = rol;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    @Override
    public String toString() {
        return "Usuario{id=" + id + ", username='" + username + "', rol='" + rol + "'}";
    }
}
