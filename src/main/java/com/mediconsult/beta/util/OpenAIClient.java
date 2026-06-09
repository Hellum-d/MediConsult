package com.mediconsult.beta.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

/**
 * Cliente HTTP para la API de OpenAI en el sistema MediConsult-Beta.
 *
 * <p>Encapsula toda la comunicación con el endpoint de OpenAI y expone
 * un único método público: {@link #consultarDiagnostico(String)}, que recibe
 * los síntomas del paciente y retorna un análisis clínico estructurado.</p>
 *
 * <p><strong>Formato de respuesta estructurada:</strong></p>
 * <pre>
 * CAUSAS: causa1 | causa2 | causa3
 * GRAVEDAD: Leve
 * JUSTIFICACION: texto
 * RECOMENDACIONES: rec1 | rec2
 * EXAMENES: exam1 | exam2
 * TRATAMIENTO: texto
 * ESTADO_ACTUAL: texto
 * OBSERVACIONES: texto
 * EVOLUCION: texto
 * REVISION: texto
 * </pre>
 *
 * <p><strong>Regla de capa:</strong> Solo {@code DiagnosticoService} puede
 * instanciar y llamar a esta clase.</p>
 *
 * @author MediConsult Team
 * @version 0.3 — modelo actualizado a gpt-4.1, visión mejorada
 */
public class OpenAIClient {

    private static final String ENDPOINT    = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY     = System.getenv("OPENAI_API_KEY") != null
            ? System.getenv("OPENAI_API_KEY")
            : "CONFIGURE_TU_API_KEY_EN_VARIABLE_DE_ENTORNO";
    private static final String MODELO      = "gpt-4.1";
    private static final int    MAX_TOKENS  = 1200;
    private static final double TEMPERATURE = 0.2;
    private static final Duration TIMEOUT   = Duration.ofSeconds(45);

    private final HttpClient httpClient;

    public OpenAIClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Consulta a OpenAI GPT-4o-mini con los síntomas del paciente y retorna
     * un análisis clínico completo en formato estructurado por secciones.
     *
     * @param sintomas descripción de síntomas; no debe ser null
     * @return respuesta estructurada o mensaje de error si la llamada falla
     */
    public String consultarDiagnostico(String sintomas) {
        if (sintomas == null || sintomas.isBlank()) {
            return "Error: los síntomas no pueden estar vacíos.";
        }

        try {
            String cuerpo = construirBody(sintomas);

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .timeout(TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                    .build();

            HttpResponse<String> respuesta = httpClient.send(
                    peticion, HttpResponse.BodyHandlers.ofString());

            int statusCode = respuesta.statusCode();
            if (statusCode == 200) {
                return extraerContenido(respuesta.body());
            } else {
                System.err.println("[OpenAIClient] HTTP " + statusCode + " — " + respuesta.body());
                return "Error de la API (HTTP " + statusCode
                        + "): verifica la API key o el estado del servicio.";
            }

        } catch (IOException e) {
            System.err.println("[OpenAIClient] IOException: " + e.getMessage());
            return "Error de red al contactar OpenAI: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[OpenAIClient] Petición interrumpida: " + e.getMessage());
            return "La consulta a OpenAI fue interrumpida.";
        }
    }

