package uclm.esi.ds.esiusuarios.http;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import uclm.esi.ds.esiusuarios.services.UserService;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class UserController {

    @Autowired
    private UserService service;

    // Registrar nuevo usuario
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, String> data) {
        try {
            String resultado = service.register(
                    data.get("email"),
                    data.get("pwd"),
                    data.get("nombre"));
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Iniciar sesión y obtener token
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> credentials) {
        try {
            String token = service.login(
                    credentials.get("email"),
                    credentials.get("pwd"));
            return ResponseEntity.ok(token);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // Cerrar sesión
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody Map<String, String> data) {
        service.logout(data.get("token"));
        return ResponseEntity.ok("Sesión cerrada correctamente.");
    }

    // Solicitar código de recuperación
    @PostMapping("/recuperarPassword")
    public ResponseEntity<String> recuperarPassword(@RequestBody Map<String, String> data) {
        service.solicitarRecuperacion(data.get("email"));
        return ResponseEntity.ok("Si el email existe, recibirás un código en tu correo.");
    }

    // Restablecer contraseña con código
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

    // Cancelar cuenta
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

    // En la clase UserController

    @GetMapping("/confirmar-cuenta")
    public ResponseEntity<String> confirmarCuenta(@RequestParam("token") String token) {
        try {
            service.confirmarCuenta(token);
            String successHtml = "<html><body style='font-family: sans-serif; text-align: center;'><h1>¡Cuenta activada!</h1><p>Tu cuenta ha sido activada correctamente. Ya puedes cerrar esta ventana e iniciar sesión.</p></body></html>";
            return ResponseEntity.ok(successHtml);
        } catch (ResponseStatusException e) {
            String errorHtml = "<html><body style='font-family: sans-serif; text-align: center;'><h1>Error</h1><p>" + e.getReason() + "</p></body></html>";
            return new ResponseEntity<>(errorHtml, e.getStatusCode());
        }
    }

}
