package uclm.esi.ds.esiusuarios.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import uclm.esi.ds.esiusuarios.services.UserService;

import java.util.Map;

// @RestController → esta clase es un controlador REST (escucha pericones de internet http)
// Recibe peticiones HTTP del frontend y devuelve respuestas JSON
// @RequestMapping("/users") → todas las rutas de esta clase empiezan por /users
// @CrossOrigin → permite que el frontend en puerto 4200 llame a este servidor (puerto 8081)
//                sin que el navegador lo bloquee por seguridad (CORS)
@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class UserController {

    // El controlador usa el servicio para hacer el trabajo real
    @Autowired
    private UserService service;

    // ============================================================
    // POST /users/register
    // El frontend manda: { "email": "...", "pwd": "...", "nombre": "..." }
    // ============================================================
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, String> data) {
        try {
            String resultado = service.register(
                    data.get("email"),
                    data.get("pwd"),
                    data.get("nombre"));
            // 201 Created = registro exitoso
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);

        } catch (IllegalArgumentException e) {
            // 400 Bad Request = algo estaba mal (email duplicado, campos vacíos...)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // ============================================================
    // POST /users/login
    // El frontend manda: { "email": "...", "pwd": "..." }
    // Devuelve: el token UUID en texto plano (ej: "a3f9k2b1-d4e5-...")
    // El frontend lo guarda y lo usa al comprar entradas
    // ============================================================
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> credentials) {
        try {
            // Llamamos al servicio, que verifica email+contraseña y devuelve el token
            String token = service.login(
                    credentials.get("email"),
                    credentials.get("pwd"));
            // 200 OK + el token en el cuerpo de la respuesta
            return ResponseEntity.ok(token);

        } catch (IllegalArgumentException e) {
            // 401 Unauthorized = credenciales incorrectas
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // ============================================================
    // POST /users/logout
    // Body: { "token": "..." }
    // Borra el token de sesión de la BD para invalidar la sesión
    // ============================================================
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody Map<String, String> data) {
        service.logout(data.get("token"));
        return ResponseEntity.ok("Sesión cerrada correctamente.");
    }

    // ============================================================
    // POST /users/recuperarPassword
    // El frontend manda: { "email": "..." }
    // El servidor genera un código y lo envía por email con Brevo
    // ============================================================
    @PostMapping("/recuperarPassword")
    public ResponseEntity<String> recuperarPassword(@RequestBody Map<String, String> data) {
        // No revelamos si el email existe o no (seguridad)
        // Siempre respondemos lo mismo
        service.solicitarRecuperacion(data.get("email"));
        return ResponseEntity.ok("Si el email existe, recibirás un código en tu correo.");
    }

    // ============================================================
    // POST /users/restablecerPassword
    // El frontend manda: { "codigo": "...", "nuevaPassword": "..." }
    // El usuario introduce el código que recibió por email
    // ============================================================
    @PostMapping("/restablecerPassword")
    public ResponseEntity<String> restablecerPassword(@RequestBody Map<String, String> data) {
        try {
            service.restablecerPassword(
                    data.get("codigo"),
                    data.get("nuevaPassword"));
            return ResponseEntity.ok("Contraseña actualizada correctamente.");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // ============================================================
    // DELETE /users/cancelarCuenta
    // El frontend manda: { "email": "...", "pwd": "..." }
    // Desactiva la cuenta (no la borra de la BD)
    // ============================================================
    @DeleteMapping("/cancelarCuenta")
    public ResponseEntity<String> cancelarCuenta(@RequestBody Map<String, String> data) {
        try {
            service.cancelarCuenta(
                    data.get("email"),
                    data.get("pwd"));
            return ResponseEntity.ok("Cuenta cancelada correctamente.");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