    /**
     * Consulta a GPT-4o-mini enviando síntomas + una imagen médica (radiografía,
     * examen, foto). Usa el endpoint de visión multimodal de OpenAI.
     *
     * @param sintomas       descripción textual de síntomas (puede ser vacía si solo hay imagen)
     * @param imagenPath     ruta absoluta al archivo de imagen (JPEG, PNG, WEBP, GIF)
     * @return análisis clínico estructurado igual que {@link #consultarDiagnostico}
     */
    public String consultarConImagen(String sintomas, String imagenPath) {
        try {
            byte[] bytesImagen = Files.readAllBytes(Path.of(imagenPath));
            String base64 = Base64.getEncoder().encodeToString(bytesImagen);

            // Detectar MIME type por extensión
            String ext = imagenPath.toLowerCase();
            String mime;
            if (ext.endsWith(".png"))       mime = "image/png";
            else if (ext.endsWith(".webp")) mime = "image/webp";
            else if (ext.endsWith(".gif"))  mime = "image/gif";
            else                            mime = "image/jpeg";

            String dataUrl = "data:" + mime + ";base64," + base64;
            String cuerpo  = construirBodyConImagen(sintomas, dataUrl);

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .timeout(Duration.ofSeconds(90))   // imágenes pueden tardar más con gpt-4.1
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                    .build();

            HttpResponse<String> respuesta = httpClient.send(
                    peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return extraerContenido(respuesta.body());
            } else {
                System.err.println("[OpenAIClient] HTTP " + respuesta.statusCode()
                        + " — " + respuesta.body());
                return "Error de la API (HTTP " + respuesta.statusCode() + "): "
                        + "verifica la API key o el tamaño de la imagen.";
            }

        } catch (IOException e) {
            return "Error al leer la imagen: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "La consulta fue interrumpida.";
        }
    }

    // ── Métodos privados ──────────────────────────────────────────────────

    private String construirBody(String sintomas) {
        // Prompt del sistema: exige formato estructurado con secciones fijas
        String systemContent =
            "Eres un asistente medico clinico de apoyo a la decision. "
            + "Responde UNICAMENTE con el siguiente formato estructurado, sin texto adicional antes ni despues. "
            + "Cada campo debe estar en UNA SOLA LINEA, sin saltos de linea internos:\n"
            + "CAUSAS: causa1 | causa2 | causa3 | causa4 | causa5\n"
            + "GRAVEDAD: Leve\n"
            + "JUSTIFICACION: explicacion breve de la gravedad\n"
            + "RECOMENDACIONES: rec1 | rec2 | rec3\n"
            + "EXAMENES: exam1 | exam2 | exam3\n"
            + "TRATAMIENTO: descripcion del tratamiento inicial\n"
            + "ESTADO_ACTUAL: descripcion del estado actual probable\n"
            + "OBSERVACIONES: observaciones clinicas clave\n"
            + "EVOLUCION: pronostico de evolucion esperada\n"
            + "REVISION: cuando debe realizarse la proxima revision\n"
            + "Para GRAVEDAD usa EXACTAMENTE una de: Leve, Moderado, Grave, Emergencia. "
            + "Maximo 5 causas separadas por |. No hagas diagnosticos definitivos.";

        String promptUsuario = "Sintomas del paciente: " + escaparJson(sintomas)
                + ". Proporciona el analisis clinico completo en el formato indicado.";

        return "{"
                + "\"model\":\"" + MODELO + "\","
                + "\"messages\":["
                +   "{\"role\":\"system\",\"content\":\"" + escaparJson(systemContent) + "\"},"
                +   "{\"role\":\"user\",\"content\":\"" + promptUsuario + "\"}"
                + "],"
                + "\"max_tokens\":" + MAX_TOKENS + ","
                + "\"temperature\":" + TEMPERATURE
                + "}";
    }

