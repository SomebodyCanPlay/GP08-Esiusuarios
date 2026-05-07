package uclm.esi.ds.esiusuarios.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import uclm.esi.ds.esiusuarios.services.UserService;

// Este controlador es el que usa esientradas para comunicarse con esiusuarios
// Cuando un usuario quiere comprar, esientradas nos pregunta:
// "¿Existe un usuario con este token? ¿Cuál es su email?"
@RestController
@CrossOrigin(origins = "*")  // Permitimos desde cualquier origen (esientradas puede estar en otro puerto)
public class ExternalController {

    // Inyectamos el servicio igual que en UserController
    @Autowired
    private UserService service;

    // ============================================================
    // GET /checkToken?token=xxxxxxxx
    //
    // esientradas llama a este endpoint para saber si el usuario está logueado
    // Si el token es válido → devuelve el email del usuario (200 OK)
    // Si el token no es válido → devuelve 401 Unauthorized
    //
    // @RequestParam → lee el parámetro de la URL: ?token=xxxxx
    //   (distinto de @PathVariable que sería /checkToken/xxxxx)
    // ============================================================
    @GetMapping("/checkToken")
    public ResponseEntity<String> checkToken(@RequestParam String token) {

        // Preguntamos al servicio: ¿existe alguien con este token?
        String email = service.checkToken(token);

        if (email != null) {
            // Token válido → devolvemos el email para que esientradas lo use
            // (lo necesita para enviar el email de confirmación de compra)
            return ResponseEntity.ok(email);
        } else {
            // Token no válido o caducado
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token no válido o sesión caducada");
        }
    }
}