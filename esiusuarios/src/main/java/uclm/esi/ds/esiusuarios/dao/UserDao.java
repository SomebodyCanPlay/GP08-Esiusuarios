package uclm.esi.ds.esiusuarios.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import uclm.esi.ds.esiusuarios.model.User;
import java.util.Optional;

// ¿Qué es una "interfaz"?
// Es como un contrato: defines qué métodos quieres, y Spring los implementa por ti.
// No necesitas escribir el cuerpo de cada método — Spring genera el SQL automáticamente.

// JpaRepository<User, Long> significa:
//   - User  → la clase que gestiona esta DAO (la tabla "usuario")
//   - Long  → el tipo del ID de esa clase (@Id private Long id)
public interface UserDao extends JpaRepository<User, Long> {

    // Spring ve "findByEmail" y genera automáticamente:
    // SELECT * FROM usuario WHERE email = ?
    // Optional<> significa que puede devolver un usuario o puede no encontrarlo (vacío)
    Optional<User> findByEmail(String email);

    // Spring genera: SELECT * FROM usuario WHERE token_sesion = ?
    // Lo usaremos para validar el token cuando esientradas nos pregunte
    Optional<User> findByTokenSesion(String tokenSesion);

    // Esta query personalizada actualiza el token de sesión de un usuario
    // @Modifying → le dice a Spring que esta query modifica datos (UPDATE/DELETE)
    // @Query      → aquí escribimos el JPQL (es como SQL pero con nombres de clase Java)
    @Modifying
    @Query("UPDATE User u SET u.tokenSesion = :token WHERE u.email = :email")
    void actualizarTokenSesion(@Param("email") String email, @Param("token") String token);

    // Desactiva la cuenta de un usuario (cancelar cuenta)
    // En vez de borrar al usuario de la BD, ponemos activo = false
    @Modifying
    @Query("UPDATE User u SET u.activo = false WHERE u.email = :email")
    void desactivarCuenta(@Param("email") String email);
}

