package uclm.esi.ds.esiusuarios.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import uclm.esi.ds.esiusuarios.model.TokenRecuperacion;
import java.util.Optional;

// Igual que UserDao, esta interfaz habla automáticamente con la tabla token_recuperacion
// JpaRepository<TokenRecuperacion, Long>:
//   - TokenRecuperacion → la clase/tabla que gestiona
//   - Long             → el tipo de su @Id
public interface TokenRecuperacionDao extends JpaRepository<TokenRecuperacion, Long> {

    // Buscar un token por su valor (el código que se envía por email)
    // Spring genera: SELECT * FROM token_recuperacion WHERE token = ?
    Optional<TokenRecuperacion> findByToken(String token);

    // Borrar todos los tokens de un email (para limpiar tokens viejos)
    // Spring genera: DELETE FROM token_recuperacion WHERE email = ?
    void deleteByEmail(String email);
}
