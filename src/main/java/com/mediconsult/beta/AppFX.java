package com.mediconsult.beta;

import com.mediconsult.beta.model.Usuario;
import com.mediconsult.beta.util.ArchivoUtil;
import com.mediconsult.beta.util.ConexionDB;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.fxml.FXMLLoader; 
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;
  
/**
 * Punto de entrada de la interfaz gráfica de MediConsult-Beta (CLINEX UI).
 *
 * <p>Esta clase es la alternativa visual a {@link Main} (modo consola).
 * Ambos modos coexisten: el fat-jar CLI sigue funcionando igual; la UI
 * se lanza con:</p>
 * <pre>
 *   mvn javafx:run
 * </pre>
 *
 * <p><strong>Secuencia de arranque:</strong></p>
 * <ol>
 *   <li>Sembrar usuarios por defecto si la base de datos está vacía.</li>
 *   <li>Cargar {@code login.fxml} como escena inicial.</li>
 *   <li>Configurar el {@link Stage} principal (tamaño, título, ícono).</li>
 * </ol>
 *
 * <p><strong>Nota sobre el módulo JavaFX:</strong> La clase que contiene
 * {@code main()} NO debe extender {@code Application} directamente cuando
 * el proyecto no usa {@code module-info.java}, porque el launcher de JavaFX
 * necesita que la clase {@code Application} esté separada del método de
 * entrada para funcionar correctamente con classpath (sin módulos).</p>
 *
 * @author MediConsult Team
 * @version 0.2
 */
public class AppFX extends Application {

    // ── Constantes de la ventana ───────────────────────────────────────────

    /** Ancho inicial de la ventana principal (px). */
    private static final double ANCHO_INICIAL  = 1280;

    /** Alto inicial de la ventana principal (px). */
    private static final double ALTO_INICIAL   = 800;

    /** Ancho mínimo permitido (evita que el layout colapse). */
    private static final double ANCHO_MINIMO   = 900;

    /** Alto mínimo permitido. */
    private static final double ALTO_MINIMO    = 600;

    /** Título de la barra de ventana. */
    private static final String TITULO         = "CLINEX — MediConsult Beta v0.2";

    /** Ruta al FXML de login dentro de resources. */
    private static final String FXML_LOGIN     =
            "/com/mediconsult/beta/view/fxml/login.fxml";

    /** Archivo CSV de usuarios (relativo al directorio de trabajo). */
    private static final String ARCHIVO_USUARIOS = "usuarios.csv";

    // ── JavaFX Application lifecycle ──────────────────────────────────────

    /**
     * Llamado por JavaFX tras inicializar el toolkit.
     * Construye la escena de login y muestra la ventana.
     *
     * @param stage ventana principal provista por el runtime de JavaFX
     * @throws IOException si {@code login.fxml} no se encuentra en el classpath
     */
    @Override
    public void start(Stage stage) throws IOException {

        // 0. Verificar la conexión a MySQL antes de cargar cualquier vista
        if (!verificarConexion()) {
            Platform.exit();
            return;
        }

        // 1. Cargar el FXML de login
        FXMLLoader loader = new FXMLLoader(getClass().getResource(FXML_LOGIN));
        Parent root = loader.load();

        // 2. Construir la escena
        Scene scene = new Scene(root, ANCHO_INICIAL, ALTO_INICIAL);

        // 3. Configurar el Stage
        stage.setScene(scene);
        stage.setTitle(TITULO);
        stage.setMinWidth(ANCHO_MINIMO);
        stage.setMinHeight(ALTO_MINIMO);

        // 4. Ícono de la aplicación (si existe en resources)
        try {
            Image icono = new Image(
                    getClass().getResourceAsStream("/com/mediconsult/beta/icon.png"));
            if (!icono.isError()) {
                stage.getIcons().add(icono);
            }
        } catch (Exception ignored) {
            // Ícono opcional — si no existe, la barra usará el ícono por defecto de JavaFX
        }

        // 5. Comportamiento al cerrar la ventana
        stage.setOnCloseRequest(event -> {
            // Cerrar sesión activa antes de salir
            com.mediconsult.beta.util.Sesion.cerrar();
        });

        stage.show();
    }

