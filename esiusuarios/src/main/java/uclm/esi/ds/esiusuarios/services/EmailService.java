package uclm.esi.ds.esiusuarios.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service // Servicio para enviar correos vía API HTTP de Brevo
public class EmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${app.mail.from}")
    private String emailFrom;

    // Añadimos una propiedad para el nombre del remitente
    @Value("${brevo.sender.name}")
    private String senderName;

    @Value("${app.base-url}")
    private String appBaseUrl;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Async
    public void enviarCorreoConfirmacion(String email, String token) {
        String url = appBaseUrl + "/users/confirmar-cuenta?token=" + token;
        String subject = "Confirma tu cuenta en ESIentradas";
        String htmlBody = "<h1>¡Bienvenido a ESIentradas!</h1>" +
                "<p>Gracias por registrarte. Por favor, haz clic en el siguiente enlace para activar tu cuenta:</p>" +
                "<h3><a href=\"" + url + "\">Activar mi cuenta</a></h3>" +
                "<p>Este enlace caducará en 24 horas. Si no te has registrado, por favor ignora este email.</p>";

        sendEmailViaHttp(email, subject, htmlBody);
    }

    @Async
    public void sendWelcomeEmail(String emailDestino, String nombre) {
        String subject = "¡Bienvenido a ESIentradas!";
        String htmlBody = "<p>Hola " + nombre + ",</p>" +
            "<p>Tu cuenta en ESIentradas ha sido creada correctamente.<br>" +
            "Ya puedes buscar espectáculos y comprar entradas.</p>" +
            "<p>¡Que disfrutes del espectáculo!<br>" +
            "<strong>El equipo de ESIentradas</strong></p>";

        sendEmailViaHttp(emailDestino, subject, htmlBody);
    }

    @Async
    public void sendPasswordRecoveryEmail(String emailDestino, String codigo) {
        String subject = "Recuperación de contraseña - ESIentradas";
        String htmlBody = "<h3>Hola,</h3>" +
            "<p>Has solicitado recuperar tu contraseña en ESIentradas.</p>" +
            "<p>Tu código de recuperación es:</p>" +
            "<h2 style='color: #2c3e50;'>" + codigo + "</h2>" +
            "<p>Este código caduca en 15 minutos.<br>" +
            "Si no has solicitado esto, ignora este email.</p>" +
            "<p><strong>El equipo de ESIentradas</strong></p>";

        sendEmailViaHttp(emailDestino, subject, htmlBody);
    }

    @Async
    public void sendAccountCancellationEmail(String emailDestino) {
        String subject = "Confirmación de baja - ESIentradas";
        String htmlBody = "<h3>Hola,</h3>" +
            "<p>Te confirmamos que tu cuenta en ESIentradas ha sido cancelada correctamente.<br>" +
            "Todos tus datos de sesión han sido invalidados por seguridad.</p>" +
            "<p>Lamentamos que nos dejes. Si en el futuro cambias de opinión, siempre serás bienvenido a registrarte de nuevo en nuestra taquilla virtual.</p>" +
            "<p>Un cordial saludo,<br>" +
            "<strong>El equipo de ESIentradas</strong></p>";

        sendEmailViaHttp(emailDestino, subject, htmlBody);
    }

    private void sendEmailViaHttp(String to, String subject, String htmlBody) {
        String escapedSubject = subject.replace("\"", "\\\"");
        String escapedHtmlBody = htmlBody.replace("\"", "\\\"");

        String jsonPayload = "{"
                + "\"sender\": {\"name\": \"" + senderName + "\", \"email\": \"" + emailFrom + "\"},"
                + "\"to\": [{\"email\": \"" + to + "\"}],"
                + "\"subject\": \"" + escapedSubject + "\","
                + "\"htmlContent\": \"" + escapedHtmlBody + "\""
                + "}";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_API_URL))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) { // 201 Created es éxito para la API de Brevo
                throw new RuntimeException("Error de Brevo enviando email a " + to + ". Status: " + response.statusCode() + ", Body: " + response.body());
            }
            System.out.println("[EmailService] Email enviado a " + to + " via API. Respuesta Brevo: " + response.statusCode());
        } catch (Exception e) {
            throw new RuntimeException("Error conectando con la API de Brevo: " + e.getMessage(), e);
        }
    }
}