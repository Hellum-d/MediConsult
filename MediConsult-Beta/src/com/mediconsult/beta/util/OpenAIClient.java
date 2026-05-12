package com.mediconsult.beta.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente HTTP para consultar la API de OpenAI.
 *
 * Recibe síntomas y devuelve posibles causas generadas por el modelo.
 * Solo debe ser usado desde DiagnosticoService.
 * No usa librerías externas para JSON.
 */
public class OpenAIClient {

    /** Endpoint de Chat Completions. */
    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";

    /** API Key (provisional). */
    private static final String API_KEY = "aqui va la api key, no se peude subir a github";

    /** Modelo a usar. */
    private static final String MODELO = "gpt-4o-mini";

    /** Límite de tokens de la respuesta. */
    private static final int MAX_TOKENS = 300;

    /** Temperatura baja para respuestas más clínicas. */
    private static final double TEMPERATURE = 0.2;

    /** Timeout máximo de espera. */
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    /** Cliente HTTP reutilizable. */
    private final HttpClient httpClient;

    /** Inicializa el HttpClient con timeout y redirecciones. */
    public OpenAIClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Envía los síntomas al modelo y devuelve posibles causas.
     * Si falla la llamada, retorna un mensaje de error en lugar de lanzar excepción.
     *
     * @param sintomas texto con los síntomas
     * @return respuesta generada por la IA o mensaje de error
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
                    peticion,
                    HttpResponse.BodyHandlers.ofString()
            );

            int statusCode = respuesta.statusCode();

            if (statusCode == 200) {
                return extraerContenido(respuesta.body());
            } else {
                System.err.println("[OpenAIClient] HTTP " + statusCode
                        + " — " + respuesta.body());
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
     * Construye el JSON del request para Chat Completions.
     */
    private String construirBody(String sintomas) {
        String sintomasEscapados = escaparJson(sintomas);
        String promptUsuario = "Posibles causas médicas de: " + sintomasEscapados
                + ". Responde en máximo 5 puntos breves.";

        return "{"
                + "\"model\":\"" + MODELO + "\","
                + "\"messages\":["
                +   "{"
                +     "\"role\":\"system\","
                +     "\"content\":\"Eres un asistente medico de apoyo. "
                +       "Responde de forma concisa con una lista numerada "
                +       "de posibles causas. No hagas diagnosticos definitivos.\""
                +   "},"
                +   "{"
                +     "\"role\":\"user\","
                +     "\"content\":\"" + promptUsuario + "\""
                +   "}"
                + "],"
                + "\"max_tokens\":" + MAX_TOKENS + ","
                + "\"temperature\":" + TEMPERATURE
                + "}";
    }

    /**
     * Extrae el texto de choices[0].message.content del JSON de respuesta.
     * El parsing se hace manualmente con String.
     */
    private String extraerContenido(String jsonResponse) {
        try {
            int idxChoices = jsonResponse.indexOf("\"choices\"");
            if (idxChoices == -1) {
                return "Error: la respuesta no contiene 'choices'.";
            }

            int idxMessage = jsonResponse.indexOf("\"message\"", idxChoices);
            if (idxMessage == -1) {
                return "Error: la respuesta no contiene 'message'.";
            }

            int idxContent = jsonResponse.indexOf("\"content\"", idxMessage);
            if (idxContent == -1) {
                return "Error: la respuesta no contiene 'content'.";
            }

            int idxColon = jsonResponse.indexOf(":", idxContent + 9);
            if (idxColon == -1) {
                return "Error: formato JSON inesperado.";
            }

            int idxAperturaComilla = jsonResponse.indexOf("\"", idxColon + 1);
            if (idxAperturaComilla == -1) {
                return "Error: no se encontró el valor de 'content'.";
            }

            int cursor = idxAperturaComilla + 1;
            StringBuilder contenido = new StringBuilder();

            while (cursor < jsonResponse.length()) {
                char c = jsonResponse.charAt(cursor);
                if (c == '\\' && cursor + 1 < jsonResponse.length()) {
                    char siguiente = jsonResponse.charAt(cursor + 1);
                    switch (siguiente) {
                        case 'n'  -> { contenido.append('\n'); cursor += 2; }
                        case 't'  -> { contenido.append('\t'); cursor += 2; }
                        case '"'  -> { contenido.append('"');  cursor += 2; }
                        case '\\' -> { contenido.append('\\'); cursor += 2; }
                        case 'r'  -> { contenido.append('\r'); cursor += 2; }
                        default   -> { contenido.append(siguiente); cursor += 2; }
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
                    ? "La IA no generó contenido."
                    : resultado;

        } catch (Exception e) {
            System.err.println("[OpenAIClient] Error parseando respuesta: " + e.getMessage());
            return "Error al procesar la respuesta: " + e.getMessage();
        }
    }

    /**
     * Escapa caracteres especiales para que el texto sea válido en JSON.
     */s
    private String escaparJson(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}