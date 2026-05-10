package uclm.esi.ds.esiusuarios.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import uclm.esi.ds.esiusuarios.services.UserService;

// Controlador para peticiones externas
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
}
            // Preguntamos al servicio: ¿existe alguien con este token?
            String email = service.checkToken(token);

            if (email != null) {
                // Token válido → devolvemos el email para que esientradas lo use
                return ResponseEntity.ok(email);
            } else {
                // Token no válido o caducado
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Token no válido o sesión caducada");
            }
        } catch (Exception e) {
            e.printStackTrace(); // Esto saldrá en la consola de Eclipse
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error en esiusuarios: " + e.getMessage() + " (Clase: " + e.getClass().getSimpleName() + ")");
        }
    }
}
