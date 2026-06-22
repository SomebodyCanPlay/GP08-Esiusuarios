package uclm.esi.ds.esiusuarios.http;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uclm.esi.ds.esiusuarios.services.UserService;

// Controlador para peticiones externas (Microservicios)
@RestController
@CrossOrigin(origins = "*")
public class ExternalController {

    @Autowired
    private UserService service;

    @GetMapping("/testPing")
    public ResponseEntity<String> testPing() {
        return ResponseEntity.ok("PONG");
    }

    // Comprueba si un token es válido y devuelve el email
    @GetMapping("/checkToken")
    public ResponseEntity<String> checkToken(@RequestParam("token") String token) {
        try {
            String email = service.checkToken(token);

            if (email != null) {
                return ResponseEntity.ok(email);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Token no válido o sesión caducada");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error en esiusuarios: " + e.getMessage() + " (Clase: " + e.getClass().getSimpleName() + ")");
        }
    }

    // --> ENDPOINTS DEL MONEDERO (Uso interno entre microservicios) <--

    // Sumar saldo (Llamado desde esientradas al cancelar)
    @PostMapping("/sumarSaldo")
    public ResponseEntity<String> sumarSaldo(@RequestBody Map<String, Object> data) {
        try {
            String email = (String) data.get("email");
            Double cantidad = Double.valueOf(data.get("cantidad").toString());
            
            service.sumarSaldo(email, cantidad);
            return ResponseEntity.ok("Saldo añadido correctamente.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al sumar saldo: " + e.getMessage());
        }
    }

    // Restar saldo (Llamado desde esientradas al comprar con monedero)
    @PostMapping("/restarSaldo")
    public ResponseEntity<String> restarSaldo(@RequestBody Map<String, Object> data) {
        try {
            String email = (String) data.get("email");
            Double cantidad = Double.valueOf(data.get("cantidad").toString());
            
            service.restarSaldo(email, cantidad);
            return ResponseEntity.ok("Saldo restado correctamente.");
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al restar saldo: " + e.getMessage());
        }
    }
}