    /**
     * Intenta conectar a MySQL. Si falla, muestra un diálogo con la causa
     * y cómo configurar la conexión, en lugar de una excepción en consola.
     *
     * @return {@code true} si la conexión se estableció
     */
    private boolean verificarConexion() {
        try {
            ConexionDB.getInstance();
            return true;
        } catch (RuntimeException e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            String detalle =
                    "Causa: " + causa.getMessage() + "\n\n"
                  + ConexionDB.descripcionConfiguracion() + "\n\n"
                  + "Verifica que MySQL esté encendido y que hayas ejecutado DB.sql.\n"
                  + "Si tu usuario o contraseña no son root / 1234, define las\n"
                  + "variables de entorno y vuelve a abrir la aplicación:\n\n"
                  + "  setx MYSQL_USER \"tu_usuario\"\n"
                  + "  setx MYSQL_PASSWORD \"tu_contraseña\"\n\n"
                  + "(Opcional: MYSQL_URL para otro host, puerto o base de datos.)";
            System.err.println("[AppFX] No se pudo conectar a MySQL.\n" + detalle);

            TextArea texto = new TextArea(detalle);
            texto.setEditable(false);
            texto.setWrapText(true);

            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle(TITULO);
            alerta.setHeaderText("No se pudo conectar a la base de datos MySQL");
            alerta.getDialogPane().setContent(texto);
            alerta.showAndWait();
            return false;
        }
    }

    // ── Punto de entrada de la UI ──────────────────────────────────────────

    /**
     * Método main separado de {@link Application} para compatibilidad con
     * classpath sin {@code module-info.java}.
     *
     * <p>Primero siembra usuarios por defecto (si la BD está vacía), luego
     * lanza el toolkit de JavaFX que invocará {@link #start(Stage)}.</p>
     *
     * @param args argumentos de línea de comando (no usados)
     */
    public static void main(String[] args) {
        sembrarUsuarios();
        launch(args);
    }

    // ── Semilla de usuarios por defecto ───────────────────────────────────

    /**
     * Crea usuarios por defecto en {@code usuarios.csv} si el archivo está vacío.
     *
     * <p>Usuarios creados:</p>
     * <ul>
     *   <li>{@code admin} / {@code admin123} — rol ADMIN</li>
     *   <li>{@code medico} / {@code medico123} — rol MEDICO</li>
     * </ul>
     *
     * <p>Esta lógica es idéntica a la de {@link Main#sembrarUsuarios()}
     * para garantizar que ambos modos (CLI y UI) arranquen con los mismos datos.</p>
     */
    private static void sembrarUsuarios() {
        List<String> lineas = ArchivoUtil.leerLineas(ARCHIVO_USUARIOS);
        if (!lineas.isEmpty()) return; // ya tiene datos, no tocar

        System.out.println("[AppFX] Creando usuarios por defecto...");

        // Usuario admin
        String hashAdmin = Usuario.hashPassword("admin123");
        ArchivoUtil.agregarLinea(ARCHIVO_USUARIOS,
                "1" + ArchivoUtil.SEPARADOR
                + "admin" + ArchivoUtil.SEPARADOR
                + hashAdmin + ArchivoUtil.SEPARADOR
                + Usuario.ROL_ADMIN);

        // Usuario médico
        String hashMedico = Usuario.hashPassword("medico123");
        ArchivoUtil.agregarLinea(ARCHIVO_USUARIOS,
                "2" + ArchivoUtil.SEPARADOR
                + "medico" + ArchivoUtil.SEPARADOR
                + hashMedico + ArchivoUtil.SEPARADOR
                + Usuario.ROL_MEDICO);

        System.out.println("[AppFX] Usuarios por defecto listos:");
        System.out.println("         admin  / admin123  (ADMIN)");
        System.out.println("         medico / medico123 (MEDICO)");
    }
}