    private String construirBodyConImagen(String sintomas, String dataUrl) {
        String systemContent =
            "Eres un asistente medico clinico de apoyo a la decision con capacidad de analisis de imagenes medicas. "
            + "Se te proporciona una imagen medica (radiografia, ecografia, examen de laboratorio, foto clinica u otro) "
            + "y opcionalmente sintomas del paciente. Analiza la imagen con criterio clinico. "
            + "Responde UNICAMENTE con el siguiente formato estructurado, sin texto adicional:\n"
            + "CAUSAS: causa1 | causa2 | causa3\n"
            + "GRAVEDAD: Leve\n"
            + "JUSTIFICACION: hallazgos relevantes de la imagen y su relacion con los sintomas\n"
            + "RECOMENDACIONES: rec1 | rec2 | rec3\n"
            + "EXAMENES: exam1 | exam2\n"
            + "TRATAMIENTO: descripcion del tratamiento inicial\n"
            + "ESTADO_ACTUAL: descripcion del estado actual probable\n"
            + "OBSERVACIONES: hallazgos visuales clave de la imagen\n"
            + "EVOLUCION: pronostico de evolucion esperada\n"
            + "REVISION: cuando debe realizarse la proxima revision\n"
            + "Para GRAVEDAD usa EXACTAMENTE una de: Leve, Moderado, Grave, Emergencia. "
            + "No hagas diagnosticos definitivos. Indica que la interpretacion debe ser confirmada por un medico.";

        String textoUsuario = (sintomas != null && !sintomas.isBlank())
                ? "Sintomas del paciente: " + escaparJson(sintomas) + ". Analiza tambien la imagen adjunta."
                : "Analiza la imagen medica adjunta y proporciona el analisis clinico.";

        // Formato multimodal: content es un array con texto + imagen
        return "{"
                + "\"model\":\"" + MODELO + "\","
                + "\"messages\":["
                +   "{\"role\":\"system\",\"content\":\"" + escaparJson(systemContent) + "\"},"
                +   "{\"role\":\"user\",\"content\":["
                +     "{\"type\":\"text\",\"text\":\"" + escaparJson(textoUsuario) + "\"},"
                +     "{\"type\":\"image_url\",\"image_url\":{\"url\":\"" + dataUrl + "\",\"detail\":\"high\"}}"
                +   "]}"
                + "],"
                + "\"max_tokens\":1200,"
                + "\"temperature\":" + TEMPERATURE
                + "}";
    }

    private String extraerContenido(String jsonResponse) {
        try {
            int idxChoices = jsonResponse.indexOf("\"choices\"");
            if (idxChoices == -1) return "Error: la respuesta no contiene 'choices'.";

            int idxMessage = jsonResponse.indexOf("\"message\"", idxChoices);
            if (idxMessage == -1) return "Error: la respuesta no contiene 'message'.";

            int idxContent = jsonResponse.indexOf("\"content\"", idxMessage);
            if (idxContent == -1) return "Error: la respuesta no contiene 'content'.";

            int idxColon = jsonResponse.indexOf(":", idxContent + 9);
            if (idxColon == -1) return "Error: formato JSON inesperado (sin ':' tras 'content').";

            int idxAperturaComilla = jsonResponse.indexOf("\"", idxColon + 1);
            if (idxAperturaComilla == -1) return "Error: no se encontró el valor de 'content'.";

            int cursor = idxAperturaComilla + 1;
            StringBuilder contenido = new StringBuilder();
            while (cursor < jsonResponse.length()) {
                char c = jsonResponse.charAt(cursor);
                if (c == '\\' && cursor + 1 < jsonResponse.length()) {
                    char sig = jsonResponse.charAt(cursor + 1);
                    switch (sig) {
                        case 'n'  -> { contenido.append('\n'); cursor += 2; }
                        case 't'  -> { contenido.append('\t'); cursor += 2; }
                        case '"'  -> { contenido.append('"');  cursor += 2; }
                        case '\\' -> { contenido.append('\\'); cursor += 2; }
                        case 'r'  -> { contenido.append('\r'); cursor += 2; }
                        default   -> { contenido.append(sig);  cursor += 2; }
                    }
                } else if (c == '"') {
                    break;
                } else {
                    contenido.append(c);
                    cursor++;
                }
            }

            String resultado = contenido.toString().trim();
            return resultado.isEmpty()
                    ? "La IA no generó contenido para los síntomas indicados."
                    : resultado;

        } catch (Exception e) {
            System.err.println("[OpenAIClient] Error parseando respuesta: " + e.getMessage());
            return "Error al procesar la respuesta de la IA: " + e.getMessage();
        }
    }

    private String escaparJson(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
